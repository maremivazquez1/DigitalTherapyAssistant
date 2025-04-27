// BurnoutResponse.java
package harvard.capstone.digitaltherapy.burnout.model;

import java.time.LocalDateTime;
import java.util.Map;

public class BurnoutResponse {
    private final String questionId;
    private final String textResponse;
    private final Map<String, Object> multimodalInsights;
    private final LocalDateTime timestamp;

    public BurnoutResponse(String questionId, String textResponse, Map<String, Object> multimodalInsights, LocalDateTime timestamp) {
        this.questionId = questionId;
        this.textResponse = textResponse;
        this.multimodalInsights = multimodalInsights;
        this.timestamp = timestamp;
    }

    public String getQuestionId() {
        return questionId;
    }

    public String getTextResponse() {
        return textResponse;
    }

    public Map<String, Object> getMultimodalInsights() {
        return multimodalInsights;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
