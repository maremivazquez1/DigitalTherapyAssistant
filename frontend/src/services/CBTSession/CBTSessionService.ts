/**
 * CBTSessionService.ts
 * 
 * This service provides a centralized interface for managing CBT sessions.
 * It abstracts API calls related to session management, chat, WebSocket communication, 
 * speech processing, and user preferences.
 * 
 * Features:
 * - Starts, retrieves, and ends CBT sessions.
 * - Sends user messages and retrieves AI-generated responses.
 * - Manages WebSocket connections for real-time chat.
 * - Processes Speech-to-Text (STT) and Text-to-Speech (TTS) operations.
 * - Saves and loads user preferences.
 * 
 * Improvements:
 * - Ensures WebSocket disconnects if session creation fails.
 * - Improves error messages for debugging.
 * - Adds TypeScript return types for better type safety.
 * - Handles missing or invalid session IDs before making API calls.
 */

// API imports
import { startSession, getSession, endSession } from "./sessionAPI";
import { sendMessage } from "./chatAPI";
import { connectWebSocket, disconnectWebSocket } from "./websocket";
import { processSpeech, convertTextToSpeech } from "./speechAPI";
import { savePreferences, loadPreferences } from "./preferencesAPI";

// Type imports
import { Session } from "../../types/CBTSession/session";
import { ChatMessage } from "../../types/CBTSession/chat";
import { UserPreferences } from "../../types/CBTSession/preferences";

// External library import
import axios from "axios";

const handleApiError = (error: unknown, context: string) => {
    if (axios.isAxiosError(error)) {
        console.error(`API Error in ${context}:`, error.response?.data || error.message);
        throw new Error(error.response?.data?.message || `An API error occurred in ${context}`);
    } else if (error instanceof Error) {
        console.error(`Unexpected Error in ${context}:`, error.message);
        throw new Error(error.message);
    } else {
        console.error(`Unknown Error in ${context}:`, error);
        throw new Error(`An unknown error occurred in ${context}`);
    }
};

const CBTSessionService = {
    /**
     * Starts a new CBT session for the given user.
     * Connects to WebSocket for real-time chat updates.
     */
    async startNewSession(userId: string): Promise<Session> {
        try {
            const session = await startSession(userId);
            connectWebSocket(session.sessionId, (message) => {
                console.log("Received WebSocket message:", message);
            });
            return session;
        } catch (error: unknown) {
            handleApiError(error, "startNewSession");
            return Promise.reject(); // Ensure return value in catch block
        }
    },

    /**
     * Retrieves an existing CBT session by session ID.
     * Ensures session ID is valid before making the request.
     */
    async fetchSession(sessionId: string): Promise<Session> {
        if (!sessionId) {
            console.error("fetchSession failed: sessionId is required.");
            throw new Error("Session ID is required.");
        }
        try {
            return await getSession(sessionId);
        } catch (error: unknown) {
            handleApiError(error, "fetchSession");
            return Promise.reject(); // Ensure return value in catch block
        }
    },

    /**
     * Ends the current CBT session and disconnects WebSocket.
     * Ensures session ID is valid before making the request.
     */
    async endCurrentSession(sessionId: string): Promise<{ success: boolean; message: string }> {
        if (!sessionId) {
            console.error("endCurrentSession failed: sessionId is required.");
            throw new Error("Session ID is required.");
        }
        try {
            disconnectWebSocket();
            return await endSession(sessionId);
        } catch (error: unknown) {
            handleApiError(error, "endCurrentSession");
            return Promise.reject(); // Ensure return value in catch block
        }
    },

    /**
     * Sends a message in an active CBT session.
     * Ensures session ID is valid before making the request.
     */
    async sendUserMessage(sessionId: string, message: string): Promise<ChatMessage> {
        if (!sessionId) {
            console.error("sendUserMessage failed: sessionId is required.");
            throw new Error("Session ID is required.");
        }
        try {
            return await sendMessage(sessionId, message);
        } catch (error: unknown) {
            handleApiError(error, "sendUserMessage");
            return Promise.reject(); // Ensure return value in catch block
        }
    },

    /**
     * Processes speech-to-text (STT) for user audio input.
     * Sends recorded audio to the backend for transcription.
     */
    async processUserSpeech(audioBlob: Blob): Promise<string> {
        try {
            return await processSpeech(audioBlob);
        } catch (error: unknown) {
            handleApiError(error, "processUserSpeech");
            return Promise.reject(); // Ensure return value in catch block
        }
    },

    /**
     * Converts AI-generated text into speech (TTS).
     * Sends text to the backend to generate an audio response.
     */
    async getTextToSpeechResponse(text: string): Promise<string> {
        try {
            return await convertTextToSpeech(text);
        } catch (error: unknown) {
            handleApiError(error, "getTextToSpeechResponse");
            return Promise.reject(); // Ensure return value in catch block
        }
    },

    /**
     * Saves user preferences for CBT session settings.
     * Stores settings like language, voice mode, and response speed.
     */
    async saveUserPreferences(preferences: UserPreferences): Promise<UserPreferences> {
        try {
            return await savePreferences(preferences);
        } catch (error: unknown) {
            handleApiError(error, "saveUserPreferences");
            return Promise.reject(); // Ensure return value in catch block
        }
    },

    /**
     * Loads user preferences for CBT session settings.
     * Retrieves stored settings like language, voice mode, and response speed.
     */
    async loadUserPreferences(): Promise<UserPreferences> {
        try {
            return await loadPreferences();
        } catch (error: unknown) {
            handleApiError(error, "loadUserPreferences");
            return Promise.reject(); // Ensure return value in catch block
        }
    }
};

export default CBTSessionService;