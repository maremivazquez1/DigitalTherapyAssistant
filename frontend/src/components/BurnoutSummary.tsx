// src/components/BurnoutSummary.tsx
import React from "react";
import { useLocation, useNavigate } from "react-router-dom";

interface AssessmentResult {
  type: string;
  sessionId: string;
  score: number;
  summary: string;
}

/**
 * This summary component now pulls its data from React Router's location.state
 * directly, so you can just render it in a route without wrapping it.
 */
const BurnoutSummary: React.FC = () => {
  const { state } = useLocation();
  const navigate = useNavigate();
  const result = (state as any)?.result as AssessmentResult | undefined;

  // If no result was passed, redirect back to the assessment start
  React.useEffect(() => {
    if (!result) {
      navigate("/burnout-assessment");
    }
  }, [result, navigate]);

  if (!result) return null;

  const handleRestart = () => {
    navigate("/burnout-assessment");
  };

  return (
    <div
      className="min-h-screen bg-base-200 flex flex-col items-center px-4 py-12"
      data-theme="calming"
    >
      <h1 className="text-4xl font-bold mb-8">Assessment Summary</h1>

      <div className="w-full max-w-3xl bg-base-100 shadow-md p-6 rounded-lg">
        <h2 className="text-2xl font-semibold mb-4">Your Results</h2>
        <p className="mb-4">
          <strong>Score: {result.score}</strong>
        </p>
        <p className="text-lg whitespace-pre-line">{result.summary}</p>
      </div>

      <button onClick={handleRestart} className="btn btn-primary mt-12">
        Restart Assessment
      </button>
    </div>
  );
};

export default BurnoutSummary;
