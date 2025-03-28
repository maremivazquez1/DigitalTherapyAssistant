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

  // Log any changes to connection status
  useEffect(() => {
    console.log("isConnected changed to:", isConnected);
  }, [isConnected]);

  useEffect(() => {
    console.log("[useWebSocket] Connecting to:", url);
    if (!connect) return;
    const socket = new WebSocket(url);
    // Set binaryType to arraybuffer so binary messages are received as ArrayBuffer
    socket.binaryType = "arraybuffer";
    socketRef.current = socket;

    socket.onopen = () => {
      console.log("[WebSocket] Connection established.");
      setIsConnected(true);
    };

    socket.onmessage = (event) => {
      console.log("[useWebSocket] Raw message received:", event.data);

      // Process text messages
      if (typeof event.data === "string") {
        try {
          const parsed: WebSocketMessage = JSON.parse(event.data);
          console.log("[useWebSocket] Parsed text message:", parsed);
          setMessages((prev) => [...prev, parsed]);
        } catch (err) {
          console.error("[useWebSocket] Error parsing text message:", err);
        }
      }

      // Process binary messages
      if (event.data instanceof ArrayBuffer) {
        console.log("[useWebSocket] Received binary data from server.");
        // Convert the ArrayBuffer into a Blob using the MIME type your backend uses.
        // Here we assume the backend converts to MP3, so we use "audio/mpeg".
        const blob = new Blob([event.data], { type: "audio/mpeg" });
        const audioUrl = URL.createObjectURL(blob);
        console.log("[useWebSocket] Created audio URL:", audioUrl);
        // Create a message with both text and audio.
        const audioMsg: WebSocketMessage = {
          type: "audio",
          text: "Processed audio response", // Default text; you may adjust as needed
          audio: audioUrl,
        };
        setMessages((prev) => [...prev, audioMsg]);
      }
    };

    socket.onerror = (err) => {
      console.error("[WebSocket] Error:", err);
    };

    socket.onclose = (event) => {
      console.log("[useWebSocket] Connection closed. Code:", event.code, "Reason:", event.reason);
      setIsConnected(false);
    };

    return () => {
      socket.close();
    };
  }, [connect, url]);

  const sendMessage = (data: any) => {
    console.log("[useWebSocket] sendMessage called with data:", data);
    // Directly check the socket's readyState instead of isConnected
    if (socketRef.current && socketRef.current.readyState === WebSocket.OPEN) {
      socketRef.current.send(data);
    } else {
      console.error("[useWebSocket] WebSocket is not connected.");
    }
  };

  return { isConnected, messages, sendMessage };
};