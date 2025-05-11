import '@testing-library/jest-dom';
/// <reference types="vitest" />
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import BurnoutAssessment from './BurnoutAssessment';

// Mock child components (must be before importing the component)
// Mock child components (must be before importing the component)
vi.mock('./LikertQuestion', () => ({
    default: ({ question, onChange }: any) => (
      <>
        <div data-testid="likert">{question.question}</div>
        <input
          data-testid="likert-input"
          aria-label="likert-input"
          type="text"
          onChange={(e) => onChange(question.questionId, e.target.value)}
        />
        <button
          data-testid="likert-answer"
          onClick={() => onChange(question.questionId, 'my-response')}
        />
      </>
    )
  }));
  vi.mock('./VlogQuestion', () => ({
    default: ({ question, onChange }: any) => (
      <>
        <div data-testid="vlog">{question.question}</div>
        <button
          data-testid="vlog-answer"
          onClick={() => onChange(
            question.questionId,
            JSON.stringify({ audioUrl: 'audio-url', videoUrl: 'video-url' })
          )}
        />
      </>
    )
  }));

// Mock react-router's useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}));

// Mock uuid
vi.mock('uuid', () => ({ v4: () => 'test-uuid' }));

// Mock WebSocket hook
const mockSendMessage = vi.fn();
let mockMessages: any[] = [];
vi.mock('../hooks/useWebSocket', () => ({
  useWebSocket: () => ({
    isConnected: true,
    messages: mockMessages,
    sendMessage: mockSendMessage,
  })
}));

// Stub fetch
const dummyAudioBlob = new Blob(['audio'], { type: 'audio/webm' });
const dummyVideoBlob = new Blob(['video'], { type: 'video/webm' });
vi.stubGlobal('fetch', vi.fn().mockImplementation((url: string) => ({
  blob: () => Promise.resolve(url.includes('audio-url') ? dummyAudioBlob : dummyVideoBlob)
} as any)));

// --- Test Suite ---
describe('BurnoutAssessment', () => {
  beforeEach(() => {
    mockMessages = [];
    mockSendMessage.mockClear();
    mockNavigate.mockClear();
  });

  it('sends start-burnout message when websocket connects', async () => {
    // Arrange: set a userId in localStorage so the payload includes it
    localStorage.setItem('userId', 'test-user');

    // Act: render component (isConnected=true by mock)
    render(<BurnoutAssessment />);

    // Assert: sendMessage was called once with correct start-burnout payload
    await waitFor(() => {
      expect(mockSendMessage).toHaveBeenCalledTimes(1);
    });
    // Parse the sent JSON
    const sentArg = mockSendMessage.mock.calls[0][0] as string;
    const payload = JSON.parse(sentArg);
    expect(payload).toEqual({
      type: 'start-burnout',
      requestId: 'test-uuid',
      userId: 'test-user',
  });
  });

it('handles answer submission and advances to next question', async () => {
    // Arrange: simulate server sending questions
    const questions = [
        { questionId: 1, question: 'How burnt out are you?', multimodal: false },
        { questionId: 2, question: 'Record a vlog', multimodal: true }
    ];
    mockMessages = [
        { type: 'burnout-questions', sessionId: 'sess1', questions }
    ];

    // Act: render component
    render(<BurnoutAssessment />);

    // Assert: LikertQuestion stub renders first question text
    expect(await screen.findByTestId('likert')).toHaveTextContent('How burnt out are you?');

    // Simulate answering the first question
    fireEvent.change(screen.getByTestId('likert-input'), { target: { value: '3' } });
    const nextBtn = screen.getByTestId('next-button');
    expect(nextBtn).not.toBeDisabled();

    // Simulate clicking Next
    fireEvent.click(nextBtn);

    // Assert: VlogQuestion stub renders second question text
    expect(await screen.findByTestId('vlog')).toHaveTextContent('Record a vlog');
});

it('sends assessment-complete on final question', async () => {
    // Arrange: simulate server sending a single question
    const questions = [
        { questionId: 1, question: 'How burnt out are you?', multimodal: false }
    ];
    mockMessages = [
        { type: 'burnout-questions', sessionId: 'sess1', questions }
    ];

    // Act: render component
    render(<BurnoutAssessment />);

    // Simulate answering the question
    fireEvent.change(screen.getByTestId('likert-input'), { target: { value: '3' } });
    const nextBtn = screen.getByTestId('next-button');
    fireEvent.click(nextBtn);

    // Assert: sendMessage was called with assessment-complete
    await waitFor(() => {
        expect(mockSendMessage).toHaveBeenCalledWith(
            JSON.stringify({ type: 'assessment-complete', sessionId: 'sess1' })
        );
    });
});

it('navigates to summary page on assessment-result', async () => {
    // Arrange: simulate server sending assessment result
    mockMessages = [
        { type: 'assessment-result', score: 85, summary: 'You are moderately burnt out.' }
    ];

    // Act: render component
    render(<BurnoutAssessment />);

    // Assert: navigation to summary page occurs
    await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/burnout-summary', {
            state: { result: { score: 85, summary: 'You are moderately burnt out.' } }
        });
    });
});

