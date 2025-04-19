import React, { useState, useRef, useEffect } from "react";

interface VlogQuestionProps {
  question: {
    id: number;
    content: string;
  };
  onChange: (questionId: number, answer: string) => void;
}

const VlogQuestion: React.FC<VlogQuestionProps> = ({ question, onChange }) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [recording, setRecording] = useState<boolean>(false);

  // Placeholder: When you implement video recording logic,
  // this effect can set up the video stream.
  useEffect(() => {
    // Example for live stream (uncomment and adjust in a real scenario):
    // navigator.mediaDevices.getUserMedia({ video: true }).then((stream) => {
    //   if (videoRef.current) {
    //     videoRef.current.srcObject = stream;
    //   }
    // }).catch(err => console.error("Error accessing camera", err));
  }, []);

  // Simulate toggling recording state and returning a placeholder value.
  const handleRecord = () => {
    setRecording((prev) => !prev);
    // For now, just pass a placeholder value.
    onChange(question.id, "recorded_video_placeholder");
  };

  return (
    <div className="flex flex-col items-center">
      {/* Video box styled to match the Typeform layout */}
      <div className="w-full max-w-sm h-64 bg-gray-200 rounded-md overflow-hidden mb-4 shadow-md">
        <video
          ref={videoRef}
          autoPlay
          muted
          playsInline
          className="object-cover w-full h-full"
        >
          {/* Fallback content if video cannot be loaded */}
          Your browser does not support the video tag.
        </video>
      </div>
      <button onClick={handleRecord} className="btn btn-accent btn-lg">
        {recording ? "Stop Recording" : "Record Video"}
      </button>
      <p className="text-sm text-gray-500 mt-4">Tap to record your response.</p>
    </div>
  );
};

export default VlogQuestion;
