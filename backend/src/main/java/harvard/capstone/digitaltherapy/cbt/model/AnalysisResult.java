package harvard.capstone.digitaltherapy.cbt.model;

import org.springframework.context.annotation.Bean;

public class AnalysisResult {
    private double congruenceScore;
    private String dominantEmotion;
    private String[] cognitiveDistortions;
    private String interpretation;
    private String[] followUpPrompts;

    public double getCongruenceScore() {
        return congruenceScore;
    }
    public void setCongruenceScore(double cs) {
        this.congruenceScore = cs;
    }

    public String getDominantEmotion() {
        return dominantEmotion;
    }
    public void setDominantEmotion(String de) {
        this.dominantEmotion = de;
    }

    public String[] getCognitiveDistortions() {
        return cognitiveDistortions;
    }
    public void setCognitiveDistortions(String[] cd) {
        this.cognitiveDistortions = cd;
    }

    public String getInterpretation() {
        return interpretation;
    }
    public void setInterpretation(String i) {
        this.interpretation = i;
    }

    public String[] getFollowUpPrompts() {
        return followUpPrompts;
    }
    public void setFollowUpPrompts(String[] fp) {
        this.followUpPrompts = fp;
    }

    @Override
    public String toString() {
        return "Analysis{" +
                "congruenceScore=" + congruenceScore +
                ", dominantEmotion='" + dominantEmotion + '\'' +
                ", cognitiveDistortions=" + String.join(", ", cognitiveDistortions) +
                ", interpretation='" + interpretation + '\'' +
                ", followUpPrompts=" + String.join(", ", followUpPrompts) +
                '}';
    }
}
