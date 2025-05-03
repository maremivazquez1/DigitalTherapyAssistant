package harvard.capstone.digitaltherapy.burnout.model;

import java.util.List;
import java.util.Objects;

/**
 * Response object for the creation of a new burnout assessment session.
 * Contains the session ID and the list of questions to be presented to the user.
 */
public class BurnoutSessionCreationResponse {

    private final String sessionId;
    private final List<BurnoutQuestion> questions;

    /**
     * Creates a new BurnoutSessionCreationResponse
     *
     * @param sessionId The unique identifier for the burnout assessment session
     * @param questions The list of burnout assessment questions to present to the user
     */
    public BurnoutSessionCreationResponse(String sessionId, List<BurnoutQuestion> questions) {
        this.sessionId = sessionId;
        this.questions = List.copyOf(questions); // Create immutable copy
    }

    /**
     * @return The unique identifier for the burnout assessment session
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * @return The list of burnout assessment questions to present to the user
     */
    public List<BurnoutQuestion> getQuestions() {
        return questions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BurnoutSessionCreationResponse that = (BurnoutSessionCreationResponse) o;
        return Objects.equals(sessionId, that.sessionId) &&
                Objects.equals(questions, that.questions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, questions);
    }

    @Override
    public String toString() {
        return "BurnoutSessionCreationResponse{" +
                "sessionId='" + sessionId + '\'' +
                ", questionsCount=" + questions.size() +
                '}';
    }
}
