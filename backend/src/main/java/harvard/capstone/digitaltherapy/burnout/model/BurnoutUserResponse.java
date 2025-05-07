// BurnoutResponse.java
package harvard.capstone.digitaltherapy.burnout.model;


import java.util.Map;


public class BurnoutUserResponse {
    private final String questionId;
    private final String textResponse;
    private final Map<String, Object> multimodalInsights;

    public BurnoutUserResponse(String questionId, String textResponse, Map<String, Object> multimodalInsights) {
        this.questionId = questionId;
        this.textResponse = textResponse;
        this.multimodalInsights = multimodalInsights;
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
}
