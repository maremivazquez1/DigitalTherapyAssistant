// src/components/BurnoutAssessment.tsx
import React, { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { FaArrowRight } from "react-icons/fa";
import { v4 as uuidv4 } from "uuid";
import LikertQuestion from "./LikertQuestion";
import VlogQuestion from "./VlogQuestion";
import { useWebSocket } from "../hooks/useWebSocket";
import type { BurnoutQuestion } from "../types/burnout/assessment";

const BurnoutAssessment: React.FC = () => {
  const navigate = useNavigate();
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [questions, setQuestions] = useState<BurnoutQuestion[]>([]);
  const [currentIndex, setCurrentIndex] = useState<number>(0);
  const [responses, setResponses] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState<boolean>(true);
  const [ ,setResultsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [finalResult, setFinalResult] = useState<any>(null);

  const requestId = useRef(uuidv4());
  const { isConnected, messages, sendMessage } = useWebSocket(
    "ws://localhost:8080/ws/burnout",
    true
  );

  // 1) Start session once WebSocket is open
useEffect(() => {
  if (!isConnected) return;
  console.log("[BurnoutAssessment] WebSocket open, sending start-burnout");
  sendMessage(
    JSON.stringify({ type: "start-burnout", requestId: requestId.current, userId: localStorage.getItem("userId") })
  );
}, [isConnected]);

  // 2) Receive questions
  useEffect(() => {
    for (const msg of messages) {
      if ((msg as any).error) {
        setError((msg as any).error);
        setLoading(false);
        return;
    }
      if (msg.type === "burnout-questions") {
        const data = msg as any;
        setSessionId(data.sessionId);
        setQuestions(data.questions as BurnoutQuestion[]);
        setLoading(false);
      }
      if (msg.type === "assessment-result") {
        console.log("[BurnoutAssessment] Received assessment result:", msg);
        const { score, summary } = msg as any;
        setFinalResult({ score, summary });
      }
    }
  }, [messages]);

  useEffect(() => {
    if (finalResult != null) {
      console.log("[BurnoutAssessment] Final result received:", finalResult);
      navigate("/burnout-summary", { state: { result: finalResult } });
    }
  }, [finalResult, navigate]);

  // 3) Local answer store
  const handleAnswer = (questionId: number, answer: string) => {
    setResponses((prev) => ({ ...prev, [questionId]: answer }));
  };

  // 4) Send current answer then advance or finish
  const handleNext = async () => {
    const q = questions[currentIndex];
    const answer = responses[q.questionId];
    if (!sessionId || answer == null) return;
  
    if (q.multimodal) {
      // 1) parse back the two URLs from the stringified payload
      let media: { videoUrl: string; audioUrl: string };
      try {
        media = JSON.parse(answer);
      } catch (err) {
        console.error("Invalid media payload", err);
        return;
      }
  
      // 2a) audio
      sendMessage(JSON.stringify({
        type: "audio_upload",
        sessionId,
        questionId: q.questionId,
      }));
      try {
        const audioBlob = await fetch(media.audioUrl).then(r => r.blob());
        sendMessage(audioBlob);
      } catch (err) {
        console.error("Failed to fetch/send audio blob", err);
      }
  
      // 2b) video
      sendMessage(JSON.stringify({
        type: "video_upload",
        sessionId,
        questionId: q.questionId,
      }));
      try {
        const videoBlob = await fetch(media.videoUrl).then(r => r.blob());
        sendMessage(videoBlob);
      } catch (err) {
        console.error("Failed to fetch/send video blob", err);
      }
  
    } else {
      // likert & open text
      sendMessage(
        JSON.stringify({
          type: "answer",
          sessionId,
          questionId: q.questionId,
          response: answer,
        })
      );
    }

    // if last question, signal completion
    if (currentIndex === questions.length - 1) {
      if (!finalResult) { // Ensure assessment-complete is sent only once
        setResultsLoading(true);
        sendMessage(
          JSON.stringify({ type: "assessment-complete", sessionId })
        );
      }
      return;
    }

    setCurrentIndex(i => i + 1);
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <span className="loading loading-spinner text-primary mr-2"></span>
        <span>Loading assessment…</span>
      </div>
    );
  }
  if (error) return (
    <div className="min-h-screen flex items-center justify-center">
      <p className="text-red-500">{error}</p>
    </div>
  );
  if (!questions.length) return <div>No questions available.</div>

  const q = questions[currentIndex];
  return (
    <div className="min-h-screen bg-base-200 flex flex-col items-center px-4" data-theme="calming">
      <div className="max-w-3xl w-full text-center py-10">
        <h1 className="text-3xl font-bold mb-2">{q.question}</h1>

        <div className="mb-10" >
          {q.multimodal === false && <LikertQuestion question={q} onChange={handleAnswer} />}
          {q.multimodal == true && <VlogQuestion question={q} onChange={handleAnswer} />}
        </div>

        <button
          data-testid="next-button"
          onClick={handleNext}
          disabled={responses[q.questionId] == null}
          className="btn btn-primary btn-circle btn-lg"
        >
          <FaArrowRight className="text-xl" />
        </button>

        <p className="text-gray-500 text-sm mt-4">
          Question {currentIndex + 1} of {questions.length}
        </p>
      </div>
    </div>
  );
};

export default BurnoutAssessment;
