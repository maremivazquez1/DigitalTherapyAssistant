// src/types/burnout/assessment.ts

//export type QuestionType = "likert" | "open_text" | "vlog";

export interface BurnoutQuestion {
  questionId: number;
  //type: QuestionType;
  multimodal: boolean
  //content: string;
  question: string;
  domain: string;
  //subtitle?: string;
}

export interface SessionPayload {
  sessionId: string;
  requestId?: string;
  questions: BurnoutQuestion[];
}

export interface AnswerPayload {
  sessionId: string;
  questionId: number;
  //questionType: QuestionType;
  multimodal: boolean;
  answer: string;
  questionContent?: string;
}
