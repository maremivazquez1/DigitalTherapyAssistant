// src/components/VlogQuestion.tsx
import React, { useRef, useState, useEffect } from "react";
import { useWebSocket } from "../hooks/useWebSocket";

interface VlogQuestionProps {
  question: {
    id: number;
    content: string;
  };
  onChange: (questionId: number, answer: string) => void;
}

const VlogQuestion: React.FC<VlogQuestionProps> = ({ question, onChange }) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const [stream, setStream] = useState<MediaStream | null>(null);
  const [recording, setRecording] = useState(false);
  const [chunks, setChunks] = useState<Blob[]>([]);

  // Only connect WS when recording starts
  const { isConnected, sendMessage } = useWebSocket(
    "ws://localhost:8080/ws/burnout", 
    recording
  );

  // 1. getUserMedia + live preview
  useEffect(() => {
    navigator.mediaDevices
      .getUserMedia({ video: true, audio: true })
      .then((mediaStream) => {
        setStream(mediaStream);
        if (videoRef.current) {
          videoRef.current.srcObject = mediaStream;
        }
      })
      .catch((err) => console.error("Error accessing camera/mic:", err));

    return () => {
      stream?.getTracks().forEach((t) => t.stop());
    };
  }, []);

  // 2. start recording
  const handleStart = () => {
    if (!stream) return;
    const recorder = new MediaRecorder(stream, { mimeType: "video/webm" });
    const localChunks: Blob[] = [];

    recorder.ondataavailable = (e) => {
      if (e.data.size > 0) localChunks.push(e.data);
    };

    recorder.onstop = () => {
      const blob = new Blob(localChunks, { type: "video/webm" });
      const videoUrl = URL.createObjectURL(blob);

      // notify parent so Next button enables
      onChange(question.id, videoUrl);

      // send raw blob via WebSocket
      if (isConnected) {
        sendMessage(blob);
      } else {
        console.warn("WebSocket not connected; video not sent");
      }

      setChunks([]);
    };

    mediaRecorderRef.current = recorder;
    recorder.start();
    setChunks(localChunks);
    setRecording(true);
  };

  // 3. stop recording
  const handleStop = () => {
    mediaRecorderRef.current?.stop();
    setRecording(false);
  };

  return (
    <div className="flex flex-col items-center">
      <div className="w-full max-w-sm h-64 bg-gray-200 rounded-md overflow-hidden mb-4 shadow-md">
        <video
          ref={videoRef}
          autoPlay
          muted
          playsInline
          className="object-cover w-full h-full"
        >
          Your browser does not support video.
        </video>
      </div>

      <button
        onClick={recording ? handleStop : handleStart}
        className={`btn btn-accent btn-lg ${recording ? "btn-error" : ""}`}
      >
        {recording ? "Stop Recording" : "Record Video"}
      </button>

      <p className="text-sm text-gray-500 mt-4">
        {recording
          ? "Recording… click to stop."
          : "Click to start recording your response."}
      </p>
    </div>
  );
};

export default VlogQuestion;
