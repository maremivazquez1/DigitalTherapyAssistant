import React, { useState, useEffect, useRef } from "react";
import therapyRoom from "../assets/therapy-room-1.svg";
import { FaMicrophone, FaVideo } from "react-icons/fa";
import hark from "hark";
import testWav from '../assets/harvard.wav';

interface ChatMessage {
  id: number;
  sender: string;  // "User" | "AI"
  message?: string;
  audioUrl?: string;
  timestamp: Date;
}

const MOCK_AUDIO = testWav;

class FakeWebSocket {
  onmessage: ((ev: MessageEvent) => any) | null = null;
  onopen: (() => any) | null = null;
  onclose: (() => any) | null = null;

  open() {
    console.log("[FakeWebSocket] open() called, simulating server handshake...");
    setTimeout(() => {
      if (this.onopen) {
        console.log("[FakeWebSocket] onopen triggered");
        this.onopen();
      }
    }, 500);
  }

  send(data: any) {
    console.log("[FakeWebSocket] send() - Received audio data from client:", data);
  }

  serverRespondWithAudio() {
    console.log("[FakeWebSocket] serverRespondWithAudio() - Will send audio to client in 1.5s");
    setTimeout(() => {
      if (this.onmessage) {
        console.log("[FakeWebSocket] Sending audio message to client now...");
        const fakeAIResponse = {
          data: JSON.stringify({
            type: "audio",
            text: "Here's an audio response from the AI",
            audio: MOCK_AUDIO,
          }),
        };
        this.onmessage(fakeAIResponse as MessageEvent);
      }
    }, 1500);
  }

  close() {
    console.log("[FakeWebSocket] close() called, simulating server close.");
    if (this.onclose) this.onclose();
  }
}

