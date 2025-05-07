package harvard.capstone.digitaltherapy.burnout.orchestration;

import harvard.capstone.digitaltherapy.burnout.model.*;
import harvard.capstone.digitaltherapy.burnout.workers.BurnoutWorker;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import harvard.capstone.digitaltherapy.workers.AudioAnalysisWorker;
import harvard.capstone.digitaltherapy.workers.VideoAnalysisWorker;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the burnout assessment process including:
 * - Creating and managing assessment sessions
 * - Generating assessment questions
 * - Collecting and aggregating user responses
 * - Calculating burnout scores
 * - Generating assessment summaries
 */
@Service
public class BurnoutAssessmentOrchestrator {

    private final BurnoutWorker burnoutWorker;
    private final VideoAnalysisWorker videoAnalysisWorker;
    private final AudioAnalysisWorker audioAnalysisWorker;
    private final Map<String, BurnoutAssessmentSession> activeSessions;

    public BurnoutAssessmentOrchestrator() {
        this.burnoutWorker = new BurnoutWorker();
        this.videoAnalysisWorker = new VideoAnalysisWorker();
        this.audioAnalysisWorker = new AudioAnalysisWorker();
        this.activeSessions = new HashMap<>();
    }


    /**
     * Creates a new burnout assessment session and returns the session details including questions
     *
     * @param userId The ID of the user taking the assessment
     * @return A response object containing the session ID and assessment questions
     */
    public BurnoutSessionCreationResponse createAssessmentSession(String userId) {
        // Generate a unique session ID
        String sessionId = UUID.randomUUID().toString();

        // Generate a burnout assessment with questions
        BurnoutAssessment assessment = burnoutWorker.generateBurnoutAssessment();

        // Create a new session
        BurnoutAssessmentSession session = new BurnoutAssessmentSession(
                sessionId,
                userId,
                assessment,
                LocalDateTime.now()
        );

        // Store the session
        activeSessions.put(sessionId, session);

        System.out.println("Created session " + sessionId + " for user " + userId + " with " + assessment.getQuestions().size() + " questions.");

        return new BurnoutSessionCreationResponse(
                sessionId,
                assessment.getQuestions()
        );
    }

    /**
     * Records a user's response to a burnout assessment question
     *
     * @param sessionId The ID of the assessment session
     * @param questionId The ID of the question being answered
     * @param response The user's response
     * @param videoUrl URL to S3 video response for multimodal questions
     * @param audioUrl URL to S3 audio response multimodal questions
     * @return True if the response was successfully recorded
     */
    public boolean recordResponse(String sessionId, String questionId, String response, String videoUrl, String audioUrl) {
        System.out.println("Recording response for session " + sessionId + ", question " + questionId);
        System.out.println("Audio URL: " + audioUrl + ", Video URL: " + videoUrl);

        BurnoutAssessmentSession session = getSession(sessionId);
        if (session == null) {
            return false;
        }

        // Find the question
        BurnoutQuestion question = session.getAssessment().getQuestions().stream()
                .filter(q -> q.getQuestionId().equals(questionId))
                .findFirst()
                .orElse(null);

        if (question == null) {
            System.err.println("Question " + questionId + " not found in session " + sessionId);
            return false;
        }

        Map<String, Object> multimodalInsights = new HashMap<>();

        // Process multimodal content only if URLs are provided
        if (videoUrl != null || audioUrl != null) {
            // Process video insights if video URL is provided
            if (videoUrl != null) {
                System.out.println("Starting video analysis for: " + videoUrl);

                // Start async video analysis
                videoAnalysisWorker.detectFacesFromVideoAsync(videoUrl)
                        .thenAccept(jsonResult -> {
                            System.out.println("Video analysis completed for question " + questionId);
                            updateResponseWithVideoAnalysis(sessionId, questionId, jsonResult);
                        })
                        .exceptionally(ex -> {
                            System.err.println("Video analysis failed: " + ex.getMessage());
                            return null;
                        });
            }

            // Process audio insights if audio URL is provided
            if (audioUrl != null) {
                System.out.println("Starting audio analysis for: " + audioUrl);

                // Start async audio analysis
                audioAnalysisWorker.analyzeAudioAsync(audioUrl)
                        .thenAccept(jsonResult -> {
                            System.out.println("Audio analysis completed for question " + questionId);
                            updateResponseWithAudioAnalysis(sessionId, questionId, jsonResult);
                        })
                        .exceptionally(ex -> {
                            System.err.println("Audio analysis failed: " + ex.getMessage());
                            return null;
                        });
            }
        } else {
            System.out.println("No multimodal content provided for question " + questionId);
        }

        // Record the response immediately with empty multimodal insights
        BurnoutUserResponse burnoutResponse = new BurnoutUserResponse(
                questionId,
                response,
                multimodalInsights
        );

        session.getResponses().put(questionId, burnoutResponse);
        System.out.println("Response recorded for question " + questionId + " in session " + sessionId);

        return true;
    }

