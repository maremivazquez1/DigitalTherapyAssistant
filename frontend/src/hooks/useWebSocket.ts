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
    console.log("isConnected changed to:", isConnected);
  }, [isConnected]);

  useEffect(() => {
    console.log("[useWebSocket] Connecting to:", url);
    if (!connect) return;
    const socket = new WebSocket(url);
    socket.binaryType = "arraybuffer"; // if you need to send/receive binary data
    socketRef.current = socket;

    socket.onopen = () => {
      console.log(isConnected);
      console.log("[WebSocket] Connection established.");
      console.log(isConnected);
      setIsConnected(true);
      console.log(isConnected);
    };

    socket.onmessage = (event) => {
      // Check if the received data is a string or binary data
      if (typeof event.data === "string") {
        console.log("[useWebSocket] Received text message:", event.data);
        try {
          const msg: WebSocketMessage = JSON.parse(event.data);
          setMessages((prev) => [...prev, msg]);
        } catch (err) {
          console.error("[useWebSocket] Error parsing text message:", err);
        }
      } else if (event.data instanceof ArrayBuffer) {
        console.log("[useWebSocket] Received binary data from server.");
        // Convert the binary data (ArrayBuffer) into a Blob.
        // Use the correct MIME type—if your backend processes to MP3, use "audio/mpeg".
        const blob = new Blob([event.data], { type: "audio/mpeg" });
        // Create a URL for the Blob
        const audioUrl = URL.createObjectURL(blob);
        // Create a message object that includes both text and the audio URL.
        const msg: WebSocketMessage = {
          type: "audio",
          text: "Processed audio response", // You can adjust this text as needed
          audio: audioUrl,
        };
        setMessages((prev) => [...prev, msg]);
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
    console.log("connection", isConnected);
    if (socketRef.current && isConnected) {
      socketRef.current.send(data);
    } else {
      console.error("[useWebSocket] WebSocket is not connected.");
    }
  };

  const handleBinaryResponse = (arrayBuffer: ArrayBuffer) => {
    console.log("[handleBinaryResponse] Received binary response, byteLength:", arrayBuffer.byteLength);
  
    // Convert the ArrayBuffer to a Blob.
    // Adjust the MIME type if needed (here we assume audio/ogg with Opus codec).
    const blob = new Blob([arrayBuffer], { type: "audio/ogg; codecs=opus" });
    const url = URL.createObjectURL(blob);
    
    // Create an audio element and play it.
    const audioElem = new Audio(url);
    audioElem.play().catch(err => {
      console.error("[handleBinaryResponse] Playback error:", err);
    });
  };

  return { isConnected, messages, sendMessage };
};