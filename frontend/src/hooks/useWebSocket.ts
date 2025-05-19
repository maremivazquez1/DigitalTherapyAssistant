import { useState, useEffect, useRef } from "react";

export interface WebSocketMessage {
  type: string;
  text?: string;
  audio?: string;
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
    if (!connect) return;

    // Retrieve token from local storage and append it as a query parameter
    const token = localStorage.getItem("token");
    // If token exists, append it to the URL; otherwise, use the original URL.
    const wsUrl = token ? `${url}?token=${encodeURIComponent(token)}` : url;
    console.log("[useWebSocket] Connecting using URL:", wsUrl);

    const socket = new WebSocket(wsUrl);
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
          const parsed = JSON.parse(event.data);
          console.log("[useWebSocket] Parsed text message:", parsed);

          // AWS error
          if (parsed.error && parsed.code === 500) {
            console.warn("[useWebSocket] AWS Error detected:", parsed.error);
            setMessages((prev) => [
              ...prev,
              {
                type: "audio",
                text: 
                  "Sorry, something went wrong while processing your audio. Please try again later.",
                audio: undefined,
              },
            ]);
            return;
          }
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
        const audioMsg: WebSocketMessage = {
          type: "audio",
          text: "Processed audio response",
          audio: audioUrl,
        };
        setMessages((prev) => [...prev, audioMsg]);
      }
    };

    socket.onerror = (err) => {
      console.error("[WebSocket] Error:", err);
      setMessages((prev) => [
        ...prev,
        {
          type: "system",
          text: "Connection error occurred. Please check your connection.",
        },
      ]);
    };

    socket.onclose = (event) => {
      console.log("[WebSocket] Connection closed. Code:", event.code, "Reason:", event.reason);
      setIsConnected(false);
      setMessages((prev) => [
        ...prev,
        {
          type: "system",
          text: "Connection closed. Please try reconnecting.",
        },
      ]);
    };

    return () => {
      socket.close();
    };
  }, [connect, url]);

  const sendMessage = (data: any) => {
    console.log("[useWebSocket] sendMessage called with data:", data);
    if (socketRef.current && socketRef.current.readyState === WebSocket.OPEN) {
      socketRef.current.send(data);
    } else {
      console.error("[useWebSocket] WebSocket is not connected.");
    }
  };

  return { isConnected, messages, sendMessage };
};
