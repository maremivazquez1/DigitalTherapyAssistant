// src/types/burnout/assessment.ts

export type QuestionType = "likert" | "open_text" | "vlog";

export interface BurnoutQuestion {
  id: number;
  type: QuestionType;
  content: string;
  subtitle?: string;
}

export interface SessionPayload {
  sessionId: string;
  questions: BurnoutQuestion[];
}

/** Now includes the sessionId */
export interface AnswerPayload {
  sessionId: string;
  questionId: number;
  questionType: QuestionType;
  answer: string;
}