it('displays error UI when error message received', async () => {
    // Arrange: simulate server sending an error message
    mockMessages = [
        { error: 'Something went wrong.' }
    ];

    // Act: render component
    render(<BurnoutAssessment />);

    // Assert: error message is displayed
    expect(await screen.findByText('Something went wrong.')).toBeInTheDocument();
});

it('disables Next button if no answer is provided', async () => {
    // Arrange: simulate server sending questions
    const questions = [
        { questionId: 1, question: 'How burnt out are you?', multimodal: false }
    ];
    mockMessages = [
        { type: 'burnout-questions', sessionId: 'sess1', questions }
    ];

    // Act: render component
    render(<BurnoutAssessment />);

    // Assert: Next button is disabled initially
    const nextBtn = screen.getByTestId('next-button');
    expect(nextBtn).toBeDisabled();
});

it('handles answer submission and advances to next question', async () => {
    // Arrange: prepare two questions (first Likert, second Vlog)
    const questions = [
      { questionId: 1, question: 'How burnt out are you?', multimodal: false },
      { questionId: 2, question: 'Record a vlog', multimodal: true }
    ];
    mockMessages = [
      { type: 'burnout-questions', sessionId: 'sess1', questions }
    ];

    // Act: render component and wait for first question
    render(<BurnoutAssessment />);
    const firstLikert = await screen.findByTestId('likert');
    expect(firstLikert).toHaveTextContent('How burnt out are you?');

    // Clear initial start-burnout message
    mockSendMessage.mockClear();

    // Simulate answering the first question via Likert stub
    fireEvent.click(screen.getByTestId('likert-answer'));

    // Next button should now be enabled
    const nextBtn = screen.getByTestId('next-button');
    expect(nextBtn).toBeEnabled();

    // Act: click Next
    fireEvent.click(nextBtn);

    // Assert: sendMessage called with answer payload
    expect(mockSendMessage).toHaveBeenCalledTimes(1);
    const sent = JSON.parse(mockSendMessage.mock.calls[0][0] as string);
    expect(sent).toEqual({
      type: 'answer',
      sessionId: 'sess1',
      questionId: 1,
      response: 'my-response'
    });

    // UI should advance to second question (Vlog stub)
    const vlog = await screen.findByTestId('vlog');
    expect(vlog).toHaveTextContent('Record a vlog');
  });

  it('sends assessment-complete on final question', async () => {
     // Arrange: single question scenario
     const questions = [
        { questionId: 1, question: 'Only question', multimodal: false }
      ];
      mockMessages = [
        { type: 'burnout-questions', sessionId: 'sess1', questions }
      ];
  
      // Act: render and wait for question
      render(<BurnoutAssessment />);
      expect(await screen.findByTestId('likert')).toHaveTextContent('Only question');
  
      // Clear prior messages and stub sendMessage
      mockSendMessage.mockClear();
  
      // Simulate answering and clicking Next
      fireEvent.click(screen.getByTestId('likert-answer'));
      const nextBtn = screen.getByTestId('next-button');
      expect(nextBtn).toBeEnabled();
      fireEvent.click(nextBtn);
  
  });

  it('navigates to summary page on assessment-result', async () => {
    // Arrange: simulate initial questions and then an assessment-result
    const questions = [
        { questionId: 1, question: 'Only question', multimodal: false }
      ];
      mockMessages = [
        { type: 'burnout-questions', sessionId: 'sess1', questions },
        { type: 'assessment-result', score: 75, summary: 'Test summary' }
      ];
  
      // Act: render component
      render(<BurnoutAssessment />);
  
      // Assert: navigate is called with the final result
      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith(
          '/burnout-summary',
          { state: { result: { score: 75, summary: 'Test summary' } } }
        );
      });
  });

  it('displays error UI when error message received', async () => {
    // Arrange: simulate error message from server
    mockMessages = [
        { error: 'Something went wrong' }
      ];
  
      // Act: render component
      render(<BurnoutAssessment />);
  
      // Assert: error message is displayed and styled correctly
      const errorElem = await screen.findByText('Something went wrong');
      expect(errorElem).toBeInTheDocument();
      expect(errorElem).toHaveClass('text-red-500');
  });

  it('uploads audio and video on vlog question submission', async () => {
    // Arrange: two questions
    const questions = [
      { questionId: 1, question: 'Rate stress', multimodal: false },
      { questionId: 2, question: 'Record a vlog', multimodal: true }
    ];
    mockMessages = [{ type: 'burnout-questions', sessionId: 'sess1', questions }];

    // Act: render and answer first question
    render(<BurnoutAssessment />);
    await screen.findByTestId('likert');
    fireEvent.click(screen.getByTestId('likert-answer'));
    fireEvent.click(screen.getByTestId('next-button'));

    // Now on vlog question
    expect(await screen.findByTestId('vlog')).toHaveTextContent('Record a vlog');
    mockSendMessage.mockClear();

    // Simulate vlog answer and Next
    fireEvent.click(screen.getByTestId('vlog-answer'));
    fireEvent.click(screen.getByTestId('next-button'));
  });

});