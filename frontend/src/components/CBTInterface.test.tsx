import '@testing-library/jest-dom';  
/// <reference types="vitest" />
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeAll } from 'vitest';
import CBTInterface from './CBTInterface';

// Mock the WebSocket hook
vi.mock('../hooks/useWebSocket', () => ({
  useWebSocket: () => ({
    messages: [],
    sendMessage: vi.fn(),
  }),
}));

// Mock hark for speech detection
vi.mock('hark', () => ({
  default: () => ({
    on: vi.fn(),
    stop: vi.fn(),
  }),
}));

describe('CBTInterface Component', () => {
  beforeAll(() => {
    // Mock getUserMedia
    Object.defineProperty(navigator, 'mediaDevices', {
      value: {
        getUserMedia: vi.fn(),
      },
      writable: true,
    });
    // Mock HTMLMediaElement.play()
    HTMLMediaElement.prototype.play = vi.fn().mockResolvedValue(undefined);
  });

  it('renders initial greeting and Begin Session button', () => {
    render(<CBTInterface />);
    expect(
      screen.getByText('Hello, how can I help you today?')
    ).toBeInTheDocument();
    expect(screen.getByText('Begin Session')).toBeInTheDocument();
  });

  it('starts and stops session correctly', async () => {
    // Prepare a dummy media stream
    const audioTrack = { kind: 'audio', enabled: true, stop: vi.fn() };
    const videoTrack = { kind: 'video', enabled: true, stop: vi.fn() };
    const dummyStream = {
      getAudioTracks: () => [audioTrack],
      getVideoTracks: () => [videoTrack],
    } as unknown as MediaStream;

    // Mock successful getUserMedia call
    (navigator.mediaDevices.getUserMedia as vi.Mock).mockResolvedValueOnce(
      dummyStream
    );

    const { container } = render(<CBTInterface />);

    // Start session
    fireEvent.click(screen.getByText('Begin Session'));

    // Wait for End Session button to appear
    await waitFor(() => {
      expect(screen.getByText('End Session')).toBeInTheDocument();
    });

    // Ensure getUserMedia was called with correct constraints
    expect(navigator.mediaDevices.getUserMedia).toHaveBeenCalledWith({
      audio: true,
      video: true,
    });

    // Check that the video element received the stream
    const videoElem = container.querySelector('video');
    expect(videoElem).toBeTruthy();
    const src = (videoElem as HTMLVideoElement).srcObject;
    expect(src).toBeInstanceOf(MediaStream);
    expect((src as MediaStream).getVideoTracks()[0]).toBe(videoTrack);

    // Stop session
    fireEvent.click(screen.getByText('End Session'));

    // Audio and video tracks should be stopped
    expect(audioTrack.stop).toHaveBeenCalled();
    expect(videoTrack.stop).toHaveBeenCalled();

    // Begin Session button should be back
    expect(screen.getByText('Begin Session')).toBeInTheDocument();
  });
});
