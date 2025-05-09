package harvard.capstone.digitaltherapy.burnout.service;

import harvard.capstone.digitaltherapy.burnout.fhir.BurnoutAssessmentFhirConverter;
import harvard.capstone.digitaltherapy.burnout.model.BurnoutAssessmentResult;
import harvard.capstone.digitaltherapy.burnout.model.BurnoutQuestion;
import harvard.capstone.digitaltherapy.burnout.model.BurnoutUserResponse;
import harvard.capstone.digitaltherapy.utility.S3Utils;

import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Service for converting Burnout Assessment data to FHIR format,
 * validating it against FHIR specifications, and storing it in S3.
 */
@Service
public class BurnoutFhirService {
    private static final Logger logger = LoggerFactory.getLogger(BurnoutFhirService.class);

    private final FhirContext fhirContext;
    private final FhirValidator validator;
    private final S3Utils s3Utils;
    private final BurnoutAssessmentFhirConverter converter;

    @Autowired
    public BurnoutFhirService(S3Utils s3Utils, BurnoutAssessmentFhirConverter converter) {
        this.s3Utils = s3Utils;
        this.converter = converter;

        // Initialize FHIR context and validator
        this.fhirContext = FhirContext.forR4();
        this.validator = fhirContext.newValidator();

        logger.info("BurnoutFhirService initialized");
    }

    /**
     * Converts an assessment result to FHIR, validates it, and stores it in S3.
     *
     * @param result The burnout assessment result to convert
     * @return The S3 URL where the FHIR document is stored
     */
    public String processAndStoreAssessment(BurnoutAssessmentResult result) {
        try {
            logger.info("Converting assessment result to FHIR: {}", result.getSessionId());

            // Convert to FHIR
            QuestionnaireResponse fhirResponse = converter.convertToFhir(result);

            // Generate a unique ID if not already set
            if (fhirResponse.getId() == null || fhirResponse.getId().isEmpty()) {
                fhirResponse.setId(UUID.randomUUID().toString());
            }

            // Enhance with additional metadata
            enhanceFhirResource(fhirResponse, result);

            // Validate the FHIR resource
            ValidationResult validationResult = validator.validateWithResult(fhirResponse);
            if (!validationResult.isSuccessful()) {
                logger.warn("FHIR validation issues found: {}", validationResult.getMessages().size());
                validationResult.getMessages().forEach(message ->
                        logger.debug("Validation message: {}", message.getMessage()));
            }

            // Serialize to JSON
            String fhirJson = serializeToJson(fhirResponse);

            // Store in S3
            String s3Key = generateS3Key(result.getSessionId());
            String s3Url = uploadToS3(fhirJson, s3Key);

            logger.info("FHIR document stored at: {}", s3Url);
            return s3Url;

        } catch (Exception e) {
            logger.error("Error processing FHIR conversion: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process FHIR conversion", e);
        }
    }

    /**
     * Add additional metadata to the FHIR resource
     */
    private void enhanceFhirResource(QuestionnaireResponse fhirResponse, BurnoutAssessmentResult result) {
        // Add profile reference
        fhirResponse.getMeta().addProfile("http://example.org/fhir/StructureDefinition/burnout-assessment");

        // Add last updated timestamp
        fhirResponse.getMeta().setLastUpdated(new Date());

        // Add creation mode tag
        fhirResponse.getMeta().addTag()
                .setSystem("http://harvard.capstone.digitaltherapy/fhir/tags")
                .setCode("auto-generated")
                .setDisplay("Automatically generated from assessment");

        // Add version information
        fhirResponse.getMeta().setVersionId("1");

        // Add source information as an extension
        Extension sourceExt = new Extension()
                .setUrl("http://harvard.capstone.digitaltherapy/fhir/extensions/source-system");
        sourceExt.setValue(new StringType("DigitalTherapyAssistant"));
        fhirResponse.addExtension(sourceExt);
    }

    /**
     * Serialize a FHIR resource to JSON
     */
    private String serializeToJson(Resource resource) {
        return fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(resource);
    }

    /**
     * Serialize a FHIR resource to XML
     */
    private String serializeToXml(Resource resource) {
        return fhirContext.newXmlParser().setPrettyPrint(true).encodeResourceToString(resource);
    }

    /**
     * Generate a consistent S3 key for the FHIR document
     */
    private String generateS3Key(String sessionId) {
        return "fhir/burnout-assessment-" + sessionId + ".json";
    }

    /**
     * Upload the FHIR document to S3
     */
    private String uploadToS3(String fhirJson, String s3Key) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(
                    fhirJson.getBytes(StandardCharsets.UTF_8));

            return s3Utils.uploadFile(inputStream, s3Key, "application/fhir+json");
        } catch (Exception e) {
            logger.error("Failed to upload FHIR document to S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store FHIR document", e);
        }
    }

    /**
     * Retrieve a previously stored FHIR document from S3
     */
    public String retrieveFhirDocument(String sessionId) {
        try {
            String s3Key = generateS3Key(sessionId);
            return s3Utils.downloadFileAsString(s3Key);
        } catch (Exception e) {
            logger.error("Failed to retrieve FHIR document: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve FHIR document", e);
        }
    }
}
