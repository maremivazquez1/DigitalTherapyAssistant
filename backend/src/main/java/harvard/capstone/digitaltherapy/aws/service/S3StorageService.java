package harvard.capstone.digitaltherapy.aws.service;

import harvard.capstone.digitaltherapy.utility.S3Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Service
public class S3StorageService {

    private final S3Utils s3Utils;

    @Autowired
    public S3StorageService(S3Utils s3Utils) {
        this.s3Utils = s3Utils;
    }

    public String readTextFromS3(String s3Path) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        s3Utils.streamFileFromS3(s3Path, outputStream);
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    public void writeTextToS3(String s3Path, String content) throws IOException {
        File tempFile = File.createTempFile("llm-output", ".txt");
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8))) {
            writer.write(content);
        }
        s3Utils.uploadFile(tempFile.getAbsolutePath(), s3Path);
        tempFile.delete();
    }
}
