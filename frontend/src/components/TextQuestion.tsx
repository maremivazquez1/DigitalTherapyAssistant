import React, { ChangeEvent } from "react";

interface TextQuestionProps {
  question: {
    id: number;
    content: string;
  };
  onChange: (questionId: number, answer: string) => void;
}

const TextQuestion: React.FC<TextQuestionProps> = ({ question, onChange }) => {
  const handleChange = (e: ChangeEvent<HTMLTextAreaElement>) => {
    onChange(question.id, e.target.value);
  };

  return (
<div>
      <textarea
        placeholder="Type your answer here..."
        onChange={handleChange}
        className="textarea textarea-bordered w-full max-w-md mx-auto resize-none p-4"
        rows={6}
      ></textarea>
    </div>
  );
};

export default TextQuestion;
