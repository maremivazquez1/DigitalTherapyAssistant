package harvard.capstone.digitaltherapy.burnout.model;


/**
 * Represents a complete burnout assessment question
 */
public class BurnoutQuestion {
    private final String question;
    private final String questionId;
    private final AssessmentDomain domain;
    private final boolean isMultimodal;

    public BurnoutQuestion(String questionId, String question, AssessmentDomain domain, boolean isMultimodal) {
        this.questionId = questionId;
        this.question = question;
        this.domain = domain;
        this.isMultimodal = isMultimodal;
    }

    public String getQuestion() {
        return question;
    }

    public AssessmentDomain getDomain() {
        return domain;
    }

    public boolean isMultimodal() {
        return isMultimodal;
    }

    public String getQuestionId() {
        return questionId;
    }

    @Override
    public String toString() {
        return question;
    }
}
