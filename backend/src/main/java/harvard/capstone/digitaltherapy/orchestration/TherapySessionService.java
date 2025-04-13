package harvard.capstone.digitaltherapy.orchestration;

public interface TherapySessionService {
    /**
     * Creates a new therapy session
     * @return The session ID for the new session
     */
    String createSession();

    /**
     * Processes a user text message and generates a response
     * @param sessionId The session identifier
     * @param userMessage The message from the user
     * @return The therapeutic response
     */
    String processUserMessage(String sessionId, String userMessage);

    /**
     * Processes a user voice/audio message and generates a response
     * @param sessionId The session identifier
     * @param transcribedText The transcribed text from audio
     * @return The therapeutic response
     */

    // can be added later
//    String processTranscribedAudioMessage(String sessionId, String transcribedText);

    /**
     * Checks if a session exists
     * @param sessionId The session identifier
     * @return True if the session exists, false otherwise
     */

    boolean sessionExists(String sessionId);
}
