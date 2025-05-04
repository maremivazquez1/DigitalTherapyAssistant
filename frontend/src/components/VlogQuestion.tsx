// src/components/VlogQuestion.tsx
import React, { useRef, useState, useEffect } from "react";
import type { BurnoutQuestion } from "../types/burnout/assessment";

interface VlogQuestionProps {
  question: BurnoutQuestion;
  onChange: (questionId: number, answer: string) => void;
}

const VlogQuestion: React.FC<VlogQuestionProps> = ({
  question: { questionId: questionId },
  onChange,
}) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const [stream, setStream] = useState<MediaStream | null>(null);
  const [recording, setRecording] = useState(false);
  const [videoUrl, setVideoUrl] = useState<string | null>(null);
  const [videoBlob, setVideoBlob] = useState<Blob | null>(null);

  // 1. Acquire camera + mic for live preview once
  useEffect(() => {
    let localStream: MediaStream;
    navigator.mediaDevices
      .getUserMedia({ video: true, audio: true })
      .then((ms) => {
        localStream = ms;
        setStream(ms);
      })
      .catch((err) => console.error("getUserMedia error:", err));
    return () => localStream?.getTracks().forEach((t) => t.stop());
  }, []);

  // 2. Update <video> element for preview or playback
  useEffect(() => {
    const videoEl = videoRef.current;
    if (!videoEl) return;
    if (videoUrl) {
      videoEl.srcObject = null;
      videoEl.src = videoUrl;
      videoEl.muted = false;
      videoEl.controls = true;
      videoEl.play().catch(() => {});
    } else if (stream) {
      videoEl.src = "";
      videoEl.srcObject = stream;
      videoEl.muted = true;
      videoEl.controls = false;
      videoEl.play().catch(() => {});
    }
  }, [stream, videoUrl]);

  // 3. Reset when question changes
  useEffect(() => {
    setVideoUrl(null);
    setVideoBlob(null);
    setRecording(false);
  }, [questionId]);

  // 4. Start recording
  const handleStart = () => {
    setVideoUrl(null);
    setVideoBlob(null);
    setRecording(true);
    const chunks: Blob[] = [];
    const candidateTypes = [
      "video/webm; codecs=vp8,opus",
      "video/webm; codecs=vp9,opus",
      "video/webm",
      "video/mp4",
    ];
    const mimeType = candidateTypes.find((t) =>
      MediaRecorder.isTypeSupported(t)
    );

    let recorder: MediaRecorder;
    try {
      recorder = mimeType
        ? new MediaRecorder(stream!, { mimeType })
        : new MediaRecorder(stream!);
    } catch (err) {
      console.error("MediaRecorder init failed:", err);
      setRecording(false);
      return;
    }

    recorder.ondataavailable = (e) => {
      if (e.data.size) chunks.push(e.data);
    };
    recorder.onstop = () => {
      const blob = new Blob(chunks, { type: recorder.mimeType });
      const url = URL.createObjectURL(blob);
      setVideoBlob(blob);
      setVideoUrl(url);
      setRecording(false);
    };

    mediaRecorderRef.current = recorder;
    recorder.start();
  };

  // 5. Stop recording
  const handleStop = () => {
    mediaRecorderRef.current?.stop();
  };

  // 6. Re-record or submit
  const handleReRecord = () => handleStart();
  const handleSubmit = () => {
    if (!videoBlob) return;
    onChange(questionId, videoUrl!);
  };

  // 7. Render UI
  return (
    <div className="flex flex-col items-center">
      <div className="w-full max-w-sm h-64 bg-gray-200 rounded-md overflow-hidden mb-4 shadow-md">
        <video
          ref={videoRef}
          autoPlay
          playsInline
          className="object-cover w-full h-full"
        />
      </div>

      {/* Before/while recording: Record or Stop */}
      {!videoUrl && (
        <button
          onClick={recording ? handleStop : handleStart}
          disabled={!stream}
          className={`btn btn-accent btn-lg ${
            recording ? "btn-error" : ""
          }`}
        >
          {recording ? "Stop Recording" : "Record Video"}
        </button>
      )}

      {/* After recording: Re-record or Submit */}
      {videoUrl && (
        <div className="flex gap-4">
          <button onClick={handleReRecord} className="btn btn-outline">
            Re-record
          </button>
          <button onClick={handleSubmit} className="btn btn-primary">
            Submit Recording
          </button>
        </div>
      )}

      <p className="text-sm text-gray-500 mt-4">
        {recording
          ? "Recording…"
          : videoUrl
          ? "Review your video or re-record."
          : stream
          ? "Click to start recording your response."
          : "Preparing camera…"}
      </p>
    </div>
  );
};

export default VlogQuestion;
