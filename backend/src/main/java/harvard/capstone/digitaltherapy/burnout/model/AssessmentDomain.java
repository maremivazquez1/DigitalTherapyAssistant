package harvard.capstone.digitaltherapy.burnout.model;

/**
 * Class to represent assessment domains for burnout evaluation
 */
public enum AssessmentDomain {
    WORK("Work", "Questions about job stressors and workplace dynamics"),
    PERSONAL("Personal", "Questions about interpersonal relationships with friends, family, and partners"),
    LIFESTYLE("Lifestyle", "Questions about routines, sleep habits, and diet/nutrition");

    private final String displayName;
    private final String description;

    AssessmentDomain(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
