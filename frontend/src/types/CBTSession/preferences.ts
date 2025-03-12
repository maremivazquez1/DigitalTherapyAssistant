// defines the user preferences for the CBT session
export interface UserPreferences {
    enableVoice: boolean;
    language: string;
    responseSpeed: "slow" | "normal" | "fast";
}