/**
 * sessionUtils.ts
 * 
 * This file provides helper functions for managing and processing CBT session data.
 * It includes utilities for checking session expiration, formatting session details,
 * and managing session data in local storage.
 * 
 * Functions:
 * - isSessionActive(session): Checks if a session is still active based on expiration time.
 * - formatSessionData(session): Formats session details for display in components.
 * - saveSessionToLocalStorage(session): Saves session data to local storage.
 * - loadSessionFromLocalStorage(): Retrieves session data from local storage.
 */

export const isSessionActive = (session: { expiresAt: string }): boolean => {
    if (!session || !session.expiresAt) return false;
    return new Date(session.expiresAt).getTime() > Date.now();
};

export const formatSessionData = (session: { sessionId: string; status: string }): string => {
    return `Session ID: ${session.sessionId} - Status: ${session.status.toUpperCase()}`;
};

export const saveSessionToLocalStorage = (session: { sessionId: string; userId: string; expiresAt: string }) => {
    localStorage.setItem("activeSession", JSON.stringify(session));
};

export const loadSessionFromLocalStorage = (): { sessionId: string; userId: string; expiresAt: string } | null => {
    const session = localStorage.getItem("activeSession");
    return session ? JSON.parse(session) : null;
};
