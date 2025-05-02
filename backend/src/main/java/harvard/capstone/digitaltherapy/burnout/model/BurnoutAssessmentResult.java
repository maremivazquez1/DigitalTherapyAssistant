package harvard.capstone.digitaltherapy.burnout.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents the response that is returned to the user after a burnout assessment has been completed.
 */
public class BurnoutAssessmentResult {
    private final String sessionId;
    private final String userId;
    private final BurnoutAssessment assessment;
    private final Map<String, BurnoutUserResponse> responses;
    private final BurnoutScore score;
    private final String summary;
    private final LocalDateTime completedAt;

    public BurnoutAssessmentResult(String sessionId, String userId, BurnoutAssessment assessment,
                                   Map<String, BurnoutUserResponse> responses, BurnoutScore score,
                                   String summary, LocalDateTime completedAt) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.assessment = assessment;
        this.responses = responses;
        this.score = score;
        this.summary = summary;
        this.completedAt = completedAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public BurnoutAssessment getAssessment() {
        return assessment;
    }

    public Map<String, BurnoutUserResponse> getResponses() {
        return responses;
    }

    public BurnoutScore getScore() {
        return score;
    }

    public String getSummary() {
        return summary;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
