import React, { useState, useEffect } from "react";
import type { BurnoutQuestion } from "../types/burnout/assessment";

interface LikertQuestionProps {
  question: BurnoutQuestion;                // use the shared type
  onChange: (questionId: number, answer: string) => void;
}

const LIKERT_OPTIONS = ["Never", "Rarely", "Sometimes", "Often", "Always"];

const LikertQuestion: React.FC<LikertQuestionProps> = ({ question, onChange }) => {
  // keep track of selection locally
  const [selected, setSelected] = useState<string | null>(null);

  // if your question object already has an answer field, you can
  // initialize from it and re-sync when it changes:
  useEffect(() => {
    if ((question as any).answer) {
      setSelected((question as any).answer as string);
    }
  }, [question]);

  return (
    <div className="flex flex-wrap justify-center gap-4">
      {LIKERT_OPTIONS.map((option) => {
        const isActive = selected === option;
        return (
          <button
            key={option}
            onClick={() => {
              setSelected(option);
              onChange(question.questionId, option);
            }}
            className={
              // use `btn-accent` (filled) when active, otherwise outline
              `btn btn-lg ${
                isActive ? "btn-ghost btn-active" : "btn-outline"
              }`
            }
          >
            {option}
          </button>
        );
      })}
    </div>
  );
};

export default LikertQuestion;
