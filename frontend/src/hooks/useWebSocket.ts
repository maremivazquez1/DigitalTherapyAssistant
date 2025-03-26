import { useState, useEffect, useRef } from "react";

export interface WebSocketMessage {
  type: string;
  text?: string;
  audio?: string;
  // add additional fields as needed
}

export const useWebSocket = (url: string, connect: boolean) => {
  const [isConnected, setIsConnected] = useState(false);
  const [messages, setMessages] = useState<WebSocketMessage[]>([]);
  const socketRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    console.log("[useWebSocket] Connecting to:", url);
    if (!connect) return;
    const socket = new WebSocket(url);
    socket.binaryType = "arraybuffer"; // if you need to send/receive binary data
    socketRef.current = socket;

    socket.onopen = () => {
      console.log("[WebSocket] Connection established.");
      setIsConnected(true);
    };

    socket.onmessage = (event) => {
      console.log("[WebSocket] Message received:", event.data);
      try {
        const msg: WebSocketMessage = JSON.parse(event.data);
        setMessages((prev) => [...prev, msg]);
      } catch (err) {
        console.error("[WebSocket] Error parsing message:", err);
      }
    };

    socket.onerror = (err) => {
      console.error("[WebSocket] Error:", err);
    };

    socket.onclose = () => {
      console.log("[WebSocket] Connection closed.");
      setIsConnected(false);
    };

    return () => {
      socket.close();
    };
  }, [connect, url]);

  const sendMessage = (data: any) => {
    if (socketRef.current && isConnected) {
      socketRef.current.send(data);
    } else {
      console.error("WebSocket is not connected.");
    }
  };

  return { isConnected, messages, sendMessage };
};