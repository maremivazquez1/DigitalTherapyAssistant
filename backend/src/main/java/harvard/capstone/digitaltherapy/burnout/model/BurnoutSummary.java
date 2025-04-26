package harvard.capstone.digitaltherapy.burnout.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class BurnoutSummary {
    private final String sessionId;
    private final String overallInsight;

    public BurnoutSummary(String sessionId, String overallInsight) {
        this.sessionId = sessionId;
        this.overallInsight = overallInsight;
    }

    // Getters omitted for brevity

    public String getSessionId() {
        return sessionId;
    }

    public String getOverallInsight() {
        return overallInsight;
    }
}