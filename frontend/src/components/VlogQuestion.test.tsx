import '@testing-library/jest-dom';
/// <reference types="vitest" />
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, type Mock } from 'vitest';
import VlogQuestion from './VlogQuestion';
import type { BurnoutQuestion } from '../types/burnout/assessment';

// Stub MediaStreamTrack so new MediaStreamTrack() works
vi.stubGlobal('MediaStreamTrack', class {
  kind: string;
  constructor(kind: string = 'video') {
    this.kind = kind;
  }
} as any);

vi.stubGlobal('MediaStream', class {
    constructor() {}
    getVideoTracks() { return []; }
    getAudioTracks() { return []; }
  } as any);

// Stub navigator.mediaDevices.getUserMedia
Object.defineProperty(navigator, 'mediaDevices', {
    writable: true,
    value: {
      getUserMedia: vi.fn().mockResolvedValue({
        getVideoTracks: () => [{ kind: 'video', stop: vi.fn() }],
      getAudioTracks: () => [{ kind: 'audio', stop: vi.fn() }],
        getTracks: function() { return [...this.getVideoTracks(), ...this.getAudioTracks()]; }
      }),
    },
  });

// Capture recorder instances
const recorders: any[] = [];
// Stub MediaRecorder
vi.stubGlobal('MediaRecorder', class {
  ondataavailable: ((e: any) => void) | null = null;
  onstop: (() => void) | null = null;
  state = 'inactive';
  constructor() {
    recorders.push(this);
  }
  start = vi.fn();
  stop = vi.fn();
} as any);

// Stub URL.createObjectURL
vi.stubGlobal('URL', {
  ...URL,
  createObjectURL: vi.fn(() => 'blob:url'),
} as any);

// Stub video.play to avoid errors
HTMLMediaElement.prototype.play = vi.fn().mockResolvedValue(undefined);

describe('VlogQuestion Component', () => {
  beforeEach(() => {
    recorders.length = 0;
    (navigator.mediaDevices.getUserMedia as Mock).mockClear();
  });

  it('records and submits vlog correctly', async () => {
    const onChange = vi.fn();
    const question: BurnoutQuestion = { questionId: 5, question: 'Record yourself', multimodal: true, domain: 'general' };

    render(<VlogQuestion question={question} onChange={onChange} />);

    // Wait for getUserMedia call and initial Record button
    await waitFor(() => expect(navigator.mediaDevices.getUserMedia).toHaveBeenCalled());
    const recordBtn = screen.getByRole('button', { name: /record video/i });
    expect(recordBtn).toBeEnabled();

    // Start recording
    fireEvent.click(recordBtn);
    expect(recordBtn).toHaveTextContent(/stop recording/i);
    // start called on each of the three recorders
    await waitFor(() => expect(recorders.length).toBe(3));
    recorders.forEach(rec => expect(rec.start).toHaveBeenCalled());

    // Stop recording
    fireEvent.click(recordBtn);
    recorders.forEach(rec => expect(rec.stop).toHaveBeenCalled());

    // Simulate stop events to generate URLs
    const [videoRec, audioRec, fullRec] = recorders;
    act(() => videoRec.onstop && videoRec.onstop());
    act(() => audioRec.onstop && audioRec.onstop());
    act(() => fullRec.onstop && fullRec.onstop());

    // Now submit button should appear
    const submitBtn = await screen.findByRole('button', { name: /submit recording/i });
    expect(submitBtn).toBeInTheDocument();

    // Submit recording
    fireEvent.click(submitBtn);
    expect(onChange).toHaveBeenCalledWith(
      5,
      JSON.stringify({ videoUrl: 'blob:url', audioUrl: 'blob:url' })
    );
  });
});
