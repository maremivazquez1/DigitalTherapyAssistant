// src/components/VlogQuestion.tsx
import React, { useRef, useState, useEffect } from "react";
import { useWebSocket } from "../hooks/useWebSocket";

interface VlogQuestionProps {
  question: {
    id: number;
    content: string;
  };
  sessionId: string;
  onChange: (questionId: number, answer: string) => void;
}

const VlogQuestion: React.FC<VlogQuestionProps> = ({
  question: { id: questionId, content },
  sessionId,
  onChange,
}) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const [stream, setStream] = useState<MediaStream | null>(null);
  const [recording, setRecording] = useState(false);

  // Only connect WS while recording
  const { isConnected, sendMessage } = useWebSocket(
    "ws://localhost:8080/ws/burnout",
    recording
  );

  // 1. Acquire camera + mic and show live preview
  useEffect(() => {
    let localStream: MediaStream;
    navigator.mediaDevices
      .getUserMedia({ video: true, audio: true })
      .then((ms) => {
        localStream = ms;
        setStream(ms);
        if (videoRef.current) {
          videoRef.current.srcObject = ms;
        }
      })
      .catch((err) => console.error("Error accessing camera/mic:", err));

    return () => {
      localStream?.getTracks().forEach((t) => t.stop());
    };
  }, []);

  // 2. Start recording
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

      // Notify parent; parent’s handleAnswer will POST JSON
      onChange(questionId, videoUrl);

      // Send a control message so backend knows what’s coming
      if (isConnected) {
        sendMessage(
          JSON.stringify({
            type: "video_upload",
            sessionId,
            questionId,
          })
        );
        // Then send the raw blob
        sendMessage(blob);
      } else {
        console.warn("WebSocket not connected; video not sent");
      }
    };

    mediaRecorderRef.current = recorder;
    recorder.start();
    setRecording(true);
  };

  // 3. Stop recording
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
          Your browser doesn’t support video.
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
          ? "Recording… click again to stop."
          : "Click to start recording your response."}
      </p>
    </div>
  );
};

export default VlogQuestion;
