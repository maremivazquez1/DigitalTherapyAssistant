package harvard.capstone.digitaltherapy.aws;

import harvard.capstone.digitaltherapy.aws.service.RekognitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RekognitionServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(RekognitionServiceTest.class);

    @Mock
    private RekognitionClient mockRekognitionClient;

    @InjectMocks
    private RekognitionService rekognitionService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testDetectFacesFromVideoAsync_validS3Url() throws Exception {
        String s3Url = "s3://mock-bucket/mock-key.mp4";
        String mockJobId = "test-job-id";

        // Mock StartFaceDetection response
        StartFaceDetectionResponse startResponse = StartFaceDetectionResponse.builder()
            .jobId(mockJobId)
            .build();

        when(mockRekognitionClient.startFaceDetection(any(StartFaceDetectionRequest.class)))
            .thenReturn(startResponse);

        // Mock GetFaceDetection response with one detected face
        FaceDetail faceDetail = FaceDetail.builder()
            .confidence(98.7f)
            .build();

        FaceDetection faceDetection = FaceDetection.builder()
            .timestamp(1000L)
            .face(faceDetail)
            .build();

        GetFaceDetectionResponse getResponse = GetFaceDetectionResponse.builder()
            .jobStatus("SUCCEEDED")
            .faces(Collections.singletonList(faceDetection))
            .build();

        // Return mock response immediately on first poll
        when(mockRekognitionClient.getFaceDetection(any(GetFaceDetectionRequest.class)))
            .thenReturn(getResponse);

        // Inject mock Rekognition client into the service
        injectMockClient(rekognitionService, mockRekognitionClient);

        // Call the async method and wait for the result
        CompletableFuture<String> future = rekognitionService.detectFacesFromVideoAsync(s3Url);
        String jsonResponse = future.get(10, TimeUnit.SECONDS); // Wait up to 10 seconds

        // Assert response is not null or empty
        assertNotNull(jsonResponse);
        assertTrue(jsonResponse.toLowerCase().contains("timestamp")); // Basic check for expected JSON content

        logger.info("JSON response: {}", jsonResponse);
    }


    // Utility method for injecting the mock client into the private field
    private void injectMockClient(RekognitionService service, RekognitionClient mockClient) {
        try {
            java.lang.reflect.Field field = RekognitionService.class.getDeclaredField("rekognitionClient");
            field.setAccessible(true);
            field.set(service, mockClient);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock RekognitionClient", e);
        }
    }
}
