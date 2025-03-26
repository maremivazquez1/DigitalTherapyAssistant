import { Server } from "mock-socket";
import testWav from "../assets/harvard.wav";

const MOCK_AUDIO = testWav;

// Create a mock server at the same URL your hook connects to
const mockServer = new Server("ws://localhost:8080/ws/cbt");

mockServer.on("connection", (socket) => {
  console.log("[MockServer] Client connected.");

  // Listen for messages from the client
  socket.on("message", (data) => {
    // Log the data type and content
    if (typeof data === "string") {
      console.log("[MockServer] Received text data:", data);
      try {
        const parsed = JSON.parse(data);
        if (parsed.type === "end-of-speech") {
          // Simulate a response after a delay when end-of-speech is received
          setTimeout(() => {
            const response = {
              type: "audio",
              text: "This is a simulated server response",
              audio: MOCK_AUDIO,
            };
            console.log("[MockServer] Sending simulated audio response.");
            socket.send(JSON.stringify(response));
          }, 1500);
        }
      } catch (err) {
        console.error("[MockServer] Error parsing text message:", err);
      }
    } else if (data instanceof ArrayBuffer) {
      console.log("[MockServer] Received ArrayBuffer, byteLength:", data.byteLength);
    } else if (data instanceof Blob) {
      console.log("[MockServer] Received Blob, size:", data.size);
    } else {
      console.log("[MockServer] Received data of unknown type:", data);
    }
  });

});

export default mockServer;
