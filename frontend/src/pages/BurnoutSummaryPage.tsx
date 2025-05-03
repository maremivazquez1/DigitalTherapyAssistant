import React, { useState } from "react";
import BurnoutSummary from "../components/BurnoutSummary";
import type { BurnoutQuestion } from "../types/burnout/assessment";

interface BurnoutSummaryPageProps {
  initialQuestions: BurnoutQuestion[];
  initialResponses: Record<number, string>;
}

const BurnoutSummaryPage: React.FC<BurnoutSummaryPageProps> = ({
  initialQuestions,
  initialResponses,
}) => {
  const [questions] = useState<BurnoutQuestion[]>(initialQuestions);
  const [responses] = useState<Record<number, string>>(initialResponses);

  const handleRestart = () => {
    window.location.href = "/burnout-assessment";
  };

  return (
    <BurnoutSummary
      questions={questions}
      responses={responses}
      onRestart={handleRestart}
    />
  );
};

export default BurnoutSummaryPage;
