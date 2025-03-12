/**
 * WebSocketMessage.ts
 * 
 * This file defines the TypeScript interface for WebSocket messages exchanged between
 * the frontend and backend during a CBT session.
 * 
 * Types:
 * - WebSocketMessageType: Represents different types of WebSocket messages.
 * - WebSocketMessage: Defines the structure of a message sent or received via WebSocket.
 */

export type WebSocketMessageType = "chat" | "system" | "ping" | "error";

export interface WebSocketMessage {
    type: WebSocketMessageType;
    sessionId: string;
    content?: string; // Optional for "ping" messages
    timestamp: string;
    error?: string; // Used for error messages
}
