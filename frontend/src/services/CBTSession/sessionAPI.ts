/**
 * sessionAPI.ts
 * 
 * This file provides API functions for managing CBT session lifecycle.
 * It allows creating, retrieving, and ending sessions, as well as fetching session history.
 * 
 * Requirements:
 * - Axios must be installed (`npm install axios`).
 * - The backend API should be running and accessible at the specified `API_BASE_URL`.
 * - Ensure `userId` and `sessionId` are correctly obtained before making API calls.
 * 
 * Functions:
 * - startSession(userId: string): Starts a new CBT session.
 * - getSession(sessionId: string): Retrieves an active session.
 * - endSession(sessionId: string): Ends a session.
 * - getSessionHistory(userId: string): Fetches past session history.
 */

import axios from "axios";
import { Session, SessionHistory } from "../../types/CBTSession/session";

// update with the correct API base URL
const API_BASE_URL = "https://api.yourapp.com/session";

export const startSession = async (userId: string): Promise<Session> => {
    try {
        const response = await axios.post(`${API_BASE_URL}/start`, { userId });
        return response.data;
    } catch (error) {
        console.error("Error starting session:", error);
        throw error;
    }
};

export const getSession = async (sessionId: string): Promise<Session> => {
    try {
        const response = await axios.get(`${API_BASE_URL}/retrieve/${sessionId}`);
        return response.data;
    } catch (error) {
        console.error("Error retrieving session:", error);
        throw error;
    }
};

export const endSession = async (sessionId: string): Promise<{ success: boolean; message: string }> => {
    try {
        const response = await axios.delete(`${API_BASE_URL}/end/${sessionId}`);
        return response.data;
    } catch (error) {
        console.error("Error ending session:", error);
        throw error;
    }
};

export const getSessionHistory = async (userId: string): Promise<SessionHistory> => {
    try {
        const response = await axios.get(`${API_BASE_URL}/history/${userId}`);
        return response.data;
    } catch (error) {
        console.error("Error fetching session history:", error);
        throw error;
    }
};
