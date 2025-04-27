package harvard.capstone.digitaltherapy.burnout.model;

import java.time.LocalDateTime;
import java.util.Map;

public class BurnoutResult {
    private final String sessionId;
    private final String userId;
    private final BurnoutAssessment assessment;
    private final Map<String, BurnoutResponse> responses;
    private final BurnoutScore score;
    private final BurnoutSummary summary;
    private final LocalDateTime completedAt;

    public BurnoutResult(String sessionId, String userId, BurnoutAssessment assessment,
                         Map<String, BurnoutResponse> responses, BurnoutScore score,
                         BurnoutSummary summary, LocalDateTime completedAt) {
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

    public Map<String, BurnoutResponse> getResponses() {
        return responses;
    }

    public BurnoutScore getScore() {
        return score;
    }

    public BurnoutSummary getSummary() {
        return summary;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
