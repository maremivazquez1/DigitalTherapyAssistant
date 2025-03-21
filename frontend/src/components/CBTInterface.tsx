import React, { useState, useEffect, useRef } from "react";
import therapyRoom from "../assets/therapy-room-1.svg";
import { FaMicrophone, FaMicrophoneSlash, FaVideo, FaVideoSlash } from "react-icons/fa";
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
  const [cameraOn, setCameraOn] = useState(true); // camera is on by default
  // Track if user is speaking (for UI or logs)
  const [isSpeaking, setIsSpeaking] = useState(false);

// --------------------- REFS ---------------------
  // 1) The combined stream with audio+video
  const combinedStreamRef = useRef<MediaStream | null>(null);

  // 2) Separate recorders for audio & video
  const audioRecorderRef = useRef<MediaRecorder | null>(null);
  const videoRecorderRef = useRef<MediaRecorder | null>(null);

  // For Hark speech detection
  const harkRef = useRef<any>(null);

  // For the FakeWebSocket
  const socketRef = useRef<FakeWebSocket | null>(null);

  // For scrolling the chat
  const chatContainerRef = useRef<HTMLDivElement>(null);

  // For showing user’s video in the avatar
  const videoRef = useRef<HTMLVideoElement | null>(null);


  const ttsAudioRef = useRef<HTMLAudioElement | null>(null);


  // --------------------- SCROLL TO BOTTOM OF CHAT ---------------------
  useEffect(() => {
    if (chatContainerRef.current) {
      chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
    }
  }, [chatMessages]);

  // --------------------- HANDLE SERVER MESSAGES ---------------------
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

  // --------------------- START SESSION ---------------------
  const startSession = async () => {

    if (sessionActive) return;
    console.log("[startSession] Starting session...");
    setSessionActive(true);

    const mockSocket = new FakeWebSocket();
    socketRef.current = mockSocket;
    mockSocket.open();

    try {
      console.log("[startSession] Requesting microphone + camera...");
      const combinedStream = await navigator.mediaDevices.getUserMedia({
        audio: true,
        video: true,
      });
      combinedStreamRef.current = combinedStream; // store in ref

      // 2) Split into audio-only and video-only
      const audioTrack = combinedStream.getAudioTracks()[0];
      const videoTrack = combinedStream.getVideoTracks()[0];
      const audioOnlyStream = new MediaStream([audioTrack]);
      const videoOnlyStream = new MediaStream([videoTrack]);

      console.log("[startSession] Audio & video tracks acquired.");

      
      // Use Hark
      const speechEvents = hark(audioOnlyStream, {
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

      // MediaRecorder to send audio chunks
      const audioRecorder = new MediaRecorder(audioOnlyStream);
      audioRecorderRef.current = audioRecorder;

      audioRecorder.ondataavailable = (e) => {
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
      audioRecorder.start(500);

      // 5) Create a MediaRecorder for video
      const videoRecorder = new MediaRecorder(videoOnlyStream);
      videoRecorderRef.current = videoRecorder;

      videoRecorder.ondataavailable = (e) => {
        console.log(`[videoRecorder] chunk size: ${e.data.size}, type: ${e.data.type}`);
        if (e.data.size > 0 && socketRef.current) {
          console.log("[videoRecorder] Sending video chunk to server...");
          socketRef.current.send(e.data);
        }
      };
      videoRecorder.start(500);

      console.log("[startSession] Audio & video recorders started.");

      // 6) (Optional) Display user's camera in the avatar
      if (videoRef.current) {
          videoRef.current.srcObject = videoOnlyStream;
          videoRef.current.play().catch((err) => {
          console.warn("[Video] Autoplay might be blocked or error:", err);
          });
      }

      } catch (err) {
      console.error("[startSession] Mic/camera access error:", err);
      }
    };

  // --------------------- FINALIZE USER SPEECH ---------------------
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

  // --------------------- STOP SESSION ---------------------
  const stopSession = () => {
    console.log("[stopSession] Ending session...");
    setSessionActive(false);

    // Stop code-based TTS playback
    if (ttsAudioRef.current) {
        ttsAudioRef.current.pause();
        ttsAudioRef.current.src = "";  // discard old audio
        ttsAudioRef.current = null;
    }

    // 1) Stop the audio
    if (audioRecorderRef.current && audioRecorderRef.current.state !== "inactive") {
        console.log("[stopSession] Stopping MediaRecorder...");
        audioRecorderRef.current?.stop();
        audioRecorderRef.current = null;
    }

    // 3) Stop video recorder
    if (videoRecorderRef.current && videoRecorderRef.current.state !== "inactive") {
        console.log("[stopSession] Stopping video recorder...");
        videoRecorderRef.current.stop();
        videoRecorderRef.current = null;
    }

    // Clear the camera feed
    if (videoRef.current) {
        console.log("[stopSession] Clearing camera box...");
        videoRef.current.pause();
        videoRef.current.srcObject = null; 
    }

    // 5) Stop all tracks in the combined stream
    if (combinedStreamRef.current) {
        console.log("[stopSession] Stopping all mic+cam tracks...");
        combinedStreamRef.current.getTracks().forEach((track) => track.stop());
        combinedStreamRef.current = null;
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
    setMicMuted(false);
  };

  // --------------------- TOGGLE MIC ---------------------
  const toggleMic = () => {
    console.log("[toggleMic] toggling mic");
    if (!audioRecorderRef.current) return;
  
    const track = combinedStreamRef.current?.getAudioTracks()[0];
    if (track) {
      track.enabled = !track.enabled; // flip track.enabled
      console.log("[toggleMic]", track.enabled ? "unmuted" : "muted");
    }
    setMicMuted(!micMuted);
  };

  // --------------------- CAMERA CLICK ---------------------
  const handleCamClick = () => {
    console.log("[handleCamClick] Toggling camera");
    if (!combinedStreamRef.current) return; // If there's no combined stream, do nothing
  
    // Get the video track
    const videoTrack = combinedStreamRef.current.getVideoTracks()[0];
    if (videoTrack) {
      // Flip the .enabled property
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

    <div className="absolute bottom-5 left-1/2 transform -translate-x-1/2 flex items-center gap-4">
    {/* Mic Button */}
    <button
        onClick={toggleMic}
        className={`btn btn-circle btn-sm ${
        micMuted ? "bg-warning" : "bg-neutral"
        }`}
    >
        <span className="text-neutral-content">
        {micMuted ? <FaMicrophoneSlash /> : <FaMicrophone />}
        </span>
    </button>

    {/* Camera Button */}
    <button
        onClick={handleCamClick}
        className={`btn btn-circle btn-sm ${
        cameraOn ? "bg-neutral" : "bg-warning"
        }`}
    >
        <span className="text-neutral-content">
        {cameraOn ? <FaVideo /> : <FaVideoSlash />}
        </span>
    </button>
    </div>

      <div className="absolute bottom-5 right-5 avatar">
        <div className="w-38 h-38 rounded border-4 border-neutral overflow-hidden">
          <video
            ref={videoRef}
            autoPlay
            muted
            className="object-cover w-full h-full"
          />
        </div>
      </div>
    </div>
  );
};

export default CBTInterface;
