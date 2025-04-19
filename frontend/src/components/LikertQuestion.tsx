import React from "react";

interface LikertQuestionProps {
  // Adjust your question interface as needed. Here we use id and content.
  question: {
    id: number;
    content: string;
  };
  onChange: (questionId: number, answer: string) => void;
}

const LIKERT_OPTIONS = ["Never", "Rarely", "Sometimes", "Often", "Always"];

const LikertQuestion: React.FC<LikertQuestionProps> = ({ question, onChange }) => {
  return (
    <div className="flex flex-wrap justify-center gap-4">
      {LIKERT_OPTIONS.map((option) => (
        <button
          key={option}
          onClick={() => onChange(question.id, option)}
          className="btn btn-outline btn-lg"
        >
          {option}
        </button>
      ))}
    </div>
  );
};

export default LikertQuestion;
