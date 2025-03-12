import axios from "axios";
import { UserPreferences } from "../../types/CBTSession/preferences";

//replace with the real API base URL
const API_BASE_URL = "https://api.yourapp.com/preferences";

/**
 * Saves user preferences to the backend.
 */
export const savePreferences = async (preferences: UserPreferences): Promise<UserPreferences> => {
    try {
        const response = await axios.post(`${API_BASE_URL}/save`, preferences);
        return response.data;
    } catch (error) {
        console.error("Error saving preferences:", error);
        throw error;
    }
};

/**
 * Loads user preferences from the backend.
 */
export const loadPreferences = async (): Promise<UserPreferences> => {
    try {
        const response = await axios.get(`${API_BASE_URL}/load`);
        return response.data;
    } catch (error) {
        console.error("Error loading preferences:", error);
        throw error;
    }
};