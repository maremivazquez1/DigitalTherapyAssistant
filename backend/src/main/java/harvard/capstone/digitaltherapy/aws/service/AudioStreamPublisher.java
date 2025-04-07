package harvard.capstone.digitaltherapy.aws.service;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import harvard.capstone.digitaltherapy.cbt.controller.CBTController;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.transcribestreaming.model.AudioEvent;
import software.amazon.awssdk.services.transcribestreaming.model.AudioStream;
import software.amazon.awssdk.core.SdkBytes;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class AudioStreamPublisher implements SdkPublisher<AudioStream> {
    private static final Logger logger = LoggerFactory.getLogger(CBTController.class);

    private final List<byte[]> buffer = new ArrayList<>(); // Store audio chunks
    private final List<Subscriber<? super AudioStream>> subscribers = new CopyOnWriteArrayList<>();
    private boolean isCompleted = false;

    // Publishes a new chunk of audio data
    public void publish(byte[] audioData) {
        logger.info("ASP - Publishing audio data chunk of size: {}", audioData.length);
        buffer.add(audioData);
        if (!isCompleted) {
            pushChunks(); // Push the buffered chunks
        }
    }

    // This method handles pushing the buffered audio chunks to subscribers
    private void pushChunks() {
        // Check if there are buffered chunks to send
        if (buffer.isEmpty()) {
            return; // No chunks to push
        }

        // Loop through each chunk in the buffer and send it to subscribers
        for (byte[] chunk : buffer) {
            // Convert the byte array to SdkBytes
            SdkBytes sdkBytes = SdkBytes.fromByteArray(chunk);

            // Create an AudioEvent using the builder and set the audio chunk
            AudioEvent audioEvent = AudioEvent.builder()
                .audioChunk(sdkBytes)  // Set the audio data
                .build();

            // Notify all subscribers with the AudioStream (or AudioEvent)
            for (Subscriber<? super AudioStream> subscriber : subscribers) {
                subscriber.onNext(audioEvent); // Publish the chunk
            }
        }

        // Clear the buffer after sending the chunks
        buffer.clear();
    }

    @Override
    public void subscribe(Subscriber<? super AudioStream> subscriber) {
        // Add the subscriber to the list
        subscribers.add(subscriber);

        // Create a Subscription for the subscriber
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                // Handle request for more data (we push as soon as data is available)
            }

            @Override
            public void cancel() {
                // Remove the subscriber from the list if the subscription is canceled
                subscribers.remove(subscriber);
            }
        });
    }

    // Mark the stream as completed and notify all subscribers
    public void complete() {
        isCompleted = true;
        for (Subscriber<? super AudioStream> subscriber : subscribers) {
            subscriber.onComplete(); // Notify subscribers that the stream is complete
        }
    }

    // Check if the stream is completed
    public boolean isCompleted() {
        return isCompleted;
    }
}
