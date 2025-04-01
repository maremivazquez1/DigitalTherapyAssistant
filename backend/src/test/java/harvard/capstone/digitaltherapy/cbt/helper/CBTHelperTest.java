package harvard.capstone.digitaltherapy.cbt.helper;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import harvard.capstone.digitaltherapy.utility.S3Utils;
import harvard.capstone.digitaltherapy.cbt.service.CBTHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.io.*;
import java.util.Base64;

class CBTHelperTest {

    @Mock
    private S3Utils s3Service;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private CBTHelper cbtHelper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testDownloadTextFile_Success() throws IOException {
        String fileName = "test.txt";
        OutputStream mockOutputStream = mock(OutputStream.class);

        doAnswer(invocation -> {
            OutputStream os = invocation.getArgument(2);
            os.write("Mock file content".getBytes());
            return null;
        }).when(s3Service).streamFileFromS3(anyString(), eq(fileName), any(OutputStream.class));

        ResponseEntity<StreamingResponseBody> response = cbtHelper.downloadTextFile(fileName);
        assertNotNull(response.getBody());
    }

    @Test
    void testDownloadTextFile_InvalidFileName() {
        ResponseEntity<StreamingResponseBody> response = cbtHelper.downloadTextFile("   ");
        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void testConvertMultiPartToBinaryFile() throws IOException {
        when(multipartFile.getOriginalFilename()).thenReturn("testFile.txt");
        when(multipartFile.getBytes()).thenReturn("Test file content".getBytes());

        File file = cbtHelper.convertMultiPartToBinaryFile(multipartFile);
        assertNotNull(file);
        assertEquals("testFile.txt", file.getName());
    }

    @Test
    void testCreateMultipartFileFromBase64() throws IOException {
        String base64String = Base64.getEncoder().encodeToString("Test audio content".getBytes());
        MultipartFile file = cbtHelper.createMultipartFileFromBase64(base64String, "audio.mp3");

        assertNotNull(file);
        assertEquals("audio.mp3", file.getOriginalFilename());
        assertEquals("audio/mpeg", file.getContentType());
        assertArrayEquals("Test audio content".getBytes(), file.getBytes());
    }

    @Test
    void testConvertFileToBase64() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("Test file content");
        }

        String base64String = cbtHelper.convertFileToBase64(tempFile);
        assertEquals(Base64.getEncoder().encodeToString("Test file content".getBytes()), base64String);
    }
}
