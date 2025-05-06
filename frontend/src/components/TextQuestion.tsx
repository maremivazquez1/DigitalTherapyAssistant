// src/components/TextQuestion.tsx
import React, { ChangeEvent } from "react";
import type { BurnoutQuestion } from "../types/burnout/assessment";

interface TextQuestionProps {
  question: BurnoutQuestion;
  onChange: (questionId: number, answer: string) => void;
}

const TextQuestion: React.FC<TextQuestionProps> = ({ question, onChange }) => {
  const handleChange = (e: ChangeEvent<HTMLTextAreaElement>) => {
    onChange(question.questionId, e.target.value);
  };

  return (
    <div>
      <textarea
        placeholder="Type your answer here..."
        onChange={handleChange}
        className="textarea textarea-bordered w-full max-w-md mx-auto resize-none p-4"
        rows={6}
      />
    </div>
  );
};

export default TextQuestion;
