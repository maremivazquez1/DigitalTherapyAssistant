import React, { useState, useEffect, useRef } from "react";
import therapyRoom from "../assets/therapy-room-1.svg";
import { FaMicrophone, FaMicrophoneSlash, FaVideo, FaVideoSlash } from "react-icons/fa";
import hark from "hark";
import ReactMarkdown from 'react-markdown';
import { useWebSocket } from "../hooks/useWebSocket";
import { WebSocketHeaderMessage } from "../types/CBTSession/webSocketMessage";

interface ChatMessage {
  id: number;
  sender: string; // "User" | "Assistant"
  message?: string;
  audioUrl?: string;
  timestamp: Date;
  pending?: boolean;
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
  // Track if the assistant is still generating a reply.
  const [isAwaitingResponse, setIsAwaitingResponse] = useState(false);

  // --------------------- REFS ---------------------
  const combinedStreamRef = useRef<MediaStream | null>(null);
  const audioRecorderRef = useRef<MediaRecorder | null>(null);
  const videoRecorderRef = useRef<MediaRecorder | null>(null);
  const harkRef = useRef<any>(null);
  const chatContainerRef = useRef<HTMLDivElement | null>(null);
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const ttsAudioRef = useRef<HTMLAudioElement | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  const videoChunksRef = useRef<Blob[]>([]);
  const utteranceFileIdRef = useRef<string | null>(null);
  const utteranceStartTimeRef = useRef<string | null>(null);
  const sessionIdRef = useRef<string | null>(null);
  // Tracks the last processed message index.
  const lastProcessedIndex = useRef(0);
  // A counter for the user messages (or "turns")—optional for logging.
  const userMessageCountRef = useRef<number>(0);
  // A ref to track the awaiting response flag synchronously.
  const isAwaitingResponseRef = useRef(false);

  // Synchronize the ref with state whenever isAwaitingResponse changes.
  useEffect(() => {
    isAwaitingResponseRef.current = isAwaitingResponse;
  }, [isAwaitingResponse]);

  // --------------------- WEBSOCKET ---------------------
  const { messages, sendMessage } = useWebSocket("ws://localhost:8080/ws/cbt", sessionActive);

  // --------------------- PROCESS INCOMING MESSAGES ---------------------
  useEffect(() => {
    // Process all new messages that haven't been handled yet.
    for (let i = lastProcessedIndex.current; i < messages.length; i++) {
      const msg = messages[i];
      switch (msg.type) {
        case "input-transcription": {
          setChatMessages((prev) => {
            const index = prev.findIndex(
              (m) => m.pending && m.sender === "User"
            );
            if (index !== -1) {
              const newMessages = [...prev];
              newMessages[index] = {
                ...newMessages[index],
                message: msg.text,
                pending: false,
              };
              return newMessages;
            }
            // Fallback: if no pending message exists, add a new one.
            return [
              ...prev,
              { id: Date.now(), sender: "User", message: msg.text, timestamp: new Date() },
            ];
          });
          break;
        }
        case "output-transcription": {
          // Final assistant text response.
          const assistantMsg: ChatMessage = {
            id: Date.now(),
            sender: "Assistant",
            message: msg.text,
            timestamp: new Date(),
          };
          // Replace any pending assistant messages with the final one.
          setChatMessages((prev) => [
            ...prev.filter((m) => m.sender !== "Assistant" || !m.pending),
            assistantMsg,
          ]);
          // Clear the awaiting flag so new input is allowed.
          setIsAwaitingResponse(false);
          break;
        }
        case "audio": {
          console.log("[useEffect] Processing audio message from server");
          if (ttsAudioRef.current) {
            ttsAudioRef.current.pause();
            ttsAudioRef.current.src = "";
            ttsAudioRef.current = null;
          }
          if (msg.audio) {
            const audioElem = new Audio(msg.audio);
            ttsAudioRef.current = audioElem;
            audioElem
              .play()
              .catch((err) => {
                console.warn("[TTS] Could not autoplay:", err);
              });
            audioElem.onended = () => {
              console.log("[TTS] Audio ended");
              ttsAudioRef.current = null;
            };
          }
          // When the audio response completes, allow new input.
          setIsAwaitingResponse(false);
          break;
        }
        default:
          console.warn("[useEffect] Unrecognized message type:", msg.type);
      }
    }
    // Update the last processed index.
    lastProcessedIndex.current = messages.length;
  }, [messages]);

