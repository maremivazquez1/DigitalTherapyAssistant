package harvard.capstone.digitaltherapy.burnout.model;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a complete burnout assessment with questions for all domains
 */
public class BurnoutAssessment {
    private final List<BurnoutQuestion> questions;

    public BurnoutAssessment(List<BurnoutQuestion> questions) {
        this.questions = questions;
    }

    public List<BurnoutQuestion> getQuestions() {
        return questions;
    }

    public List<BurnoutQuestion> getQuestionsForDomain(AssessmentDomain domain) {
        return questions.stream()
                .filter(q -> q.getDomain() == domain)
                .collect(Collectors.toList());
    }

    public List<BurnoutQuestion> getStandardQuestions() {
        return questions.stream()
                .filter(q -> !q.isMultimodal())
                .collect(Collectors.toList());
    }

    public List<BurnoutQuestion> getMultimodalQuestions() {
        return questions.stream()
                .filter(BurnoutQuestion::isMultimodal)
                .collect(Collectors.toList());
    }
}
