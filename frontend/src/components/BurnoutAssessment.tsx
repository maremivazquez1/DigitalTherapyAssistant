// src/components/BurnoutAssessment.tsx
import React, { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { FaArrowRight } from "react-icons/fa";
import { v4 as uuidv4 } from "uuid";
import LikertQuestion from "./LikertQuestion";
import TextQuestion from "./TextQuestion";
import VlogQuestion from "./VlogQuestion";
import { useWebSocket } from "../hooks/useWebSocket";
import type {
  BurnoutQuestion,
  AnswerPayload,
} from "../types/burnout/assessment";

export const mockQuestions: BurnoutQuestion[] = [
  // Likert questions
  {
    id: 1,
    type: "likert",
    content: "How often in the past 2 weeks have you felt overwhelmed at work?",
    subtitle: "Consider your busiest days",
  },
  {
    id: 2,
    type: "likert",
    content: "How often in the past 2 weeks have you found it hard to concentrate?",
  },
  {
    id: 3,
    type: "likert",
    content: "How often in the past 2 weeks have you felt emotionally drained?",
  },

  // Vlog questions
  {
    id: 5,
    type: "vlog",
    content: "Record a 30-second video describing your current energy levels.",
  },
  {
    id: 6,
    type: "vlog",
    content: "Record a short clip telling us what you do to unwind after a stressful day.",
  },
];

const BurnoutAssessment: React.FC = () => {
  const navigate = useNavigate();
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [questions, setQuestions] = useState<BurnoutQuestion[]>([]);
  const [currentIndex, setCurrentIndex] = useState<number>(0);
  const [responses, setResponses] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState<boolean>(true);

  // generate a requestId for session kickoff
  const requestId = useRef(uuidv4());

  // open burnout WebSocket immediately
  const { isConnected, messages, sendMessage } = useWebSocket(
    "ws://localhost:8080/ws/burnout",
    true
  );

  // 1) Kick off the session via WebSocket
  useEffect(() => {
    sendMessage(
      JSON.stringify({
        type: "start-burnout",
        requestId: requestId.current,
      })
    );
  }, [sendMessage]);

  // 2) Load mock questions once
  useEffect(() => {
    setSessionId("mock-session-123");
    setQuestions(mockQuestions);
    setLoading(false);
  }, []);

  // 3) Gather answer for the current question
  const handleAnswer = (questionId: number, answer: string) => {
    setResponses((prev) => ({ ...prev, [questionId]: answer }));
  };

  // 4) Move to next question or finish
  const handleNext = async () => {
    const q = questions[currentIndex];
    const answer = responses[q.id];
    if (!sessionId || answer == null) return;

    if (q.type === "vlog") {
      // inform server of incoming blob
      sendMessage(
        JSON.stringify({ type: "vlog", sessionId, questionId: q.id })
      );
      // fetch blob and send via WS
      try {
        const blob = await fetch(answer).then((r) => r.blob());
        sendMessage(blob);
      } catch (err) {
        console.error("Failed to fetch/send video blob", err);
      }
    } else {
      // likert & open text
      sendMessage(
        JSON.stringify({
          type: "answer",
          sessionId,
          questionId: q.id,
          response: answer,
        })
      );
    }

    // advance or done
    if (currentIndex < questions.length - 1) {
      setCurrentIndex((i) => i + 1);
    } else {
      navigate("/burnout-summary", { state: { questions, responses } });
    }
  };


  // 5) Render loading / no-questions states
  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        Loading assessment…
      </div>
    );
  }
  if (!questions.length) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        No questions available.
      </div>
    );
  }

  // 6) Main question render
  const q = questions[currentIndex];
  return (
    <div
      className="min-h-screen w-full bg-base-200 flex flex-col justify-center items-center px-4"
      data-theme="calming"
    >
      <div className="max-w-3xl w-full text-center py-10">
        <h1 className="text-3xl font-bold mb-2">{q.content}</h1>
        {q.subtitle && <p className="text-sm text-gray-600 mb-8">{q.subtitle}</p>}

        <div className="mb-10">
          {q.type === "likert" && <LikertQuestion question={q} onChange={handleAnswer} />}
          {q.type === "open_text" && <TextQuestion question={q} onChange={handleAnswer} />}
          {q.type === "vlog" && (
            <VlogQuestion
              question={q}
              onChange={handleAnswer}
            />
          )}
        </div>

        <button
          onClick={handleNext}
          disabled={responses[q.id] == null}
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
