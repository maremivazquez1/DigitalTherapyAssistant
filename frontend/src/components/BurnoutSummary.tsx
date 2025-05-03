import React from "react";
import type { BurnoutQuestion } from "../types/burnout/assessment";

interface BurnoutSummaryProps {
  questions: BurnoutQuestion[];
  responses: Record<number, string>;
  onRestart: () => void;
}

const BurnoutSummary: React.FC<BurnoutSummaryProps> = ({
  questions,
  responses,
  onRestart,
}) => {
  return (
    <div
      className="min-h-screen bg-base-200 flex flex-col items-center px-4 py-12"
      data-theme="calming"
    >
      <h1 className="text-4xl font-bold mb-8">Assessment Summary</h1>
      <div className="w-full max-w-3xl space-y-6">
        {questions.map((q) => {
          const answer = responses[q.id];
          return (
            <div key={q.id} className="card bg-base-100 shadow-md p-6">
              <h2 className="text-xl font-semibold mb-2">{q.content}</h2>
              {q.subtitle && (
                <p className="text-sm text-gray-500 mb-4">{q.subtitle}</p>
              )}
              {q.type === "vlog" && answer && (
                <video
                  src={answer}
                  controls
                  className="w-full max-w-md rounded"
                />
              )}
              {(q.type === "likert" || q.type === "open_text") && (
                <p className="text-lg">
                  {answer || (
                    <span className="italic text-gray-400">No response</span>
                  )}
                </p>
              )}
            </div>
          );
        })}
      </div>
      <button onClick={onRestart} className="btn btn-primary mt-12">
        Restart Assessment
      </button>
    </div>
  );
};

export default BurnoutSummary;
