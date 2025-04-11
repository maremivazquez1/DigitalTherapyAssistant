import React, { useState, useEffect, useRef } from "react";
import therapyRoom from "../assets/therapy-room-1.svg";
import { FaMicrophone, FaMicrophoneSlash, FaVideo, FaVideoSlash } from "react-icons/fa";
import hark from "hark";
import { useWebSocket } from "../hooks/useWebSocket";

interface ChatMessage {
  id: number;
  sender: string;  // "User" | "Assistant"
  message?: string;
  audioUrl?: string;
  timestamp: Date;
  pending?: boolean; // Optional flag to indicate a temporary message
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

  // --------------------- REFS ---------------------
  const combinedStreamRef = useRef<MediaStream | null>(null);
  const audioRecorderRef = useRef<MediaRecorder | null>(null);
  const videoRecorderRef = useRef<MediaRecorder | null>(null);
  const harkRef = useRef<any>(null);
  const chatContainerRef = useRef<HTMLDivElement | null>(null);
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const ttsAudioRef = useRef<HTMLAudioElement | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);

  // New refs to store metadata for each utterance
  const utteranceFileIdRef = useRef<string | null>(null);
  const utteranceStartTimeRef = useRef<string | null>(null);

  // New ref for the session ID
  const sessionIdRef = useRef<string | null>(null);

  // --------------------- USE WEBSOCKET HOOK ---------------------
  // The connection will be established when sessionActive becomes true.
  const { messages, sendMessage } = useWebSocket("ws://localhost:8080/ws/cbt", sessionActive);

  // --------------------- EFFECT: PROCESS INCOMING MESSAGES ---------------------
  useEffect(() => {
    if (messages.length === 0) return;

    const msg = messages[messages.length - 1];

    switch (msg.type) {
      /**
       * When the backend sends:
       *   {"type": "input-transcription", "text": "final transcript..."}
       * we update the pending bubble (if any) with the actual transcript.
       */
      case "input-transcription": {
        setChatMessages((prev) => {
          const pendingIndex = prev.findIndex(
            (m) => m.pending && m.sender === "User"
          );
          if (pendingIndex !== -1) {
            // Update existing pending bubble
            const updated = [...prev];
            updated[pendingIndex] = {
              ...updated[pendingIndex],
              message: msg.text,
              pending: false,
            };
            return updated;
          }
          // If no pending bubble exists, add a new one
          return [
            ...prev,
            {
              id: Date.now(),
              sender: "User",
              message: msg.text,
              timestamp: new Date(),
            },
          ];
        });
        break;
      }
      /**
       * When the backend sends:
       *   {"type": "output-transcription", "text": "assistant reply..."}
       * add that as a new Assistant chat bubble.
       */
      case "output-transcription": {
        const assistantMsg: ChatMessage = {
          id: Date.now(),
          sender: "Assistant",
          message: msg.text,
          timestamp: new Date(),
        };
        setChatMessages((prev) => [...prev, assistantMsg]);
        break;
      }
      /**
       * When the backend sends:
       *   {"type": "audio", "text": "processed audio response", "audio": "audioURL"}
       * then only handle playing the audio and do NOT add an extra bubble.
       */
      case "audio": {
        console.log("[useEffect] Processing audio message from server");
        // Stop any currently playing audio
        if (ttsAudioRef.current) {
          ttsAudioRef.current.pause();
          ttsAudioRef.current.src = "";
          ttsAudioRef.current = null;
        }
        // Play the TTS audio if available
        if (msg.audio) {
          const audioElem = new Audio(msg.audio);
          ttsAudioRef.current = audioElem;
          audioElem.play().catch((err) => {
            console.warn("[TTS] Could not autoplay:", err);
          });
          audioElem.onended = () => {
            console.log("[TTS] Audio ended");
            ttsAudioRef.current = null;
          };
        }
        break;
      }
      default:
        console.warn("[useEffect] Unrecognized message type:", msg.type);
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
      // Generate a new session ID when the session starts
      sessionIdRef.current = `session_${Date.now()}`;

      const combinedStream = await navigator.mediaDevices.getUserMedia({
        audio: true,
        video: true,
      });

      combinedStreamRef.current = combinedStream;
      const audioTrack = combinedStream.getAudioTracks()[0];
      const videoTrack = combinedStream.getVideoTracks()[0];
      const audioOnlyStream = new MediaStream([audioTrack]);
      const videoOnlyStream = new MediaStream([videoTrack]);

      // Setup MediaRecorder for audio
      const audioRecorder = new MediaRecorder(audioOnlyStream, {
        mimeType: "audio/webm;codecs=opus",
      });

      audioChunksRef.current = [];

      audioRecorder.ondataavailable = (e) => {
        if (e.data.size > 0) {
          audioChunksRef.current.push(e.data);
          console.log("[Recorder] Buffered chunk:", e.data.size);
        }
      };

      audioRecorder.onstop = () => {
        console.log("[Recorder] Stopped. Finalizing...");

        if (audioChunksRef.current.length === 0) {
          console.warn("No audio chunks — not sending.");
          return;
        }

        // Create a header object with metadata including session ID
        const headerMessage = {
          type: "header",
          session_id: sessionIdRef.current, // Added session ID here
          file_id: utteranceFileIdRef.current || `audio_${Date.now()}`,
          modality: "audio",
          timestamp_start: utteranceStartTimeRef.current || new Date().toISOString(),
          timestamp_end: new Date().toISOString(),
          user_id: "user_12345",
        };

        // Send the header before sending the actual blob
        sendMessage(JSON.stringify(headerMessage));
        console.log("[Recorder] Sent header:", headerMessage);

        // Combine all buffered chunks into a final Blob (complete utterance)
        const completeAudioBlob = new Blob(audioChunksRef.current, {
          type: "audio/webm;codecs=opus",
        });
        console.log("Final Blob size:", completeAudioBlob.size);

        // Send the complete audio blob to the backend
        sendMessage(completeAudioBlob);
        console.log("[Recorder] Sent complete audio blob.");

        // Clear buffered chunks and reset utterance metadata for the next utterance
        audioChunksRef.current = [];
        utteranceFileIdRef.current = null;
        utteranceStartTimeRef.current = null;
      };

      audioRecorderRef.current = audioRecorder;

      // Setup Hark for speech detection
      const harkInstance = hark(audioOnlyStream, {
        interval: 50,
        threshold: -50,
      });

      harkInstance.on("speaking", () => {
        console.log("[Hark] speaking");

        if (ttsAudioRef.current) {
          ttsAudioRef.current.pause();
        }

        // Only start a new recorder if one isn’t already running
        if (audioRecorderRef.current?.state !== "recording") {
          // Initialize metadata for this utterance
          utteranceFileIdRef.current = `audio_${Date.now()}`;
          utteranceStartTimeRef.current = new Date().toISOString();
          console.log("[Hark] New utterance started with file ID:", utteranceFileIdRef.current);

          audioChunksRef.current = [];
          audioRecorderRef.current?.start(500); // Collect every 500ms
        }
      });

      harkInstance.on("stopped_speaking", () => {
        console.log("[Hark] stopped_speaking");

        if (audioRecorderRef.current?.state === "recording") {
          console.log("[Hark] Stopping recorder...");
          audioRecorderRef.current.stop(); // onstop will handle sending metadata and blob
        }
      });

      harkRef.current = harkInstance;

      // Setup MediaRecorder for video (if needed)
      const videoRecorder = new MediaRecorder(videoOnlyStream);
      videoRecorderRef.current = videoRecorder;
      videoRecorder.start(500);

      if (videoRef.current) {
        videoRef.current.srcObject = videoOnlyStream;
        videoRef.current.play().catch((err) => {
          console.warn("[Video] Autoplay might be blocked:", err);
        });
      }

      console.log("[startSession] Recorders and Hark initialized.");
    } catch (err) {
      console.error("[startSession] Error accessing media devices:", err);
    }
  };

  // --------------------- STOP SESSION ---------------------
  const stopSession = () => {
    console.log("[stopSession] Ending session...");
    setSessionActive(false);

    if (ttsAudioRef.current) {
      console.log("[stopSession] Forcing complete audio shutdown...");
      try {
        ttsAudioRef.current.pause();
        if (ttsAudioRef.current.src?.startsWith("blob:")) {
          console.log("[stopSession] Revoking audio URL:", ttsAudioRef.current.src);
          URL.revokeObjectURL(ttsAudioRef.current.src);
        }
        ttsAudioRef.current.src = "";
        ttsAudioRef.current.load();
        ttsAudioRef.current.onended = null;
        ttsAudioRef.current.onerror = null;
        ttsAudioRef.current.onpause = null;
        ttsAudioRef.current = null;
      } catch (err) {
        console.warn("[stopSession] Error during audio shutdown:", err);
      }
      ttsAudioRef.current = new Audio();
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
                  className={`chat-bubble p-4 shadow-md rounded-full ${
                    msg.sender === "User"
                      ? "bg-base-300 bg-opacity-80 text-primary-content"
                      : "bg-base-300 bg-opacity-80 text-secondary-content"
                  }`}
                >
                  {msg.message}
                  {msg.audioUrl && <audio src={msg.audioUrl} controls={false} />}
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

      {/* MIC & CAMERA CONTROLS */}
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

      {/* SELF-VIEW VIDEO */}
      <div className="absolute bottom-5 right-5 avatar">
        <div className="w-38 h-38 rounded border-4 border-neutral overflow-hidden">
          <video ref={videoRef} autoPlay muted className="object-cover w-full h-full" />
        </div>
      </div>
    </div>
  );
};

export default CBTInterface;
