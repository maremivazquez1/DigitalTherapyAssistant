package harvard.capstone.digitaltherapy.burnout.fhir;

import harvard.capstone.digitaltherapy.burnout.model.BurnoutAssessmentResult;
import harvard.capstone.digitaltherapy.burnout.model.BurnoutQuestion;
import harvard.capstone.digitaltherapy.burnout.model.BurnoutUserResponse;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class BurnoutAssessmentFhirConverter {

    public QuestionnaireResponse convertToFhir(BurnoutAssessmentResult result) {
        QuestionnaireResponse response = new QuestionnaireResponse();

        // Set basic metadata
        response.setId(result.getSessionId());
        response.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
        response.setAuthored(Date.from(result.getCompletedAt()
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()));

        // Set subject reference
        response.setSubject(new Reference("Patient/" + result.getUserId()));

        // Add all question-answer pairs
        for (BurnoutQuestion question : result.getAssessment().getQuestions()) {
            String questionId = question.getQuestionId();
            BurnoutUserResponse userResponse = result.getResponses().get(questionId);

            if (userResponse != null) {
                QuestionnaireResponse.QuestionnaireResponseItemComponent item =
                        response.addItem();
                item.setLinkId(questionId);
                item.setText(question.getQuestion());
                item.addAnswer().setValue(new StringType(userResponse.getTextResponse()));
            }
        }

        // Add score as an item
        QuestionnaireResponse.QuestionnaireResponseItemComponent scoreItem =
                response.addItem();
        scoreItem.setLinkId("score");
        scoreItem.setText("Calculated Burnout Score");
        scoreItem.addAnswer().setValue(new DecimalType(result.getScore().getOverallScore()));

        // Add summary as an item
        QuestionnaireResponse.QuestionnaireResponseItemComponent summaryItem =
                response.addItem();
        summaryItem.setLinkId("summary");
        summaryItem.setText("Assessment Summary");
        summaryItem.addAnswer().setValue(new StringType(result.getSummary()));

        return response;
    }
}
