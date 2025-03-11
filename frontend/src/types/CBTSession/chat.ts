/**
 * chat.ts
 * 
 * This file defines TypeScript interfaces for chat-related data in the CBT session.
 * It standardizes the structure of chat messages exchanged between the user and AI.
 * 
 * Types:
 * - ChatMessage: Represents a single chat message (user or AI).
 * - ChatHistory: Represents a list of chat messages in a session.
 */

export type ChatRole = "user" | "ai";

export interface ChatMessage {
    id: string;
    sessionId: string;
    role: ChatRole;
    content: string;
    timestamp: string;
    audioUrl?: string; // Optional: Used for TTS responses
}

export interface ChatHistory {
    sessionId: string;
    messages: ChatMessage[];
}