const CBTInterface: React.FC = () => {
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

  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const micStreamRef = useRef<MediaStream | null>(null);
  const socketRef = useRef<FakeWebSocket | null>(null);

  const harkRef = useRef<any>(null);
  const ttsAudioRef = useRef<HTMLAudioElement | null>(null);

  // Track if user is speaking (for UI or logs)
  const [isSpeaking, setIsSpeaking] = useState(false);

  const chatContainerRef = useRef<HTMLDivElement>(null);

  // Scroll to bottom on new messages
  useEffect(() => {
    if (chatContainerRef.current) {
      chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
    }
  }, [chatMessages]);

  // Handle server messages
  useEffect(() => {
    const handleServerMessage = (event: MessageEvent) => {
      console.log("[handleServerMessage] Received message from server:", event.data);
      try {
        const msg = JSON.parse(event.data);
        if (msg.type === "audio") {
            console.log("[handleServerMessage] Audio from server");
          
            // 1) Stop old audio if it's still playing
            if (ttsAudioRef.current) {
              ttsAudioRef.current.pause();
              ttsAudioRef.current.src = "";   // or set currentTime = 0
              ttsAudioRef.current = null;
            }
          
            // 2) Insert the new chat bubble (like you already do)
            const aiMsg: ChatMessage = {
              id: Date.now(),
              sender: "Assistant",
              message: msg.text,
              audioUrl: msg.audio, // e.g. the mp3 url or base64
              timestamp: new Date(),
            };
            setChatMessages((prev) => [...prev, aiMsg]);
          
            // 3) Create & play the new audio
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
      } catch (err) {
        console.error("[handleServerMessage] Error parsing server msg:", err);
      }
    };
  
    if (socketRef.current) {
      console.log("[useEffect] Attaching onmessage to socketRef.current");
      socketRef.current.onmessage = handleServerMessage;
    }
  }, [sessionActive]);

  // Start session: get mic, set up Hark, set up MediaRecorder
  const startSession = async () => {
    if (sessionActive) return;
    console.log("[startSession] Starting session...");
    setSessionActive(true);

    const mockSocket = new FakeWebSocket();
    socketRef.current = mockSocket;
    mockSocket.open();

    try {
      console.log("[startSession] Requesting microphone...");
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      micStreamRef.current = stream;

      console.log("[startSession] Microphone acquired. Setting up Hark + MediaRecorder...");
      
      // 1) Use Hark
      const speechEvents = hark(stream, {
        interval: 50,   // how often to check volume
        threshold: -50, // decibels, tweak as needed
      });

      speechEvents.on("speaking", () => {
        console.log("[Hark] speaking event => user is interrupting");
        setIsSpeaking(true);
      
        // Barge-in logic: pause the TTS audio
        if (ttsAudioRef.current) {
          ttsAudioRef.current.pause();
        }
      
      });

      // Immediately finalize speech on "stopped_speaking"
      speechEvents.on("stopped_speaking", () => {
        console.log("[Hark] stopped_speaking event");
        setIsSpeaking(false);
        finalizeUserSpeech();
      });

      // 2) MediaRecorder to send audio chunks
      const recorder = new MediaRecorder(stream);
      recorder.ondataavailable = (e) => {
        console.log(`[ondataavailable] chunk size: ${e.data.size}, type: ${e.data.type}`);
        if (micMuted) {
          console.log("[ondataavailable] micMuted, skipping chunk.");
          return;
        }
        if (e.data.size > 0 && socketRef.current) {
          console.log("[ondataavailable] Sending chunk to server...");
          socketRef.current.send(e.data);
        }
      };
      recorder.start(500);
      mediaRecorderRef.current = recorder;
    } catch (err) {
      console.error("[startSession] Mic access error:", err);
    }
  };

  // finalize user speech immediately
  const finalizeUserSpeech = () => {
    console.log("[finalizeUserSpeech] We'll add a user bubble, then server responds w/ audio.");

    const userMsg: ChatMessage = {
      id: Date.now(),
      sender: "User",
      message: "User's final transcribed text (mock)",
      timestamp: new Date(),
    };
    setChatMessages((prev) => [...prev, userMsg]);

    if (socketRef.current) {
      console.log("[finalizeUserSpeech] telling mock socket to respond w/ audio");
      socketRef.current.serverRespondWithAudio();
    }
  };

  const stopSession = () => {
    console.log("[stopSession] Ending session...");
    setSessionActive(false);

    // Stop code-based TTS playback
    if (ttsAudioRef.current) {
        ttsAudioRef.current.pause();
        ttsAudioRef.current.src = "";  // discard old audio
        ttsAudioRef.current = null;
    }

    // 1) Stop the MediaRecorder
    if (mediaRecorderRef.current && mediaRecorderRef.current.state !== "inactive") {
        console.log("[stopSession] Stopping MediaRecorder...");
        mediaRecorderRef.current.stop();
        mediaRecorderRef.current = null;
    }

    // 2) Stop the mic stream tracks
    if (micStreamRef.current) {
        console.log("[stopSession] Stopping all mic tracks...");
        micStreamRef.current.getTracks().forEach(track => track.stop());
        micStreamRef.current = null;
    }

    // 3) (Optional) If you're using Hark and stored it in a ref, stop it:
    if (harkRef.current) {
        console.log("[stopSession] Stopping Hark...");
        harkRef.current.stop();
        harkRef.current = null;
    }

    // 4) Close the FakeWebSocket
    if (socketRef.current) {
      console.log("[stopSession] Closing FakeWebSocket...");
      socketRef.current.close();
      socketRef.current = null;
    }
    setIsSpeaking(false);
  };

  const toggleMic = () => {
    console.log("[toggleMic] toggling mic");
    if (!micStreamRef.current) return;
  
    const track = micStreamRef.current.getAudioTracks()[0];
    if (track) {
      track.enabled = !track.enabled; // flip track.enabled
      console.log("[toggleMic]", track.enabled ? "unmuted" : "muted");
    }
    setMicMuted(!micMuted);
  };

  const handleCamClick = () => {
    console.log("[handleCamClick] Camera button clicked (not implemented).");
  };

  return (
    <div
      className="hero h-screen relative bg-base-100"
      style={{ backgroundImage: `url(${therapyRoom})`, backgroundSize: "cover" }}
    >
      <div className="hero-overlay bg-neutral opacity-75"></div>

      <div className="absolute top-0 left-0 w-full h-full flex items-center justify-center">
        <div className="w-4/5">
          <div
            ref={chatContainerRef}
            className="chat-container max-h-[60vh] overflow-y-auto px-4"
          >
            {chatMessages.map((msg) => (
              <div
                key={msg.id}
                className={`chat mb-4 ${
                  msg.sender === "User" ? "chat-end" : "chat-start"
                }`}
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
                  {msg.audioUrl /*&& (
                   <audio src={msg.audioUrl} autoPlay controls={false} />
                  )*/}
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

      {/* Microphone & Camera */}
      {sessionActive && (
        <div className="absolute bottom-5 left-1/2 transform -translate-x-1/2 flex items-center gap-4">
          <button
            onClick={toggleMic}
            className={`btn btn-circle btn-sm ${
              micMuted ? "bg-red-700" : "bg-neutral"
            }`}
          >
            <span className="text-neutral-content">
              <FaMicrophone />
            </span>
          </button>
          <button
            onClick={handleCamClick}
            className="btn btn-circle btn-sm bg-neutral hover:bg-neutral-focus"
          >
            <FaVideo />
          </button>
        </div>
      )}

      <div className="absolute bottom-5 right-5 avatar">
        <div className="w-38 rounded border-4 border-neutral">
          <img
            src="https://img.daisyui.com/images/stock/photo-1534528741775-53994a69daeb.webp"
            alt="User Avatar"
          />
        </div>
      </div>
    </div>
  );
};

export default CBTInterface;