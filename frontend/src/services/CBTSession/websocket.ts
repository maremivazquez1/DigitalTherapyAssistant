/**
 * websocket.ts
 * 
 * This file manages the WebSocket connection for real-time AI interactions in the CBT session.
 * It allows sending messages, receiving AI responses, and maintaining an active connection.
 * 
 * Features:
 * - Establishes a WebSocket connection to the backend.
 * - Handles incoming AI-generated responses.
 * - Provides functions to send messages and close the connection.
 * - Supports event-based callbacks for message handling.
 * 
 * Requirements:
 * - The WebSocket server should be running and accessible at `WS_BASE_URL`.
 * - The `sessionId` should be valid before initiating a connection.
 * 
 * Functions:
 * - connectWebSocket(sessionId: string, onMessage: (message: WebSocketMessage) => void): Connects to WebSocket and listens for messages.
 * - sendWebSocketMessage(message: string): Sends a user message through WebSocket.
 * - disconnectWebSocket(): Closes the WebSocket connection.
 */

import { WebSocketMessage } from "../../types/CBTSession/webSocketMessage";

let socket: WebSocket | null = null;
let reconnectAttempts = 0;
const maxReconnectAttempts = 5;
const reconnectDelay = 1000; // Initial delay in ms (1 second)
let heartbeatInterval: number | null = null; // Updated to use number | null

// update with the correct WebSocket base URL
const WS_BASE_URL = "ws://api.yourapp.com/chat/stream";

export const connectWebSocket = (sessionId: string, onMessage: (message: WebSocketMessage) => void) => {
    if (socket && socket.readyState === WebSocket.OPEN) {
        console.warn("WebSocket is already connected.");
        return;
    }

    socket = new WebSocket(`${WS_BASE_URL}?sessionId=${sessionId}`);

    socket.onopen = () => {
        console.log("WebSocket connected.");
        heartbeatInterval = window.setInterval(() => { // Modified to properly return a number
            if (socket?.readyState === WebSocket.OPEN) {
                socket.send(JSON.stringify({ type: "ping" }));
            }
        }, 30000); // Send heartbeat every 30s
    };

    socket.onmessage = (event) => {
        try {
            const data: WebSocketMessage = JSON.parse(event.data);
            console.log("WebSocket message received:", data);
            onMessage(data);
        } catch (error) {
            console.error("Error parsing WebSocket message:", error);
        }
    };

    socket.onerror = (error) => {
        console.error("WebSocket error:", error);
    };

    socket.onclose = () => {
        console.log("WebSocket disconnected.");
        if (heartbeatInterval !== null) { // Modified to properly clear the interval
            clearInterval(heartbeatInterval);
            heartbeatInterval = null;
        }
        socket = null;
        if (reconnectAttempts < maxReconnectAttempts) {
            setTimeout(() => connectWebSocket(sessionId, onMessage), reconnectDelay * (2 ** reconnectAttempts));
            reconnectAttempts++;
        }
    };
};

export const sendWebSocketMessage = (message: string) => {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
        console.error("WebSocket is not connected.");
        return;
    }
    socket.send(JSON.stringify({ message }));
};

export const disconnectWebSocket = () => {
    if (socket) {
        socket.onopen = null;
        socket.onmessage = null;
        socket.onerror = null;
        socket.onclose = null;
        socket.close();
        socket = null;
        reconnectAttempts = 0;
    }
};