    /**
     * Updates a recorded response with video analysis results
     */
    private void updateResponseWithVideoAnalysis(String sessionId, String questionId, String analysisJson) {
        System.out.println("Applying video analysis results to session " + sessionId + ", question " + questionId);
        BurnoutAssessmentSession session = getSession(sessionId);
        if (session != null && session.getResponses().containsKey(questionId)) {
            BurnoutUserResponse response = session.getResponses().get(questionId);
            response.getMultimodalInsights().put("video", analysisJson);
            System.out.println("Updated response with video analysis for question " + questionId);
        }
    }

    /**
     * Updates a recorded response with audio analysis results
     */
    private void updateResponseWithAudioAnalysis(String sessionId, String questionId, String analysisJson) {
        System.out.println("Applying audio analysis results to session " + sessionId + ", question " + questionId);
        BurnoutAssessmentSession session = getSession(sessionId);
        if (session != null && session.getResponses().containsKey(questionId)) {
            BurnoutUserResponse response = session.getResponses().get(questionId);
            response.getMultimodalInsights().put("audio", analysisJson);
            System.out.println("Updated response with audio analysis for question " + questionId);
        }
    }



    private String formatUserResponsesForWorker(BurnoutAssessmentSession session) {
        List<BurnoutQuestion> questions = session.getAssessment().getQuestions();
        Map<String, BurnoutUserResponse> responses = session.getResponses();

        StringBuilder formattedInput = new StringBuilder();
        for (BurnoutQuestion question : questions) {
            String questionId = question.getQuestionId();
            BurnoutUserResponse response = responses.get(questionId);

            formattedInput.append("Q: ").append(question.getQuestion()).append("\n");

            if (response != null) {
                formattedInput.append("A: ").append(response.getTextResponse()).append("\n");

                if (response.getMultimodalInsights() != null && !response.getMultimodalInsights().isEmpty()) {
                    formattedInput.append("Multimodal: ").append(response.getMultimodalInsights().toString()).append("\n");
                }
            } else {
                formattedInput.append("A: [No response]\n");
            }

            formattedInput.append("---\n");
        }

        System.out.println("Formatted input for worker: \n" + formattedInput.substring(0, Math.min(200, formattedInput.length())) + "...");

        return formattedInput.toString();
    }

    /**
     * Calculates the burnout score based on the responses provided
     *
     * @param sessionId The ID of the assessment session
     * @return A BurnoutScore object with domain scores and overall score
     */
    private BurnoutScore calculateScore(String sessionId) {
        System.out.println("Calculating burnout score for session " + sessionId);
        BurnoutAssessmentSession session = getSession(sessionId);
        if (session == null || session.getResponses().isEmpty()) {
            throw new IllegalStateException("No session found or no responses recorded");
        }

        String formattedInput = formatUserResponsesForWorker(session);

        Map<String, Object> resultMap = burnoutWorker.generateBurnoutScore(formattedInput);

        double scoreValue = (double) resultMap.get("score");
        String explanation = (String) resultMap.get("explanation");

        BurnoutScore score = new BurnoutScore(
                sessionId,
                session.getUserId(),
                scoreValue,
                explanation
        );

        session.setScore(score);
        System.out.println("Score calculated: " + scoreValue + ", Explanation: " + explanation);
        return score;
    }

    /**
     * Generates a summary of the burnout assessment results
     *
     * @param sessionId The ID of the assessment session
     * @return A BurnoutSummary object with insights and recommendations
     */
    private BurnoutSummary generateSummary(String sessionId) {
        System.out.println("Generating burnout summary for session " + sessionId);
        BurnoutAssessmentSession session = getSession(sessionId);

        if (session == null || session.getScore() == null) {
            throw new IllegalStateException("No session found or score not calculated");
        }

        String formattedInput = formatUserResponsesForWorker(session);

        String overallInsight = burnoutWorker.generateBurnoutSummary(formattedInput);

        BurnoutSummary summary = new BurnoutSummary(sessionId, overallInsight);

        // Save the summary to the session
        session.setSummary(summary);

        System.out.println("Summary generated: " + overallInsight);
        return summary;
    }

    /**
     * Completes the burnout assessment process, calculating scores and generating a summary
     *
     * @param sessionId The ID of the assessment session
     * @return A BurnoutResult object with all assessment results
     */
    public BurnoutAssessmentResult completeAssessment(String sessionId) {
        System.out.println("Completing assessment for session " + sessionId);
        BurnoutAssessmentSession session = getSession(sessionId);

        if (session == null) {
            throw new IllegalStateException("No session found");
        }

        // Calculate score if not already done
        BurnoutScore score = session.getScore();
        if (score == null) {
            score = calculateScore(sessionId);
        }

        // Generate summary if not already done
        BurnoutSummary summary = session.getSummary();
        if (summary == null) {
            summary = generateSummary(sessionId);
        }

        // Create result object
        BurnoutAssessmentResult result = new BurnoutAssessmentResult(
                sessionId,
                session.getUserId(),
                session.getAssessment(),
                session.getResponses(),
                score,
                summary.getOverallInsight(),
                LocalDateTime.now()
        );

        // Mark session as complete
        session.setCompleted(true);
        session.setCompletedAt(LocalDateTime.now());

        System.out.println("Assessment completed at " + session.getCompletedAt());
        return result;
    }

    /**
     * Retrieves a session by ID
     *
     * @param sessionId The ID of the session to retrieve
     * @return The BurnoutAssessmentSession object
     */
    private BurnoutAssessmentSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }
}
