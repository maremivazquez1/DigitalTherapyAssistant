// src/components/VlogQuestion.tsx
import React, { useRef, useState, useEffect } from "react";
import type { BurnoutQuestion } from "../types/burnout/assessment";

// pick MIME
function blobType(chunks: Blob[]): string {
  if (!chunks.length) return "video/webm";
  return chunks[0].type || "video/webm";
}

interface VlogQuestionProps {
  question: BurnoutQuestion;
  onChange: (questionId: number, answer: string) => void;
}

const VlogQuestion: React.FC<VlogQuestionProps> = ({
  question: { questionId },
  onChange,
}) => {
  const videoRef = useRef<HTMLVideoElement>(null);

  // three recorders: video-only, audio-only, full
  const videoRecorderRef = useRef<MediaRecorder | null>(null);
  const audioRecorderRef = useRef<MediaRecorder | null>(null);
  const fullRecorderRef  = useRef<MediaRecorder | null>(null);

  const [stream, setStream] = useState<MediaStream | null>(null);
  const [recording, setRecording] = useState(false);

  // for backend
  const [videoUrl, setVideoUrl] = useState<string | null>(null);
  const [audioUrl, setAudioUrl] = useState<string | null>(null);
  // for preview
  const [combinedUrl, setCombinedUrl] = useState<string | null>(null);

  const videoChunks    = useRef<Blob[]>([]);
  const audioChunks    = useRef<Blob[]>([]);
  const combinedChunks = useRef<Blob[]>([]);

  // 1) getUserMedia once
  useEffect(() => {
    let local: MediaStream;
    navigator.mediaDevices
      .getUserMedia({ video: true, audio: true })
      .then((ms) => {
        local = ms;
        setStream(ms);
      })
      .catch(console.error);
    return () => local?.getTracks().forEach((t) => t.stop());
  }, []);

  // 2) preview full-stream or live
  useEffect(() => {
    const v = videoRef.current;
    if (!v) return;

    if (combinedUrl) {
      // play recorded ↯ audio+video
      v.srcObject = null;
      v.src       = combinedUrl;
      v.controls  = true;
      v.muted     = false;
      v.play().catch(() => {});
    } else if (stream) {
      // live preview muted
      v.src       = "";
      v.srcObject = stream;
      v.controls  = false;
      v.muted     = true;
      v.play().catch(() => {});
    }
  }, [stream, combinedUrl]);

  // 3) reset on new question
  useEffect(() => {
    setVideoUrl(null);
    setAudioUrl(null);
    setCombinedUrl(null);
    setRecording(false);
    videoChunks.current    = [];
    audioChunks.current    = [];
    combinedChunks.current = [];
  }, [questionId]);

  // 4) start all three
  const handleStart = () => {
    if (!stream) return;
    setRecording(true);

    // clear old URLs
    setVideoUrl(null);
    setAudioUrl(null);
    setCombinedUrl(null);

    videoChunks.current    = [];
    audioChunks.current    = [];
    combinedChunks.current = [];

    // --- video-only ---
    const vStream = new MediaStream(stream.getVideoTracks());
    const vr = new MediaRecorder(vStream);
    vr.ondataavailable = (e) => e.data.size && videoChunks.current.push(e.data);
    vr.onstop = () => {
      const blob = new Blob(videoChunks.current, { type: blobType(videoChunks.current) });
      setVideoUrl(URL.createObjectURL(blob));
    };
    videoRecorderRef.current = vr;
    vr.start();

    // --- audio-only ---
    const aStream = new MediaStream(stream.getAudioTracks());
    const ar = new MediaRecorder(aStream);
    ar.ondataavailable = (e) => e.data.size && audioChunks.current.push(e.data);
    ar.onstop = () => {
      const blob = new Blob(audioChunks.current, { type: blobType(audioChunks.current) });
      setAudioUrl(URL.createObjectURL(blob));
    };
    audioRecorderRef.current = ar;
    ar.start();

    // --- full stream ---
    const fr = new MediaRecorder(stream);
    fr.ondataavailable = (e) => e.data.size && combinedChunks.current.push(e.data);
    fr.onstop = () => {
      const blob = new Blob(combinedChunks.current, { type: blobType(combinedChunks.current) });
      setCombinedUrl(URL.createObjectURL(blob));
      setRecording(false);
    };
    fullRecorderRef.current = fr;
    fr.start();
  };

  // 5) stop all
  const handleStop = () => {
    videoRecorderRef.current?.stop();
    audioRecorderRef.current?.stop();
    fullRecorderRef.current?.stop();
  };

  // 6) re-record
  const handleReRecord = () => {
    handleStart();
  };

  // 7) submit backend payload
  const handleSubmit = () => {
    if (videoUrl && audioUrl) {
      onChange(questionId, JSON.stringify({ videoUrl, audioUrl }));
    }
  };

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

      {!combinedUrl ? (
        <button
          onClick={recording ? handleStop : handleStart}
          disabled={!stream}
          className={`btn btn-accent btn-lg ${recording ? "btn-error" : ""}`}
        >
          {recording ? "Stop Recording" : "Record Video"}
        </button>
      ) : (
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
          : combinedUrl
          ? "Review your video or re-record."
          : stream
          ? "Click to start recording your response."
          : "Preparing camera…"}
      </p>
    </div>
  );
};

export default VlogQuestion;
