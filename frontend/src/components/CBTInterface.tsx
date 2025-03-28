import React, { useState, useEffect, useRef } from "react";
import therapyRoom from "../assets/therapy-room-1.svg";
import { FaMicrophone, FaMicrophoneSlash, FaVideo, FaVideoSlash } from "react-icons/fa";
import hark from "hark";
import { useWebSocket, WebSocketMessage } from "../hooks/useWebSocket";

interface ChatMessage {
  id: number;
  sender: string;  // "User" | "Assistant"
  message?: string;
  audioUrl?: string;
  timestamp: Date;
}

const CBTInterface: React.FC = () => {
  // --------------------- STATE ---------------------
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([
    {
      id: Date.now(),
      sender: "Assistant",
      message: "Hello, how can I help you today?",
      timestamp: new Date(),
    },
  ]);
  const [sessionActive, setSessionActive] = useState(false);
  const [micMuted, setMicMuted] = useState(false);
  const [cameraOn, setCameraOn] = useState(true);
  const [isSpeaking, setIsSpeaking] = useState(false);

  // --------------------- REFS ---------------------
  const combinedStreamRef = useRef<MediaStream | null>(null);
  const audioRecorderRef = useRef<MediaRecorder | null>(null);
  const videoRecorderRef = useRef<MediaRecorder | null>(null);
  const harkRef = useRef<any>(null);
  const chatContainerRef = useRef<HTMLDivElement>(null);
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const ttsAudioRef = useRef<HTMLAudioElement | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);

  // --------------------- USE WEBSOCKET HOOK ---------------------
  // The connection will be established when sessionActive becomes true.
  const { isConnected, messages, sendMessage } = useWebSocket("ws://localhost:8080/ws/cbt", sessionActive);

  // --------------------- EFFECT: PROCESS INCOMING MESSAGES ---------------------
  useEffect(() => {
    if (messages.length > 0) {
      const msg = messages[messages.length - 1];
      if (msg.type === "audio") {
        console.log("[useEffect] Processing audio message from server");
        // Stop any currently playing audio
        if (ttsAudioRef.current) {
          ttsAudioRef.current.pause();
          ttsAudioRef.current.src = "";
          ttsAudioRef.current = null;
        }
        // Transform the minimal WebSocketMessage into a richer ChatMessage
        const aiMsg: ChatMessage = {
          id: Date.now(),
          sender: "Assistant",   // Add the sender here
          message: msg.text,     // Use the text from the WebSocketMessage
          audioUrl: msg.audio,   // Use the audio URL from the WebSocketMessage
          timestamp: new Date(),
        };
        setChatMessages((prev) => [...prev, aiMsg]);
        // Play the audio if available
        if (msg.audio) {
          const audioElem = new Audio(msg.audio);
          ttsAudioRef.current = audioElem;
          audioElem.play().catch(err => {
            console.warn("[TTS] Couldn’t autoplay:", err);
          });
          audioElem.onended = () => {
            console.log("[TTS] Audio ended");
            ttsAudioRef.current = null;
          };
        }
      }
    }
  }, [messages]);

  // --------------------- SCROLL TO BOTTOM OF CHAT ---------------------
  useEffect(() => {
    if (chatContainerRef.current) {
      chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
    }
  }, [chatMessages]);

  // --------------------- START SESSION ---------------------
  const startSession = async () => {
    if (sessionActive) return;
    console.log("[startSession] Starting session...");
    setSessionActive(true);

    try {
      console.log("[startSession] Requesting microphone + camera...");
      const combinedStream = await navigator.mediaDevices.getUserMedia({
        audio: true,
        video: true,
      });
      combinedStreamRef.current = combinedStream;
      const audioTrack = combinedStream.getAudioTracks()[0];
      const videoTrack = combinedStream.getVideoTracks()[0];
      const audioOnlyStream = new MediaStream([audioTrack]);
      const videoOnlyStream = new MediaStream([videoTrack]);

      console.log("[startSession] Audio & video tracks acquired.");

      // Use Hark for speech detection
      const speechEvents = hark(audioOnlyStream, {
        interval: 50,
        threshold: -50,
      });
      speechEvents.on("speaking", () => {
        console.log("[Hark] speaking event => user is interrupting");
        setIsSpeaking(true);
        if (ttsAudioRef.current) {
          ttsAudioRef.current.pause();
        }
      });
      speechEvents.on("stopped_speaking", () => {
        console.log("[Hark] stopped_speaking event");
        setIsSpeaking(false);
        finalizeUserSpeech();
      });
      harkRef.current = speechEvents;

      // Audio recorder for sending audio chunks via WebSocket
      const audioRecorder = new MediaRecorder(audioOnlyStream);
      audioRecorderRef.current = audioRecorder;
      audioRecorder.ondataavailable = (e) => {
        console.log(`[audioRecorder] chunk size: ${e.data.size}, type: ${e.data.type}`);
        if (micMuted) {
          console.log("[audioRecorder] micMuted, skipping chunk.");
          return;
        }
        if (e.data.size > 0) {
          console.log("[audioRecorder] Buffering audio chunk...");
          // Store the chunk in the buffer for the current utterance.
          audioChunksRef.current.push(e.data);
        }
      };
      audioRecorder.start(500);

      // Video recorder for sending video chunks via WebSocket
      const videoRecorder = new MediaRecorder(videoOnlyStream);
      videoRecorderRef.current = videoRecorder;
      /* videoRecorder.ondataavailable = (e) => {
        console.log(`[videoRecorder] chunk size: ${e.data.size}, type: ${e.data.type}`);
        if (e.data.size > 0) {
          console.log("[videoRecorder] Sending video chunk to server...");
          sendMessage(e.data);
        }
      }; */
      videoRecorder.start(500);

      console.log("[startSession] Recorders started.");

      // Display user's camera in the avatar
      if (videoRef.current) {
        videoRef.current.srcObject = videoOnlyStream;
        videoRef.current.play().catch(err => {
          console.warn("[Video] Autoplay might be blocked or error:", err);
        });
      }
    } catch (err) {
      console.error("[startSession] Error accessing mic/camera:", err);
    }
  };

  // --------------------- FINALIZE USER SPEECH ---------------------
  const finalizeUserSpeech = () => {
    console.log("[finalizeUserSpeech] Finalizing user speech.");
  
    // Create and display the user chat bubble for this utterance.
    const userMsg: ChatMessage = {
      id: Date.now(),
      sender: "User",
      message: "User's final transcribed text (mock)",
      timestamp: new Date(),
    };
    setChatMessages((prev) => [...prev, userMsg]);
  
    // Combine the buffered chunks into a single Blob.
    // The MIME type should match the one produced by MediaRecorder.
    const completeAudioBlob = new Blob(audioChunksRef.current, { type: "audio/ogg; codecs=opus" });
  
    // Clear the buffer for the next utterance.
    audioChunksRef.current = [];
  
    // Send the complete audio file as one binary message.
    sendMessage(completeAudioBlob);
  
    // Optionally, you can also send an "end-of-speech" text message if your backend expects it:
    // sendMessage(JSON.stringify({ type: "end-of-speech" }));
  };

  // --------------------- STOP SESSION ---------------------
  const stopSession = () => {
    console.log("[stopSession] Ending session...");
    setSessionActive(false);
    if (ttsAudioRef.current) {
      ttsAudioRef.current.pause();
      ttsAudioRef.current.src = "";
      ttsAudioRef.current = null;
    }
    if (audioRecorderRef.current && audioRecorderRef.current.state !== "inactive") {
      console.log("[stopSession] Stopping audio recorder...");
      audioRecorderRef.current.stop();
      audioRecorderRef.current = null;
    }
    if (videoRecorderRef.current && videoRecorderRef.current.state !== "inactive") {
      console.log("[stopSession] Stopping video recorder...");
      videoRecorderRef.current.stop();
      videoRecorderRef.current = null;
    }
    if (videoRef.current) {
      console.log("[stopSession] Clearing video element...");
      videoRef.current.pause();
      videoRef.current.srcObject = null;
    }
    if (combinedStreamRef.current) {
      console.log("[stopSession] Stopping all tracks...");
      combinedStreamRef.current.getTracks().forEach((track) => track.stop());
      combinedStreamRef.current = null;
    }
    if (harkRef.current) {
      console.log("[stopSession] Stopping Hark...");
      harkRef.current.stop();
      harkRef.current = null;
    }
  };

  // --------------------- TOGGLE MIC ---------------------
  const toggleMic = () => {
    console.log("[toggleMic] Toggling mic");
    if (!audioRecorderRef.current) return;
    const track = combinedStreamRef.current?.getAudioTracks()[0];
    if (track) {
      track.enabled = !track.enabled;
      console.log("[toggleMic]", track.enabled ? "unmuted" : "muted");
    }
    setMicMuted(!micMuted);
  };

  // --------------------- TOGGLE CAMERA ---------------------
  const handleCamClick = () => {
    console.log("[handleCamClick] Toggling camera");
    if (!combinedStreamRef.current) return;
    const videoTrack = combinedStreamRef.current.getVideoTracks()[0];
    if (videoTrack) {
      videoTrack.enabled = !videoTrack.enabled;
      console.log("[handleCamClick]", videoTrack.enabled ? "Camera ON" : "Camera OFF");
    }
    setCameraOn(!cameraOn);
  };

  // --------------------- RENDER ---------------------
  return (
    <div
      className="hero h-screen relative bg-base-100"
      style={{ backgroundImage: `url(${therapyRoom})`, backgroundSize: "cover" }}
    >
      <div className="hero-overlay bg-neutral opacity-75"></div>
      <div className="absolute top-0 left-0 w-full h-full flex items-center justify-center">
        <div className="w-4/5">
          <div ref={chatContainerRef} className="chat-container max-h-[60vh] overflow-y-auto px-4">
            {chatMessages.map((msg) => (
              <div
                key={msg.id}
                className={`chat mb-4 ${msg.sender === "User" ? "chat-end" : "chat-start"}`}
              >
                <div className="chat-header flex justify-between items-center">
                  <span>{msg.sender}</span>
                  <time className="text-xs opacity-50">
                    {msg.timestamp.toLocaleTimeString()}
                  </time>
                </div>
                <div
                  className={`chat-bubble p-3 shadow-md rounded-full ${
                    msg.sender === "User"
                      ? "bg-base-300 bg-opacity-80 text-primary-content"
                      : "bg-base-300 bg-opacity-80 text-secondary-content"
                  }`}
                >
                  {msg.message}
                  {msg.audioUrl && (
                    <audio src={msg.audioUrl} autoPlay controls={false} />
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
      {/* BEGIN SESSION BUTTON */}
      {!sessionActive && (
        <div className="absolute inset-0 flex items-center justify-center">
          <button className="btn btn-primary" onClick={startSession}>
            Begin Session
          </button>
        </div>
      )}
      {/* STOP SESSION BUTTON */}
      {sessionActive && (
        <div className="absolute top-5 left-1/2 transform -translate-x-1/2">
          <button className="btn btn-error" onClick={stopSession}>
            End Session
          </button>
        </div>
      )}
      <div className="absolute bottom-5 left-1/2 transform -translate-x-1/2 flex items-center gap-4">
        {/* Mic Button */}
        <button
          onClick={toggleMic}
          className={`btn btn-circle btn-sm ${micMuted ? "bg-warning" : "bg-neutral"}`}
        >
          <span className="text-neutral-content">
            {micMuted ? <FaMicrophoneSlash /> : <FaMicrophone />}
          </span>
        </button>
        {/* Camera Button */}
        <button
          onClick={handleCamClick}
          className={`btn btn-circle btn-sm ${cameraOn ? "bg-neutral" : "bg-warning"}`}
        >
          <span className="text-neutral-content">
            {cameraOn ? <FaVideo /> : <FaVideoSlash />}
          </span>
        </button>
      </div>
      <div className="absolute bottom-5 right-5 avatar">
        <div className="w-38 h-38 rounded border-4 border-neutral overflow-hidden">
          <video ref={videoRef} autoPlay muted className="object-cover w-full h-full" />
        </div>
      </div>
    </div>
  );
};

export default CBTInterface;