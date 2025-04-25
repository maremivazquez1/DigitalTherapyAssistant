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


export interface WebSocketHeaderMessage {
    type: "header";              // This is always "header"
    session_id: string | null;   // The current session's identifier
    file_id: string;             // The unique identifier for this utterance (or media file)
    modality: "audio" | "video"; // Indicates whether this header is for audio or video
    timestamp_start: string;     // When the media recording started, in ISO format
    timestamp_end: string;       // When the media recording ended, in ISO format
    user_id: string;             // The identifier for the user sending the message
  }