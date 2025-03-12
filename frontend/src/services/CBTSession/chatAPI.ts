/**
 * chatAPI.ts
 * 
 * This file provides API functions for handling chat interactions in the CBT session.
 * It allows sending user messages to the backend and retrieving chat history.
 * 
 * Requirements:
 * - Axios must be installed (`npm install axios`).
 * - The backend API should be running and accessible at the specified `API_BASE_URL`.
 * - Ensure `sessionId` is correctly obtained before making API calls.
 * 
 * Functions:
 * - sendMessage(sessionId: string, message: string): Sends a message to the backend.
 * - getChatHistory(sessionId: string): Fetches chat history for the given session.
 * 
 * Error Handling:
 * - This file includes a centralized error-handling function (`handleApiError`) that differentiates 
 *   between Axios errors, standard JavaScript errors, and unknown errors.
 * - If an Axios request fails, it logs the error details and throws a descriptive error message.
 * - If an unexpected error occurs, it is caught and logged appropriately to aid debugging.
 */

import axios from "axios";
import { ChatMessage, ChatHistory } from "../../types/CBTSession/chat";

const API_BASE_URL = "https://api.yourapp.com/chat";

const handleApiError = (error: unknown, context: string) => {
    if (axios.isAxiosError(error)) {
        console.error(`Error in ${context}:`, error.response?.data || error.message);
        throw new Error(error.response?.data?.message || `An API error occurred in ${context}`);
    } else if (error instanceof Error) {
        console.error(`Unexpected error in ${context}:`, error.message);
        throw new Error(error.message);
    } else {
        console.error(`Unknown error in ${context}:`, error);
        throw new Error(`An unknown error occurred in ${context}`);
    }
};

export const sendMessage = async (sessionId: string, message: string): Promise<ChatMessage> => {
    if (!sessionId) {
        throw new Error("sessionId is required to send a message.");
    }
    try {
        const response = await axios.post(`${API_BASE_URL}/send-message`, { sessionId, message });
        return response.data;
    } catch (error: unknown) {
        return handleApiError(error, "sendMessage") as never;
    }
};

export const getChatHistory = async (sessionId: string): Promise<ChatHistory> => {
    if (!sessionId) {
        throw new Error("sessionId is required to fetch chat history.");
    }
    try {
        const response = await axios.get(`${API_BASE_URL}/history/${sessionId}`);
        return response.data;
    } catch (error: unknown) {
        return handleApiError(error, "getChatHistory") as never;
    }
};
