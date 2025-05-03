package harvard.capstone.digitaltherapy.burnout.orchestration;

import harvard.capstone.digitaltherapy.burnout.model.*;
import harvard.capstone.digitaltherapy.burnout.workers.BurnoutWorker;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
//    private final SessionManager sessionManager;
    private final Map<String, BurnoutAssessmentSession> activeSessions;

    public BurnoutAssessmentOrchestrator(BurnoutWorker burnoutWorker) {
        this.burnoutWorker = burnoutWorker;
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

        // If using session manager, also store there. We currently are not implementing this, so it's commented out.
        // sessionManager.saveSession(sessionId, session);

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
     * @param videoUrl URL to  S3 video response for multimodal questions
     * @param audioUrl Url to S3 audio response multimodal questions.
     * @return True if the response was successfully recorded
     */
    public boolean recordResponse(String sessionId, String questionId, String response, String videoUrl, String audioUrl) {
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
            return false;
        }

        /*  this is where we process the response for insights
         *  we need to call the other workers to process audio / facial
         *
         *
         */

        Map<String, Object> multimodalInsights = new HashMap<>();

        // Record the response
        BurnoutUserResponse burnoutResponse = new BurnoutUserResponse(
                questionId,
                response,
                multimodalInsights
        );

        session.getResponses().put(questionId, burnoutResponse);

        // Update the session in storage
//        sessionManager.saveSession(sessionId, session);

        return true;
    }

    /**
     * Calculates the burnout score based on the responses provided
     *
     * @param sessionId The ID of the assessment session
     * @return A BurnoutScore object with domain scores and overall score
     */
    public BurnoutScore calculateScore(String sessionId) {
        BurnoutAssessmentSession session = getSession(sessionId);
        if (session == null || session.getResponses().isEmpty()) {
            throw new IllegalStateException("No session found or no responses recorded");
        }

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

        Map<String, Object> resultMap = burnoutWorker.generateBurnoutScore(formattedInput.toString());

        double scoreValue = (double) resultMap.get("score");
        String explanation = (String) resultMap.get("explanation");

        BurnoutScore score = new BurnoutScore(
                sessionId,
                session.getUserId(),
                scoreValue,
                explanation
        );

        session.setScore(score);
        return score;
    }

    /**
     * Generates a summary of the burnout assessment results
     *
     * @param sessionId The ID of the assessment session
     * @return A BurnoutSummary object with insights and recommendations
     */
    public BurnoutSummary generateSummary(String sessionId) {
        BurnoutAssessmentSession session = getSession(sessionId);

        if (session == null || session.getScore() == null) {
            throw new IllegalStateException("No session found or score not calculated");
        }

        // Generate insights and recommendations based on the score
//        BurnoutSummary summary = burnoutWorker.generateSummary(
//                session.getScore(),
//                session.getAssessment().getQuestions(),
//                session.getResponses()
//        );

        BurnoutSummary summary = new BurnoutSummary(sessionId, "");

        // Save the summary to the session
        session.setSummary(summary);
//        sessionManager.saveSession(sessionId, session);

        return summary;
    }

    /**
     * Completes the burnout assessment process, calculating scores and generating a summary
     *
     * @param sessionId The ID of the assessment session
     * @return A BurnoutResult object with all assessment results
     */
    public BurnoutAssessmentResult completeAssessment(String sessionId) {
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
//        sessionManager.saveSession(sessionId, session);

        return result;
    }

    /**
     * Retrieves a session by ID
     *
     * @param sessionId The ID of the session to retrieve
     * @return The BurnoutAssessmentSession object
     */
    private BurnoutAssessmentSession getSession(String sessionId) {
        // Try to get from active sessions cache first

        // If not in cache, try to get from session manager
//        if (session == null) {
//            session = sessionManager.getSession(sessionId, BurnoutAssessmentSession.class);
//
//            // If found, add to active sessions cache
//            if (session != null) {
//                activeSessions.put(sessionId, session);
//            }
//        }

        return activeSessions.get(sessionId);
    }
}
