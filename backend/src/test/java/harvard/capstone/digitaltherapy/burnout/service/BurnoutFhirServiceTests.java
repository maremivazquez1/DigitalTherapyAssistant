package harvard.capstone.digitaltherapy.burnout.service;

import harvard.capstone.digitaltherapy.burnout.fhir.BurnoutAssessmentFhirConverter;
import harvard.capstone.digitaltherapy.burnout.service.BurnoutFhirService;
import harvard.capstone.digitaltherapy.burnout.model.*;
import harvard.capstone.digitaltherapy.utility.S3Utils;

import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Extension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BurnoutFhirServiceTests {

    private BurnoutFhirService fhirService;

    @Mock
    private S3Utils s3Utils;

    @Mock
    private BurnoutAssessmentFhirConverter converter;

    @Mock
    private FhirValidator validator;

    @Mock
    private ValidationResult validationResult;

    private BurnoutAssessmentResult sampleResult;
    private QuestionnaireResponse sampleFhirResponse;
    private final String SESSION_ID = "test-session-123";
    private final String USER_ID = "user-456";
    private final String S3_URL = "https://s3-bucket.example.com/fhir/burnout-assessment-test-session-123.json";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create service with mocks
        fhirService = new BurnoutFhirService(s3Utils, converter);

        // Inject mocked validator
        FhirContext fhirContext = mock(FhirContext.class);
        ReflectionTestUtils.setField(fhirService, "fhirContext", fhirContext);
        ReflectionTestUtils.setField(fhirService, "validator", validator);

        // Setup sample burnout assessment result
        sampleResult = createSampleAssessmentResult();

        // Setup sample FHIR response
        sampleFhirResponse = new QuestionnaireResponse();
        sampleFhirResponse.setId(SESSION_ID);

        // Configure mocks
        when(converter.convertToFhir(any(BurnoutAssessmentResult.class))).thenReturn(sampleFhirResponse);
        when(validator.validateWithResult(any(Resource.class))).thenReturn(validationResult);
        when(validationResult.isSuccessful()).thenReturn(true);

        // Mock serialization
        when(fhirContext.newJsonParser()).thenReturn(mock(ca.uhn.fhir.parser.IParser.class));
        when(fhirContext.newJsonParser().setPrettyPrint(anyBoolean())).thenReturn(mock(ca.uhn.fhir.parser.IParser.class));
        when(fhirContext.newJsonParser().setPrettyPrint(anyBoolean()).encodeResourceToString(any(Resource.class)))
                .thenReturn("{\"resourceType\":\"QuestionnaireResponse\"}");
    }

    /**
     * Test the complete workflow with a valid assessment result
     */
    @Test
    void testProcessAndStoreAssessment_Success() {
        // Setup
        when(s3Utils.uploadFile(any(InputStream.class), anyString(), anyString())).thenReturn(S3_URL);

        // Execute
        String result = fhirService.processAndStoreAssessment(sampleResult);

        // Verify
        assertEquals(S3_URL, result);

        // Verify the conversion was called
        verify(converter).convertToFhir(sampleResult);

        // Verify S3 upload was called with correct parameters
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(s3Utils).uploadFile(any(InputStream.class), keyCaptor.capture(), eq("application/fhir+json"));
        String capturedKey = keyCaptor.getValue();
        assertTrue(capturedKey.startsWith("fhir/burnout-assessment-"));
        assertTrue(capturedKey.endsWith(".json"));
        assertTrue(capturedKey.contains(SESSION_ID));
    }

    /**
     * Test retrieving a FHIR document from S3
     */
    @Test
    void testRetrieveFhirDocument_Success() {
        // Setup
        String expectedJson = "{\"resourceType\":\"QuestionnaireResponse\"}";
        when(s3Utils.downloadFileAsString(anyString())).thenReturn(expectedJson);

        // Execute
        String result = fhirService.retrieveFhirDocument(SESSION_ID);

        // Verify
        assertEquals(expectedJson, result);

        // Verify correct S3 key was used
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(s3Utils).downloadFileAsString(keyCaptor.capture());
        String capturedKey = keyCaptor.getValue();
        assertEquals("fhir/burnout-assessment-" + SESSION_ID + ".json", capturedKey);
    }

    /**
     * Test error handling when S3 upload fails
     */
    @Test
    void testProcessAndStoreAssessment_S3UploadFailure() {
        // Setup
        when(s3Utils.uploadFile(any(InputStream.class), anyString(), anyString()))
                .thenThrow(new RuntimeException("S3 upload failed"));

        // Execute and verify
        Exception exception = assertThrows(RuntimeException.class, () -> {
            fhirService.processAndStoreAssessment(sampleResult);
        });

        // Check for any of the possible error messages in the exception chain
        assertTrue(
                exception.getMessage().contains("Failed to process FHIR conversion") ||
                        exception.getMessage().contains("Failed to store FHIR document") ||
                        (exception.getCause() != null && exception.getCause().getMessage().contains("S3 upload failed"))
        );
    }

    /**
     * Test error handling when S3 download fails
     */
    @Test
    void testRetrieveFhirDocument_NotFound() {
        // Setup
        when(s3Utils.downloadFileAsString(anyString()))
                .thenThrow(new RuntimeException("File not found"));

        // Execute and verify
        Exception exception = assertThrows(RuntimeException.class, () -> {
            fhirService.retrieveFhirDocument(SESSION_ID);
        });

        assertTrue(exception.getMessage().contains("Failed to retrieve FHIR document"));
    }

    /**
     * Test that FHIR resource enhancement adds proper metadata
     */
    @Test
    void testEnhanceFhirResourceEnhancement() {
        // Setup
        when(s3Utils.uploadFile(any(InputStream.class), anyString(), anyString())).thenReturn(S3_URL);

        // Execute
        fhirService.processAndStoreAssessment(sampleResult);

        // Capture the FHIR resource that was validated
        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(validator).validateWithResult(resourceCaptor.capture());

        QuestionnaireResponse capturedResponse = (QuestionnaireResponse) resourceCaptor.getValue();

        // Verify metadata was added
        assertNotNull(capturedResponse.getMeta());

        // Test profile
        assertFalse(capturedResponse.getMeta().getProfile().isEmpty());
        assertEquals("http://example.org/fhir/StructureDefinition/burnout-assessment",
                capturedResponse.getMeta().getProfile().get(0).getValue());

        // Test tags
        assertFalse(capturedResponse.getMeta().getTag().isEmpty());
        assertEquals("auto-generated", capturedResponse.getMeta().getTag().get(0).getCode());

        // Test timestamps and version
        assertNotNull(capturedResponse.getMeta().getLastUpdated());
        assertEquals("1", capturedResponse.getMeta().getVersionId());

        // Test extensions
        assertFalse(capturedResponse.getExtension().isEmpty());
        Extension sourceExt = capturedResponse.getExtension().stream()
                .filter(ext -> "http://harvard.capstone.digitaltherapy/fhir/extensions/source-system".equals(ext.getUrl()))
                .findFirst()
                .orElse(null);
        assertNotNull(sourceExt);
        assertTrue(sourceExt.getValue() instanceof StringType);
        assertEquals("DigitalTherapyAssistant", ((StringType)sourceExt.getValue()).getValue());
    }

    /**
     * Helper method to create a sample burnout assessment result for testing
     */
    private BurnoutAssessmentResult createSampleAssessmentResult() {

        // Create questions
        List<BurnoutQuestion> questions = new ArrayList<>();
        BurnoutQuestion q1 = new BurnoutQuestion("q1",
                "How often do you feel emotionally drained from your work?",
                AssessmentDomain.WORK, false);

        BurnoutQuestion q2 = new BurnoutQuestion("q2",
                "How often do you feel you've become more callous toward people?",
                AssessmentDomain.PERSONAL, false);

        questions.add(q1);
        questions.add(q2);

        // Create assessment
        BurnoutAssessment assessment = new BurnoutAssessment(questions);

        // Create user responses
        Map<String, BurnoutUserResponse> responses = new HashMap<>();

        Map<String, Object> insights1 = new HashMap<>();
        insights1.put("voiceAnalysis", Map.of("stress", 0.75, "energy", 0.3));

        Map<String, Object> insights2 = new HashMap<>();
        insights2.put("voiceAnalysis", Map.of("stress", 0.6, "energy", 0.4));

        BurnoutUserResponse r1 = new BurnoutUserResponse("q1", "Often", insights1);
        BurnoutUserResponse r2 = new BurnoutUserResponse("q2", "Sometimes", insights2);

        responses.put("q1", r1);
        responses.put("q2", r2);

        // Create score
        BurnoutScore score = new BurnoutScore(SESSION_ID, USER_ID, 3.5,
                "Moderate burnout detected with high emotional exhaustion");

        // Create assessment result
        return new BurnoutAssessmentResult(
                SESSION_ID,
                USER_ID,
                assessment,
                responses,
                score,
                "Moderate burnout detected",
                LocalDateTime.now()
        );
    }
}