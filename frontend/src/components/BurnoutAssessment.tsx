// src/components/BurnoutAssessment.tsx
import React, { useState, useEffect } from "react";
import { FaArrowRight } from "react-icons/fa";
import LikertQuestion from "./LikertQuestion";
import TextQuestion from "./TextQuestion";
import VlogQuestion from "./VlogQuestion";

// --- import shared types ---
import type {
  BurnoutQuestion,
  SessionPayload,
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
    id: 4,
    type: "vlog",
    content: "Record a 30-second video describing your current energy levels.",
  },
  {
    id: 5,
    type: "vlog",
    content: "Record a short clip telling us what you do to unwind after a stressful day.",
  },
];



const BurnoutAssessment: React.FC = () => {
  const [sessionId, setSessionId] = useState<SessionPayload["sessionId"] | null>(null);
  //const [questions, setQuestions] = useState<BurnoutQuestion[]>([]);
  const [questions, setQuestions] = useState<BurnoutQuestion[]>(mockQuestions);
  const [currentIndex, setCurrentIndex] = useState<number>(0);
  const [responses, setResponses] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // 1. On mount: fetch session + questions
 /*  useEffect(() => {
    fetch("/api/assessment") // your endpoint that returns SessionPayload
      .then((res) => {
        if (!res.ok) throw new Error(`Error ${res.status}`);
        return res.json() as Promise<SessionPayload>;
      })
      .then(({ sessionId, questions }) => {
        setSessionId(sessionId);
        setQuestions(questions);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setError("Could not load assessment. Please try again.");
        setLoading(false);
      });
  }, []); */

// skip the fetch in useEffect until your backend is up
useEffect(() => {
  setLoading(false);
}, []);

  // 2. Handle each answer immediately
  const handleAnswer = (questionId: number, answer: string) => {
    // locally mark as answered (to enable Next button)
    setResponses((prev) => ({ ...prev, [questionId]: answer }));

    if (!sessionId) {
      console.warn("No session yet; skipping send");
      return;
    }

    // find the questionType
    const questionType = questions.find((q) => q.id === questionId)!.type;
    const questionContent = questions.find(q => q.id === questionId)!.content;

    const payload: AnswerPayload = {
      sessionId,
      questionId,
      questionType,
      answer,
      questionContent,
    };

    fetch(`/api/assessment/${sessionId}/responses`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    }).catch((err) => {
      console.error("Failed to submit answer:", err);
      // optionally queue retry logic here
    });
  };

  // 3. Move to next question or finish
  const handleNext = () => {
    if (currentIndex < questions.length - 1) {
      setCurrentIndex((i) => i + 1);
    } else {
      alert("All done—thanks for completing the assessment!");
    }
  };

  // 4. Render loading / error / empty states
  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <span className="loading loading-spinner text-primary"></span>
      </div>
    );
  }
  if (error) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center">
        <p className="text-red-500 mb-4">{error}</p>
        <button className="btn btn-secondary" onClick={() => window.location.reload()}>
          Retry
        </button>
      </div>
    );
  }
  if (!questions.length) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <p>No questions available.</p>
      </div>
    );
  }

  // 5. Main render: Typeform-like layout
  const q = questions[currentIndex];
  return (
    <div
      className="min-h-screen w-full bg-base-200 flex flex-col justify-center items-center px-4"
      data-theme="calming"
    >
      <div className="max-w-3xl w-full text-center py-10">
        <h1 className="text-3xl font-bold mb-2">{q.content}</h1>
        {q.subtitle && (
          <p className="text-sm text-gray-600 mb-8">{q.subtitle}</p>
        )}

        <div className="mb-10">
          {q.type === "likert" && <LikertQuestion question={q} onChange={handleAnswer} />}
          {q.type === "open_text" && <TextQuestion question={q} onChange={handleAnswer} />}
          {q.type === "vlog" && <VlogQuestion
                                  question={q}
                                  sessionId={sessionId!}    // pass down the session
                                  onChange={handleAnswer}
                                />}
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
