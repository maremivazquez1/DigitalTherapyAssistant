import React, { useState, useEffect, ChangeEvent } from "react";
import LikertQuestion from "./LikertQuestion";
import TextQuestion from "./TextQuestion";
import VlogQuestion from "./VlogQuestion";

interface Question {
  id: number;
  type: "likert" | "open_text" | "vlog";
  content: string;
  // Additional fields if necessary
}

// Mock questions array for styling adjustments
const mockQuestions: Question[] = [
    {
      id: 1,
      type: "likert",
      content: "How often in the past 2 weeks have you felt stressed at work?",
    },
    {
      id: 2,
      type: "open_text",
      content: "Can you describe any factors that contributed to your stress?",
    },
    {
      id: 3,
      type: "vlog",
      content: "Record a short video about your energy levels today.",
    },
    {
      id: 4,
      type: "likert",
      content: "How often in the past 2 weeks have you felt fatigued?",
    },
    {
      id: 5,
      type: "open_text",
      content: "What steps have you taken to manage your stress?",
    },
  ];

const BurnoutAssessment: React.FC = () => {
  const [questions, setQuestions] = useState<Question[]>(mockQuestions);
  const [currentIndex, setCurrentIndex] = useState<number>(0);
  const [responses, setResponses] = useState<{ [key: number]: string }>({});
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    fetch("/api/assessment")
      .then((res) => res.json())
      .then((data: Question[]) => {
        setQuestions(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error("Error fetching assessment questions:", err);
        setLoading(false);
      });
  }, []);

  const handleAnswer = (questionId: number, answer: string) => {
    setResponses((prev) => ({ ...prev, [questionId]: answer }));
  };

  const handleNext = () => {
    if (currentIndex < questions.length - 1) {
      setCurrentIndex((prev) => prev + 1);
    } else {
      // Final submission logic goes here.
      console.log("All responses:", responses);
    }
  };

  const renderQuestion = (q: Question) => {
    switch (q.type) {
      case "likert":
        return <LikertQuestion question={q} onChange={handleAnswer} />;
      case "open_text":
        return <TextQuestion question={q} onChange={handleAnswer} />;
      case "vlog":
        return <VlogQuestion question={q} onChange={handleAnswer} />;
      default:
        return <div>Unknown question type: {q.type}</div>;
    }
  };

  if (loading) {
    return <div>Loading assessment...</div>;
  }

  if (!questions.length) {
    return <div>No questions available.</div>;
  }

  return (
    <div
      // Full-screen hero section
      className="min-h-screen w-full bg-base-200 flex flex-col justify-center items-center px-4"
      data-theme="calming"
    >
      {/* Question container */}
      <div className="max-w-3xl w-full text-center py-10">
        <h1 className="text-3xl font-bold mb-2">
          {questions[currentIndex].content}
        </h1>

        {/* Render the question component */}
        <div className="mb-10">
        {renderQuestion(questions[currentIndex])}
        </div>

        {/* Next/Submit button */}
        <button
          onClick={handleNext}
          className="btn btn-primary px-8 py-3 text-lg"
        >
          {currentIndex === questions.length - 1 ? "Submit" : "Next"}
        </button>

        {/* Optional question indicator */}
        <p className="text-gray-500 text-sm mt-4">
          Question {currentIndex + 1} of {questions.length}
        </p>
      </div>
    </div>
  );
};

export default BurnoutAssessment;