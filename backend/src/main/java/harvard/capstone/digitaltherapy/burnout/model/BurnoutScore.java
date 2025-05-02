package harvard.capstone.digitaltherapy.burnout.model;


import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class BurnoutScore {
    private final String sessionId;
    private final String userId;
    private final double overallScore;
    private final String explanation;

    public BurnoutScore(String sessionId, String userId,
                        double overallScore) {
        this(sessionId, userId, overallScore, null);
    }

    public BurnoutScore(String sessionId, String userId,
                        double overallScore, String explanation) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.overallScore = overallScore;
        this.explanation = explanation;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUserId() {
        return userId;
    }


    public double getOverallScore() {
        return overallScore;
    }

    public String getExplanation() {
        return explanation;
    }
}
