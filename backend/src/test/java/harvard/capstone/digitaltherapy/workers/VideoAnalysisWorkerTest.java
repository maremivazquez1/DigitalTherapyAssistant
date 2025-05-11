package harvard.capstone.digitaltherapy.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class VideoAnalysisWorkerTest {

    private VideoAnalysisWorker worker;
    private RekognitionClient mockRekognition;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();

        // Create a subclass with a mocked Rekognition client
        worker = new VideoAnalysisWorker() {
            {
                try {
                    var field = VideoAnalysisWorker.class.getDeclaredField("rekognitionClient");
                    field.setAccessible(true);
                    mockRekognition = mock(RekognitionClient.class);
                    field.set(this, mockRekognition);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Test
    void parseS3Url_valid_returnsS3Object() throws Exception {
        Method method = VideoAnalysisWorker.class.getDeclaredMethod("parseS3Url", String.class);
        method.setAccessible(true);

        S3Object obj = (S3Object) method.invoke(worker, "s3://mybucket/myfile.mp4");

        assertEquals("mybucket", obj.bucket());
        assertEquals("myfile.mp4", obj.name());
    }

    @Test
    void parseS3Url_invalid_throwsException() throws Exception {
        Method method = VideoAnalysisWorker.class.getDeclaredMethod("parseS3Url", String.class);
        method.setAccessible(true);

        assertThrows(Exception.class, () -> method.invoke(worker, "http://badurl.com/file"));
    }

    @Test
    void mapToCustomResponse_convertsToTop3Emotions() throws Exception {
        // Mock AWS Rekognition emotions
        Emotion e1 = Emotion.builder().type("HAPPY").confidence((float) 0.7).build();
        Emotion e2 = Emotion.builder().type("SAD").confidence((float) 0.5).build();
        Emotion e3 = Emotion.builder().type("ANGRY").confidence((float) 0.3).build();
        Emotion e4 = Emotion.builder().type("CALM").confidence((float) 0.1).build();

        FaceDetail awsFaceDetail = FaceDetail.builder()
                .confidence(0.95f)
                .emotions(List.of(e1, e2, e3, e4))
                .build();

        software.amazon.awssdk.services.rekognition.model.FaceDetection awsFace =
                software.amazon.awssdk.services.rekognition.model.FaceDetection.builder()
                        .timestamp(1234L)
                        .face(awsFaceDetail)
                        .build();

        GetFaceDetectionResponse response = GetFaceDetectionResponse.builder()
                .jobStatus(VideoJobStatus.SUCCEEDED)
                .faces(List.of(awsFace))
                .build();

        Method method = VideoAnalysisWorker.class.getDeclaredMethod("mapToCustomResponse", GetFaceDetectionResponse.class);
        method.setAccessible(true);

        Object result = method.invoke(worker, response);
        String json = objectMapper.writeValueAsString(result);

        assertTrue(json.contains("HAPPY"));
        assertTrue(json.contains("SAD"));
        assertFalse(json.contains("CALM")); // not in top 3
    }

    @Test
    void detectFacesFromVideoAsync_startsJobAndReturnsFuture() {
        StartFaceDetectionResponse mockStartResponse = StartFaceDetectionResponse.builder()
                .jobId("job-123")
                .build();

        when(mockRekognition.startFaceDetection(any(StartFaceDetectionRequest.class)))
                .thenReturn(mockStartResponse);

        CompletableFuture<String> future = worker.detectFacesFromVideoAsync("s3://mybucket/myvideo.mp4");

        assertNotNull(future);
        assertFalse(future.isDone()); // Not completed until polling returns
    }
}
