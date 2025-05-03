package harvard.capstone.digitaltherapy.burnout.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class BurnoutAssessmentSession {
    private final String sessionId;
    private final String userId;
    private final BurnoutAssessment assessment;
    private final Map<String, BurnoutUserResponse> responses;
    private LocalDateTime completedAt;
    private boolean completed;
    private BurnoutScore score;
    private BurnoutSummary summary;

    public BurnoutAssessmentSession(String sessionId, String userId, BurnoutAssessment assessment, LocalDateTime createdAt) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.assessment = assessment;
        this.responses = new HashMap<>();
        this.completed = false;
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

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public BurnoutScore getScore() {
        return score;
    }

    public void setScore(BurnoutScore score) {
        this.score = score;
    }

    public BurnoutSummary getSummary() {
        return summary;
    }

    public void setSummary(BurnoutSummary summary) {
        this.summary = summary;
    }
}