  // --------------------- SCROLL CHAT ---------------------
  useEffect(() => {
    if (chatContainerRef.current) {
      setTimeout(() => {
        if (chatContainerRef.current) {
          chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
        }
      }, 0);
    }
  }, [chatMessages]);

  // --------------------- START SESSION ---------------------
  const startSession = async () => {
    if (sessionActive) return;
    console.log("[startSession] Starting session...");
    setSessionActive(true);
    try {
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

      // Setup Audio Recorder
      const audioRecorder = new MediaRecorder(audioOnlyStream);
      audioChunksRef.current = [];
      audioRecorder.ondataavailable = (e) => {
        if (e.data.size > 0) {
          audioChunksRef.current.push(e.data);
          console.log("[Audio Recorder] Buffered chunk:", e.data.size);
        }
      };
      audioRecorder.onstop = () => {
        console.log("[Audio Recorder] Stopped. Finalizing audio...");

        if (audioChunksRef.current.length === 0) {
          console.warn("No audio chunks — not sending.");
          return;
        }
        // Block new input if already awaiting response.
        if (isAwaitingResponseRef.current) {
          console.warn("Assistant response is pending; new audio will not be sent.");
          return;
        }
        // Increment the user message count (for logging or future use).
        const newTurn = userMessageCountRef.current + 1;
        userMessageCountRef.current = newTurn;

        // Set the flag indicating we're awaiting the assistant's response.
        setIsAwaitingResponse(true);
        isAwaitingResponseRef.current = true;

        // Send a header.
        const headerMessageAudio: WebSocketHeaderMessage = {
          type: "header",
          session_id: localStorage.getItem('sessionId') || sessionIdRef.current,
          file_id: utteranceFileIdRef.current || `audio_${Date.now()}`,
          modality: "audio",
          timestamp_start: utteranceStartTimeRef.current || new Date().toISOString(),
          timestamp_end: new Date().toISOString(),
          user_id: localStorage.getItem('userId') || "user_12345"
        };
        sendMessage(JSON.stringify(headerMessageAudio));
        console.log("[Audio Recorder] Sent header:", headerMessageAudio);

        // Add a pending user message.
        const pendingUserMsg: ChatMessage = {
          id: Date.now(),
          sender: "User",
          message: "Transcribing your message...",
          pending: true,
          timestamp: new Date(),
        };
        setChatMessages((prev) => [...prev, pendingUserMsg]);

        // Create the audio blob and send it.
        const completeAudioBlob = new Blob(audioChunksRef.current, {
          type: "audio/webm;codecs=opus",
        });
        console.log("Final audio Blob size:", completeAudioBlob.size);
        sendMessage(completeAudioBlob);
        console.log("[Audio Recorder] Sent complete audio blob.");

        // Clear the audio chunks for the next utterance.
        audioChunksRef.current = [];
        utteranceFileIdRef.current = null;
        utteranceStartTimeRef.current = null;
      };
      audioRecorderRef.current = audioRecorder;

      // Setup Hark for speech detection
      const harkInstance = hark(audioOnlyStream, {
        interval: 30,    // check volume every x ms
        threshold: -45,   // dB threshold for “speech” vs “silence”
        history: 150       // silent intervals before “stopped_speaking”
      });
      harkInstance.on("speaking", () => {
        // Use the ref to block new recording if awaiting a response.
        if (isAwaitingResponseRef.current) {
          console.warn("Already awaiting response; ignoring new user speech.");
          return;
        }
        console.log("[Hark] speaking");
        if (ttsAudioRef.current) {
          ttsAudioRef.current.pause();
        }
        if (audioRecorderRef.current?.state !== "recording") {
          utteranceFileIdRef.current = `utterance_${Date.now()}`;
          utteranceStartTimeRef.current = new Date().toISOString();
          console.log("[Hark] New utterance started with file ID:", utteranceFileIdRef.current);
          // Start recording since no response is pending.
          audioChunksRef.current = [];
          audioRecorderRef.current?.start(500);
        }
        // Start the video recorder if not already started.
        if (!videoRecorderRef.current) {
          let options: { mimeType?: string } = {};
          if (MediaRecorder.isTypeSupported("video/webm;codecs=vp8")) {
            options.mimeType = "video/webm;codecs=vp8";
          }
          const videoRecorder = new MediaRecorder(videoOnlyStream, options);
          videoChunksRef.current = [];
          videoRecorder.ondataavailable = (e) => {
            if (e.data.size > 0) {
              videoChunksRef.current.push(e.data);
              console.log("[Video Recorder] Buffered chunk:", e.data.size);
            }
          };
          videoRecorder.onstop = () => {
            console.log("[Video Recorder] Stopped. Finalizing video...");
            const headerMessageVideo: WebSocketHeaderMessage = {
              type: "header",
              session_id: localStorage.getItem('sessionId') || sessionIdRef.current,
              file_id: utteranceFileIdRef.current || `video_${Date.now()}`,
              modality: "video",
              timestamp_start: utteranceStartTimeRef.current || new Date().toISOString(),
              timestamp_end: new Date().toISOString(),
              user_id: localStorage.getItem('userId') || "user_12345"
            };
            sendMessage(JSON.stringify(headerMessageVideo));
            console.log("[Video Recorder] Sent header:", headerMessageVideo);
            const completeVideoBlob = new Blob(videoChunksRef.current, { type: "video/webm" });
            console.log("Final video Blob size:", completeVideoBlob.size);
            sendMessage(completeVideoBlob);
            console.log("[Video Recorder] Sent complete video blob.");
            videoChunksRef.current = [];
            videoRecorderRef.current = null;
          };
          videoRecorderRef.current = videoRecorder;
          videoRecorder.start(500);
          console.log("[Video Recorder] Started video recorder for current utterance.");
        }
      });
      harkInstance.on("stopped_speaking", () => {
        console.log("[Hark] stopped_speaking");
        if (audioRecorderRef.current?.state === "recording") {
          console.log("[Hark] Stopping audio recorder...");
          audioRecorderRef.current.stop();
        }
        if (videoRecorderRef.current && videoRecorderRef.current.state === "recording") {
          console.log("[Hark] Stopping video recorder...");
          videoRecorderRef.current.stop();
        }
      });
      harkRef.current = harkInstance;
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
  
    // 1. Prevent further Hark events
    if (harkRef.current) {
      console.log("[stopSession] Removing Hark listeners and stopping...");
      // remove any 'speaking' / 'stopped_speaking' handlers
      harkRef.current.removeAllListeners?.();
      harkRef.current.stop();
      harkRef.current = null;
    }
  
    // 2. Stop running MediaRecorders
    if (audioRecorderRef.current) {
      if (audioRecorderRef.current.state === "recording") {
        console.log("[stopSession] Stopping audio recorder...");
        audioRecorderRef.current.stop();
      }
      audioRecorderRef.current = null;
    }
    if (videoRecorderRef.current) {
      if (videoRecorderRef.current.state === "recording") {
        console.log("[stopSession] Stopping video recorder...");
        videoRecorderRef.current.stop();
      }
      videoRecorderRef.current = null;
    }
  
    // 3. Stop all media tracks (mic + camera)
    if (combinedStreamRef.current) {
      console.log("[stopSession] Stopping all media tracks...");
      combinedStreamRef.current.getTracks().forEach(track => track.stop());
      combinedStreamRef.current = null;
    }
  
    // 4. Pause & clear the video element
    if (videoRef.current) {
      console.log("[stopSession] Clearing video element...");
      videoRef.current.pause();
      videoRef.current.srcObject = null;
    }
  
    // 5. Tear down any playing TTS audio
    if (ttsAudioRef.current) {
      console.log("[stopSession] Cleaning up TTS audio...");
      try {
        ttsAudioRef.current.pause();
        if (ttsAudioRef.current.src?.startsWith("blob:")) {
          URL.revokeObjectURL(ttsAudioRef.current.src);
        }
        ttsAudioRef.current.src = "";
        ttsAudioRef.current.load();
        ttsAudioRef.current.onended = null;
        ttsAudioRef.current.onerror = null;
        ttsAudioRef.current.onpause = null;
      } catch (err) {
        console.warn("[stopSession] Error during TTS cleanup:", err);
      }
      ttsAudioRef.current = new Audio();
    }
  
    // 6. Update UI state
    setSessionActive(false);
    setMicMuted(false);
    setCameraOn(false);
  };

  // --------------------- TOGGLE MIC ---------------------
  const toggleMic = () => {
    const track = combinedStreamRef.current?.getAudioTracks()[0];
    if (!track) return;
  
    // 1. flip our “muted” state
    const newMuted = !micMuted;
  
    // 2. apply it to the MediaStreamTrack
    track.enabled = !newMuted;
  
    // 3. store it
    setMicMuted(newMuted);
  
    console.log("[toggleMic]", newMuted ? "muted" : "unmuted");
  };

  // --------------------- TOGGLE CAMERA ---------------------
  const handleCamClick = () => {
    const track = combinedStreamRef.current?.getVideoTracks()[0];
    if (!track) return;
  
    const newCameraOn = !cameraOn;
    track.enabled = newCameraOn;
    setCameraOn(newCameraOn);
  
    console.log("[handleCamClick]", newCameraOn ? "Camera ON" : "Camera OFF");
  };

  // --------------------- RENDER ---------------------
  return (
    <div
      data-theme="calming"
      className="hero h-screen relative bg-base-100"
      style={{ backgroundImage: `url(${therapyRoom})`, backgroundSize: "cover" }}
    >
      <div className="hero-overlay bg-neutral opacity-75"></div>
      <div className="absolute top-0 left-0 w-full h-full flex items-center justify-center">
        <div className="w-4/5">
          <div ref={chatContainerRef} className="chat-container max-h-[60vh] overflow-y-auto px-4 pr-12">
            {chatMessages.map((msg) => (
              <div
                key={msg.id}
                className={`chat mb-4 ${msg.sender === "User" ? "chat-end" : "chat-start"}`}
              >
                <div className="chat-header flex justify-between items-center">
                  <span>{msg.sender}</span>
                  <time className="text-xs opacity-50">{msg.timestamp.toLocaleTimeString()}</time>
                </div>
                <div
                  className={`chat-bubble p-4 shadow-md rounded-lg ${
                    msg.sender === "User"
                      ? "bg-base-300 bg-opacity-80 text-primary-content"
                      : "bg-base-300 bg-opacity-80 text-secondary-content"
                  }`}
                >
                  <ReactMarkdown>{msg.message}</ReactMarkdown>
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
        <button
          data-testid="mic-toggle"
          onClick={toggleMic}
          className={`btn btn-circle btn-sm ${micMuted ? "bg-warning" : "bg-neutral"}`}
        >
          <span className="text-neutral-content">
            {micMuted ? <FaMicrophoneSlash /> : <FaMicrophone />}
          </span>
        </button>
        <button
          data-testid="cam-toggle"
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
