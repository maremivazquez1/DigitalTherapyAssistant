package harvard.capstone.digitaltherapy.burnout.orchestration;

import harvard.capstone.digitaltherapy.burnout.model.*;
import harvard.capstone.digitaltherapy.burnout.workers.BurnoutWorker;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates the burnout assessment process including:
 * - Creating and managing assessment sessions
 * - Generating assessment questions
 * - Collecting and aggregating user responses
 * - Calculating burnout scores
 * - Generating assessment summaries
 */
public class BurnoutAssessmentOrchestrator {

    private final BurnoutWorker burnoutWorker;
//    private final SessionManager sessionManager;
    private final Map<String, BurnoutAssessmentSession> activeSessions;

    public BurnoutAssessmentOrchestrator(BurnoutWorker burnoutWorker) {
        this.burnoutWorker = burnoutWorker;
        this.activeSessions = new HashMap<>();
    }

    /**
     * Creates a new burnout assessment session
     *
     * @param userId The ID of the user taking the assessment
     * @return The session ID for the new assessment
     */
    public String createAssessmentSession(String userId) {
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
//        sessionManager.saveSession(sessionId, session);

        return sessionId;
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
        BurnoutResponse burnoutResponse = new BurnoutResponse(
                questionId,
                response,
                multimodalInsights,
                LocalDateTime.now()
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

        // Collect all responses
        List<BurnoutQuestion> questions = session.getAssessment().getQuestions();
        Map<String, BurnoutResponse> responses = session.getResponses();

        // Calculate scores for each domain
        Map<AssessmentDomain, Double> domainScores = new HashMap<>();

        for (AssessmentDomain domain : AssessmentDomain.values()) {
            List<BurnoutResponse> domainResponses = questions.stream()
                    .filter(q -> q.getDomain() == domain)
                    .map(q -> responses.get(q.getQuestionId()))
                    .filter(r -> r != null)
                    .toList();

            if (!domainResponses.isEmpty()) {
                // Ask the BurnoutWorker to calculate domain score
                double score = 5.0;
//                double score = burnoutWorker.calculateDomainScore(domain, domainResponses);
                domainScores.put(domain, score);
            }
        }

        // Calculate overall score
        double overallScore = 0.0;
//        double overallScore = burnoutWorker.calculateOverallScore(domainScores);

        // Create and return the score
        BurnoutScore score = new BurnoutScore(
                sessionId,
                session.getUserId(),
                domainScores,
                overallScore,
                LocalDateTime.now()
        );

        // Save the score to the session
        session.setScore(score);
//        sessionManager.saveSession(sessionId, session);

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
    public BurnoutResult completeAssessment(String sessionId) {
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
        BurnoutResult result = new BurnoutResult(
                sessionId,
                session.getUserId(),
                session.getAssessment(),
                session.getResponses(),
                score,
                summary,
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
    public BurnoutAssessmentSession getSession(String sessionId) {
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
