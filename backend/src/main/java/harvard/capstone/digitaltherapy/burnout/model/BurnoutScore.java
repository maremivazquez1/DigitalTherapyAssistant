package harvard.capstone.digitaltherapy.burnout.model;

import java.time.LocalDateTime;
import java.util.Map;


public class BurnoutScore {
    private final String sessionId;
    private final String userId;
    private final Map<AssessmentDomain, Double> domainScores;
    private final double overallScore;
    private final LocalDateTime calculatedAt;

    public BurnoutScore(String sessionId, String userId, Map<AssessmentDomain, Double> domainScores,
                        double overallScore, LocalDateTime calculatedAt) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.domainScores = domainScores;
        this.overallScore = overallScore;
        this.calculatedAt = calculatedAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public Map<AssessmentDomain, Double> getDomainScores() {
        return domainScores;
    }

    public double getOverallScore() {
        return overallScore;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }
}
