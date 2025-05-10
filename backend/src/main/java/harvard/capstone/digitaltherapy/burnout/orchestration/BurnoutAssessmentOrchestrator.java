package harvard.capstone.digitaltherapy.burnout.orchestration;

import harvard.capstone.digitaltherapy.burnout.model.*;
import harvard.capstone.digitaltherapy.burnout.workers.BurnoutWorker;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import harvard.capstone.digitaltherapy.workers.AudioAnalysisWorker;
import harvard.capstone.digitaltherapy.workers.VideoAnalysisWorker;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    // Add a logger instance with the class name
    private static final Logger logger = LoggerFactory.getLogger(BurnoutAssessmentOrchestrator.class);

    private final BurnoutWorker burnoutWorker;
    private final VideoAnalysisWorker videoAnalysisWorker;
    private final AudioAnalysisWorker audioAnalysisWorker;
    private final Map<String, BurnoutAssessmentSession> activeSessions;

    public BurnoutAssessmentOrchestrator() {
        logger.info("Initializing BurnoutAssessmentOrchestrator");
        this.burnoutWorker = new BurnoutWorker();
        this.videoAnalysisWorker = new VideoAnalysisWorker();
        this.audioAnalysisWorker = new AudioAnalysisWorker();
        this.activeSessions = new HashMap<>();
        logger.debug("BurnoutAssessmentOrchestrator components initialized successfully");
    }


    /**
     * Creates a new burnout assessment session and returns the session details including questions
     *
     * @param userId The ID of the user taking the assessment
     * @return A response object containing the session ID and assessment questions
     */
    public BurnoutSessionCreationResponse createAssessmentSession(String userId) {
        logger.info("Creating burnout assessment session for user: {}", userId);
        Instant startTime = Instant.now();

        // Generate a unique session ID
        String sessionId = UUID.randomUUID().toString();
        logger.debug("Generated session ID: {}", sessionId);

        try {
            // Generate a burnout assessment with questions
            logger.debug("Requesting burnout assessment generation from BurnoutWorker");
            BurnoutAssessment assessment = burnoutWorker.generateBurnoutAssessment();
            logger.debug("Received assessment with {} questions", assessment.getQuestions().size());

            // Create a new session
            BurnoutAssessmentSession session = new BurnoutAssessmentSession(
                    sessionId,
                    userId,
                    assessment,
                    LocalDateTime.now()
            );

            // Store the session
            activeSessions.put(sessionId, session);

            Instant endTime = Instant.now();
            logger.info("Created session {} for user {} with {} questions. Operation took {} ms",
                    sessionId, userId, assessment.getQuestions().size(),
                    Duration.between(startTime, endTime).toMillis());

            return new BurnoutSessionCreationResponse(
                    sessionId,
                    assessment.getQuestions()
            );
        } catch (Exception e) {
            logger.error("Failed to create assessment session for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
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
        logger.info("Recording response for session: {}, question: {}", sessionId, questionId);
        logger.debug("Response media - Audio URL: {}, Video URL: {}",
                audioUrl != null ? audioUrl : "none",
                videoUrl != null ? videoUrl : "none");

        BurnoutAssessmentSession session = getSession(sessionId);
        if (session == null) {
            logger.warn("Session not found: {}", sessionId);
            return false;
        }

        // Find the question
        BurnoutQuestion question = session.getAssessment().getQuestions().stream()
                .filter(q -> q.getQuestionId().equals(questionId))
                .findFirst()
                .orElse(null);

        if (question == null) {
            logger.error("Question {} not found in session {}", questionId, sessionId);
            return false;
        }

        Map<String, Object> multimodalInsights = new HashMap<>();

        // Process multimodal content only if URLs are provided
        if (videoUrl != null || audioUrl != null) {
            // Process video insights if video URL is provided
            if (videoUrl != null) {
                logger.info("Starting video analysis for session: {}, question: {}, URL: {}",
                        sessionId, questionId, videoUrl);
                processVideoResponse(sessionId, questionId, videoUrl);
            }

            // Process audio insights if audio URL is provided
            if (audioUrl != null) {
                logger.info("Starting audio analysis for session: {}, question: {}, URL: {}",
                        sessionId, questionId, audioUrl);
                processAudioResponse(sessionId, questionId, audioUrl);
            }
        } else {
            logger.debug("No multimodal content provided for question {} in session {}", questionId, sessionId);
        }

        // Record the response immediately with empty multimodal insights
        try {
            BurnoutUserResponse burnoutResponse = new BurnoutUserResponse(
                    questionId,
                    response,
                    multimodalInsights
            );

            session.getResponses().put(questionId, burnoutResponse);
            logger.info("Response successfully recorded for question {} in session {}", questionId, sessionId);

            // Log the total number of responses collected so far
            logger.debug("Session {} now has {} responses out of {} questions",
                    sessionId,
                    session.getResponses().size(),
                    session.getAssessment().getQuestions().size());

            return true;
        } catch (Exception e) {
            logger.error("Failed to record response for session {}, question {}: {}",
                    sessionId, questionId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Process video response and initiate analysis
     */
    private void processVideoResponse(String sessionId, String questionId, String videoUrl) {
        try {
            // Start async video analysis
            logger.debug("Submitting video for analysis - session: {}, question: {}", sessionId, questionId);
            videoAnalysisWorker.detectFacesFromVideoAsync(videoUrl)
                    .thenAccept(jsonResult -> {
                        logger.info("Video analysis completed for session: {}, question: {}, result size: {} bytes",
                                sessionId, questionId, jsonResult.getBytes().length);
                        logger.debug("Video analysis result sample (first 100 chars): {}",
                                jsonResult.length() > 100 ? jsonResult.substring(0, 100) + "..." : jsonResult);
                        updateResponseWithVideoAnalysis(sessionId, questionId, jsonResult);
                    })
                    .exceptionally(ex -> {
                        logger.error("Video analysis failed for session {}, question {}: {}",
                                sessionId, questionId, ex.getMessage(), ex);
                        return null;
                    });
        } catch (Exception e) {
            logger.error("Error initiating video analysis for session {}, question {}: {}",
                    sessionId, questionId, e.getMessage(), e);
        }
    }

    /**
     * Process audio response and initiate analysis
     */
    private void processAudioResponse(String sessionId, String questionId, String audioUrl) {
        try {
            // Start async audio analysis
            logger.debug("Submitting audio for analysis - session: {}, question: {}", sessionId, questionId);
            audioAnalysisWorker.analyzeAudioAsync(audioUrl)
                    .thenAccept(jsonResult -> {
                        logger.info("Audio analysis completed for session: {}, question: {}, result size: {} bytes",
                                sessionId, questionId, jsonResult.getBytes().length);
                        logger.debug("Audio analysis result sample (first 100 chars): {}",
                                jsonResult.length() > 100 ? jsonResult.substring(0, 100) + "..." : jsonResult);
                        updateResponseWithAudioAnalysis(sessionId, questionId, jsonResult);
                    })
                    .exceptionally(ex -> {
                        logger.error("Audio analysis failed for session {}, question {}: {}",
                                sessionId, questionId, ex.getMessage(), ex);
                        return null;
                    });
        } catch (Exception e) {
            logger.error("Error initiating audio analysis for session {}, question {}: {}",
                    sessionId, questionId, e.getMessage(), e);
        }
    }

    /**
     * Updates a recorded response with video analysis results
     */
    private void updateResponseWithVideoAnalysis(String sessionId, String questionId, String analysisJson) {
        logger.info("Applying video analysis results to session {}, question {}", sessionId, questionId);
        BurnoutAssessmentSession session = getSession(sessionId);
        if (session != null && session.getResponses().containsKey(questionId)) {
            try {
                BurnoutUserResponse response = session.getResponses().get(questionId);
                response.getMultimodalInsights().put("video", analysisJson);
                logger.info("Successfully updated response with video analysis for question {} in session {}",
                        questionId, sessionId);
            } catch (Exception e) {
                logger.error("Failed to update response with video analysis for session {}, question {}: {}",
                        sessionId, questionId, e.getMessage(), e);
            }
        } else {
            logger.warn("Cannot update video analysis - session {} or question {} not found", sessionId, questionId);
        }
    }

    /**
     * Updates a recorded response with audio analysis results
     */
    private void updateResponseWithAudioAnalysis(String sessionId, String questionId, String analysisJson) {
        logger.info("Applying audio analysis results to session {}, question {}", sessionId, questionId);
        BurnoutAssessmentSession session = getSession(sessionId);
        if (session != null && session.getResponses().containsKey(questionId)) {
            try {
                BurnoutUserResponse response = session.getResponses().get(questionId);
                response.getMultimodalInsights().put("audio", analysisJson);
                logger.info("Successfully updated response with audio analysis for question {} in session {}",
                        questionId, sessionId);
            } catch (Exception e) {
                logger.error("Failed to update response with audio analysis for session {}, question {}: {}",
                        sessionId, questionId, e.getMessage(), e);
            }
        } else {
            logger.warn("Cannot update audio analysis - session {} or question {} not found", sessionId, questionId);
        }
    }

    private String formatUserResponsesForWorker(BurnoutAssessmentSession session) {
        logger.debug("Formatting responses for worker processing - session: {}", session.getSessionId());
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

        String formatted = formattedInput.toString();
        logger.debug("Formatted worker input ({} characters): {}",
                formatted.length(),
                formatted.substring(0, Math.min(200, formatted.length())) + "...");

        return formatted;
    }

    /**
     * Calculates the burnout score based on the responses provided
     *
     * @param sessionId The ID of the assessment session
     * @return A BurnoutScore object with domain scores and overall score
     */
    private BurnoutScore calculateScore(String sessionId) {
        logger.info("Calculating burnout score for session {}", sessionId);
        Instant startTime = Instant.now();

        BurnoutAssessmentSession session = getSession(sessionId);
        if (session == null || session.getResponses().isEmpty()) {
            logger.error("Cannot calculate score - session {} not found or has no responses", sessionId);
            throw new IllegalStateException("No session found or no responses recorded");
        }

        try {
            String formattedInput = formatUserResponsesForWorker(session);
            logger.debug("Requesting burnout score calculation from BurnoutWorker");

            Map<String, Object> resultMap = burnoutWorker.generateBurnoutScore(formattedInput);

            double scoreValue = (double) resultMap.get("score");
            String explanation = (String) resultMap.get("explanation");
            logger.debug("Received score calculation: {} with explanation length: {} characters",
                    scoreValue, explanation.length());

            BurnoutScore score = new BurnoutScore(
                    sessionId,
                    session.getUserId(),
                    scoreValue,
                    explanation
            );

            session.setScore(score);

            Instant endTime = Instant.now();
            logger.info("Score calculated for session {}: {}. Operation took {} ms",
                    sessionId, scoreValue, Duration.between(startTime, endTime).toMillis());

            return score;
        } catch (Exception e) {
            logger.error("Failed to calculate score for session {}: {}", sessionId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Generates a summary of the burnout assessment results
     *
     * @param sessionId The ID of the assessment session
     * @return A BurnoutSummary object with insights and recommendations
     */
    private BurnoutSummary generateSummary(String sessionId) {
        logger.info("Generating burnout summary for session {}", sessionId);
        Instant startTime = Instant.now();

        BurnoutAssessmentSession session = getSession(sessionId);

        if (session == null || session.getScore() == null) {
            logger.error("Cannot generate summary - session {} not found or score not calculated", sessionId);
            throw new IllegalStateException("No session found or score not calculated");
        }

        try {
            String formattedInput = formatUserResponsesForWorker(session);
            logger.debug("Requesting burnout summary generation from BurnoutWorker");

            String overallInsight = burnoutWorker.generateBurnoutSummary(formattedInput);
            logger.debug("Received summary of length: {} characters", overallInsight.length());

            BurnoutSummary summary = new BurnoutSummary(sessionId, overallInsight);

            // Save the summary to the session
            session.setSummary(summary);

            Instant endTime = Instant.now();
            logger.info("Summary generated for session {}. Operation took {} ms",
                    sessionId, Duration.between(startTime, endTime).toMillis());

            return summary;
        } catch (Exception e) {
            logger.error("Failed to generate summary for session {}: {}", sessionId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Completes the burnout assessment process, calculating scores and generating a summary
     *
     * @param sessionId The ID of the assessment session
     * @return A BurnoutResult object with all assessment results
     */
    public BurnoutAssessmentResult completeAssessment(String sessionId) {
        logger.info("Completing assessment for session {}", sessionId);
        Instant startTime = Instant.now();

        BurnoutAssessmentSession session = getSession(sessionId);

        if (session == null) {
            logger.error("Cannot complete assessment - session {} not found", sessionId);
            throw new IllegalStateException("No session found");
        }

        try {
            // Log the number of responses received vs expected
            logger.debug("Session {} has {} responses out of {} questions",
                    sessionId,
                    session.getResponses().size(),
                    session.getAssessment().getQuestions().size());

            // Calculate score if not already done
            BurnoutScore score = session.getScore();
            if (score == null) {
                logger.debug("No score found for session {}, calculating now", sessionId);
                score = calculateScore(sessionId);
            } else {
                logger.debug("Using existing score for session {}: {}", sessionId, score.getOverallScore());
            }

            // Generate summary if not already done
            BurnoutSummary summary = session.getSummary();
            if (summary == null) {
                logger.debug("No summary found for session {}, generating now", sessionId);
                summary = generateSummary(sessionId);
            } else {
                logger.debug("Using existing summary for session {}", sessionId);
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

            Instant endTime = Instant.now();
            logger.info("Assessment completed for session {}. Score: {}. Operation took {} ms",
                    sessionId, score.getOverallScore(), Duration.between(startTime, endTime).toMillis());

            return result;
        } catch (Exception e) {
            logger.error("Failed to complete assessment for session {}: {}", sessionId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Retrieves a session by ID
     *
     * @param sessionId The ID of the session to retrieve
     * @return The BurnoutAssessmentSession object
     */
    private BurnoutAssessmentSession getSession(String sessionId) {
        BurnoutAssessmentSession session = activeSessions.get(sessionId);
        if (session == null) {
            logger.warn("Session not found: {}", sessionId);
        }
        return session;
    }
}