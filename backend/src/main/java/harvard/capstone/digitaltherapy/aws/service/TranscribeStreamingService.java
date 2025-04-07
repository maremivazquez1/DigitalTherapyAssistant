package harvard.capstone.digitaltherapy.aws.service;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.transcribestreaming.TranscribeStreamingAsyncClient;
import software.amazon.awssdk.services.transcribestreaming.model.*;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TranscribeStreamingService {

    private static final Logger logger = LoggerFactory.getLogger(TranscribeStreamingService.class);

    @Autowired
    private TranscribeStreamingAsyncClient amazonTranscribeStream;

    // TranscriptionListener interface for handling events
    public interface TranscriptionListener {
        void onTranscript(String text);  // Handle new transcript
        void onComplete();              // Handle completion
        void onError(Throwable error);  // Handle errors
    }

    // Constructor initializing Transcribe client
    public TranscribeStreamingService() {
        this.amazonTranscribeStream = TranscribeStreamingAsyncClient.builder()
            .credentialsProvider(DefaultCredentialsProvider.create())
            .region(Region.US_EAST_1)
            .build();
    }

    /**
     * Starts transcription on an audio stream.
     *
     * @param audioStream A Publisher of AudioStream (to stream audio)
     * @param listener    A listener for transcription results and events
     * @return CompletableFuture that completes when the transcription finishes
     */
    public CompletableFuture<Void> startTranscription(SdkPublisher<AudioStream> audioStream, TranscriptionListener listener) {
        // Build the transcription request
        StartStreamTranscriptionRequest request = StartStreamTranscriptionRequest.builder()
                .languageCode(LanguageCode.EN_US) // Set language code
                .mediaEncoding(MediaEncoding.PCM) // Set media encoding (PCM audio)
                .mediaSampleRateHertz(16000) // Set sample rate (16kHz)
                .build();

        logger.info("Starting transcription with request: {}", request);

        // Create a response handler to process transcription events
        StartStreamTranscriptionResponseHandler responseHandler = StartStreamTranscriptionResponseHandler.builder()
                .onResponse(response -> {
                    logger.info("Received response from transcription service: {}", response);
                    // Optionally handle the initial response (e.g., log or handle metadata)
                })
                .onError(error -> {
                    logger.error("Error during transcription: {}", error.getMessage(), error);
                    listener.onError(error);
                })
                .onComplete(() -> {
                    logger.info("Transcription completed.");
                    listener.onComplete();  // Notify listener when transcription completes
                })
                .subscriber(event -> {
                    logger.info("Received transcription event: {}", event);
                    if (event instanceof TranscriptEvent) {
                        TranscriptEvent transcriptEvent = (TranscriptEvent) event;
                        logger.info("TranscriptEvent details: {}", transcriptEvent);
                        transcriptEvent.transcript().results().forEach(result -> {
                            if (!result.isPartial() && !result.alternatives().isEmpty()) {
                                String transcript = result.alternatives().get(0).transcript();
                                logger.info("Transcription result: {}", transcript);
                                listener.onTranscript(transcript);
                            } else {
                                logger.info("Partial result received: {}", result);
                            }
                        });
                    }
                })
                .build();

        // Start streaming transcription and return a CompletableFuture for async handling
        return amazonTranscribeStream.startStreamTranscription(request, audioStream, responseHandler)
            .handleAsync((response, throwable) -> {
                if (throwable != null) {
                    logger.error("Error occurred during transcription: {}", throwable.getMessage(), throwable);
                    listener.onError(throwable);  // Handle any errors that occurred
                } else {
                    logger.info("Transcription stream started successfully.");
                }
                return null;  // Return null as we don't need to return any specific value
            });
    }
}
