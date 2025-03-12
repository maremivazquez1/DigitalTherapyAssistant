/**
 * session.ts
 * 
 * This file defines TypeScript interfaces for session-related data in the CBT session.
 * It standardizes the structure of session objects and ensures type safety.
 * 
 * Types:
 * - SessionStatus: Represents possible session states.
 * - Session: Defines the structure of a CBT session.
 * - SessionHistory: Represents a list of past sessions.
 */

export type SessionStatus = "active" | "inactive" | "ended";

export interface Session {
    sessionId: string;
    userId: string;
    status: SessionStatus;
    createdAt: string;
    expiresAt?: string; // Optional for sessions that auto-expire
}

export interface SessionHistory {
    userId: string;
    sessions: Session[];
}
