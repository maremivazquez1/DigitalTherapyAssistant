package harvard.capstone.digitaltherapy.llm.service;

/**
 * LLMProcessingService orchestrates the full workflow of text generation using AWS Bedrock.
 *
 * It reads a prompt from a given S3 path, sends it to the BedrockService for text generation,
 * writes the generated output back to S3, and returns the new S3 path.
 *
 * This service abstracts away orchestration logic from lower-level services and is intended
 * to be used by controllers or WebSocket handlers that manage end-to-end CBT session flows.
 */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class LLMProcessingService {

    private final BedrockService bedrockService;
    private final S3StorageService s3Service;

    @Autowired
    public LLMProcessingService(BedrockService bedrockService, S3StorageService s3Service) {
        this.bedrockService = bedrockService;
        this.s3Service = s3Service;
    }

    public String process(String inputS3Path) throws IOException {
        String prompt = s3Service.readTextFromS3(inputS3Path);
        String response = bedrockService.generateTextWithNovaLite(prompt);
        String outputPath = generateOutputPath(inputS3Path);
        s3Service.writeTextToS3(outputPath, response);
        String rootOutputPath ="s3://dta-root/"+ outputPath;
        return rootOutputPath;
    }

    private String generateOutputPath(String inputPath) {
        String outputPath= inputPath.replace("s3://dta-root/", "");
        int dotIndex = outputPath.lastIndexOf('.');
        if (dotIndex > 0) {
            outputPath = outputPath.substring(0, dotIndex) + "-response.txt";
            return outputPath;
        } else {
            return outputPath + "-response.txt";
        }
    }
}
