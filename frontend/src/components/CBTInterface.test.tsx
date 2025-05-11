import '@testing-library/jest-dom';
/// <reference types="vitest" />
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeAll, beforeEach, type Mock } from 'vitest';
import CBTInterface from './CBTInterface';

// ─── GLOBAL STUBS ─────────────────────────────────────────────────────────
// Polyfill MediaStream
vi.stubGlobal('MediaStream', class {
  private _tracks: any[];
  constructor(tracks: any[] = []) { this._tracks = tracks; }
  getVideoTracks()  { return this._tracks.filter(t => t.kind === 'video'); }
  getAudioTracks()  { return this._tracks.filter(t => t.kind === 'audio'); }
  getTracks()       { return this._tracks; }
} as any);

// Polyfill MediaStreamTrack
vi.stubGlobal('MediaStreamTrack', class {
  kind: string;
  enabled: boolean;
  stop: Mock;
  addEventListener: Mock;
  removeEventListener: Mock;
  dispatchEvent: Mock;
  constructor(kind: string = 'audio') {
    this.kind = kind;
    this.enabled = true;
    this.stop = vi.fn();
    this.addEventListener = vi.fn();
    this.removeEventListener = vi.fn();
    this.dispatchEvent = vi.fn();
  }
} as any);

// Polyfill MediaStreamTrack
vi.stubGlobal('MediaStreamTrack', class {
  kind: string;
  enabled: boolean;
  stop: Mock;
  addEventListener: Mock;
  removeEventListener: Mock;
  dispatchEvent: Mock;
  constructor(kind: string = 'audio') {
    this.kind = kind;
    this.enabled = true;
    this.stop = vi.fn();
    this.addEventListener = vi.fn();
    this.removeEventListener = vi.fn();
    this.dispatchEvent = vi.fn();
  }
} as any);

// Polyfill video.srcObject
Object.defineProperty(window.HTMLMediaElement.prototype, 'srcObject', {
  configurable: true,
  get(this: any) { return this._srcObject; },
  set(this: any, val) { this._srcObject = val; }
});

// Stub Audio
vi.stubGlobal('Audio', vi.fn().mockImplementation((src: string) => ({
  src,
  play: vi.fn().mockResolvedValue(undefined),
  pause: vi.fn(),
  onended: null as (() => void) | null
})));

// Stub MediaRecorder
// Capture all recorder instances here
const createdRecorders: any[] = [];
beforeEach(() => {
    createdRecorders.length = 0;   // clear out any previously created recorders
    mockMessages = [];             // you may already have this
    mockSendMessage.mockClear();
    (navigator.mediaDevices.getUserMedia as Mock).mockClear();
    (Audio as unknown as Mock).mockClear();
  });

vi.stubGlobal('MediaRecorder', 
  class {
    static isTypeSupported = (_: string) => true;
    state = 'inactive';
    start = vi.fn();
    stop  = vi.fn();
    ondataavailable: ((e: any) => void) | null = null;
    onstop: (() => void) | null = null;
    constructor() {
      createdRecorders.push(this);
    }
  } as any
);

// ─── MOCKS ────────────────────────────────────────────────────────────────

vi.mock('hark', () => ({ default: () => ({ on: vi.fn(), stop: vi.fn() }) }));
const mockSendMessage = vi.fn();
let mockMessages: Array<{ type: string; text?: string; audio?: string }> = [];
vi.mock('../hooks/useWebSocket', () => ({
  useWebSocket: () => ({ messages: mockMessages, sendMessage: mockSendMessage })
}));

// ─── TEST SUITE ────────────────────────────────────────────────────────────
describe('CBTInterface Component', () => {
  beforeAll(() => {
    // Stub getUserMedia
    Object.defineProperty(navigator, 'mediaDevices', {
      value: { getUserMedia: vi.fn() }, writable: true
    });
    // Stub play()
    HTMLMediaElement.prototype.play = vi.fn().mockResolvedValue(undefined);
  });

  beforeEach(() => {
    mockMessages = [];
    mockSendMessage.mockClear();
    (navigator.mediaDevices.getUserMedia as Mock).mockClear();
    (Audio as unknown as Mock).mockClear();
  });

  it('renders initial UI', () => {
    render(<CBTInterface />);
    expect(screen.getByText('Hello, how can I help you today?')).toBeInTheDocument();
    expect(screen.getByText('Begin Session')).toBeInTheDocument();
  });

  it('starts and stops session correctly', async () => {
    const audioTrack = { kind: 'audio', enabled: true, stop: vi.fn(), addEventListener: vi.fn(), removeEventListener: vi.fn(), dispatchEvent: vi.fn() } as unknown as MediaStreamTrack;
    const videoTrack = { kind: 'video', enabled: true, stop: vi.fn(), addEventListener: vi.fn(), removeEventListener: vi.fn(), dispatchEvent: vi.fn() } as unknown as MediaStreamTrack;
    const dummyStream = new MediaStream([audioTrack, videoTrack]);
    (navigator.mediaDevices.getUserMedia as Mock).mockResolvedValueOnce(dummyStream);

    const { container } = render(<CBTInterface />);
    fireEvent.click(screen.getByText('Begin Session'));
    await waitFor(() => screen.getByText('End Session'));

    // Attach stream to video element to simulate playback
    const videoElem = container.querySelector('video') as HTMLVideoElement;
    act(() => { videoElem.srcObject = dummyStream; });
    expect((videoElem.srcObject as any).getVideoTracks()[0]).toBe(videoTrack);

    fireEvent.click(screen.getByText('End Session'));
    expect(audioTrack.stop).toHaveBeenCalled();
    expect(videoTrack.stop).toHaveBeenCalled();
    expect(screen.getByText('Begin Session')).toBeInTheDocument();
  });

  // --- Process Incoming Messages ---
  it('appends a new user message on input-transcription', async () => {
    mockMessages = [{ type: 'input-transcription', text: 'Hello from server' }];
    render(<CBTInterface />);
    expect(await screen.findByText('Hello from server')).toBeInTheDocument();
  });

  /* it('only adds one pending message even if onstop fires multiple times', async () => {
    // 1) Stub getUserMedia → dummyStream
    const audioTrack = Object.assign(new MediaStreamTrack(), { kind: 'audio', enabled: true, stop: vi.fn() });
    const videoTrack = Object.assign(new MediaStreamTrack(), { kind: 'video', enabled: true, stop: vi.fn() });
    const dummyStream = new MediaStream([audioTrack, videoTrack]);
    (navigator.mediaDevices.getUserMedia as Mock).mockResolvedValueOnce(dummyStream);
  
    // 2) Render and kick off startSession
    render(<CBTInterface />);
    fireEvent.click(screen.getByText('Begin Session'));
  
    // 3) Wait for your MediaRecorder stub to be called
    await waitFor(() => {
      expect(MediaRecorder).toHaveBeenCalled();
    });
  
    // 4) Grab the recorder instance
    const recorder = (MediaRecorder as unknown as vi.Mock).mock.results[0].value;
  
    // 5) First onstop → should add one “Transcribing…” bubble
    act(() => recorder.onstop!());
    expect(screen.getByText('Transcribing your message...')).toBeInTheDocument();
  
    // 6) Second onstop (still awaiting) → should NOT add another
    act(() => recorder.onstop!());
    const pendings = screen.getAllByText('Transcribing your message...');
    expect(pendings).toHaveLength(1);
  }); */

/* it('clears TTS audio when a new audio message is received', async () => {
    const mockAudioInstance = (Audio as unknown as vi.Mock).mock.results[0]?.value;
    mockMessages = [{ type: 'audio', audio: 'blob:fake-123' }];
    render(<CBTInterface />);
    await waitFor(() => expect(Audio).toHaveBeenCalledWith('blob:fake-123'));
    expect(mockAudioInstance.pause).toHaveBeenCalled();
    expect(mockAudioInstance.src).toBe('');
}); */

it('handles unrecognized message types gracefully', async () => {
    mockMessages = [{ type: 'unknown-type', text: 'Unexpected message' }];
    render(<CBTInterface />);
    expect(screen.queryByText('Unexpected message')).toBeNull();
});

it('scrolls chat to the bottom when a new message is added', async () => {
    const { container } = render(<CBTInterface />);
    const chatContainer = container.querySelector('.chat-container') as HTMLDivElement;

    // Mock scroll behavior
    const scrollSpy = vi.spyOn(chatContainer, 'scrollTop', 'set');
    mockMessages = [{ type: 'input-transcription', text: 'New message' }];
    await waitFor(() => expect(scrollSpy).toHaveBeenCalled());
});

it('enables mic and camera buttons when session is active', async () => {
    const audioTrack = new MediaStreamTrack() as unknown as MediaStreamTrack;
    Object.assign(audioTrack, { kind: 'audio', enabled: true, stop: vi.fn() });

    const videoTrack = new MediaStreamTrack() as unknown as MediaStreamTrack;
    Object.assign(videoTrack, { kind: 'video', enabled: true, stop: vi.fn() });

    const dummyStream = new MediaStream([audioTrack, videoTrack]);
    (navigator.mediaDevices.getUserMedia as Mock).mockResolvedValueOnce(dummyStream);

    render(<CBTInterface />);
    fireEvent.click(screen.getByText('Begin Session'));
    await waitFor(() => screen.getByText('End Session'));

    const micBtn = screen.getByTestId('mic-toggle');
    const camBtn = screen.getByTestId('cam-toggle');
    expect(micBtn).not.toBeDisabled();
    expect(camBtn).not.toBeDisabled();
});

/* it('handles errors when accessing media devices', async () => {
    (navigator.mediaDevices.getUserMedia as Mock).mockRejectedValueOnce(new Error('Permission denied'));

    render(<CBTInterface />);
    fireEvent.click(screen.getByText('Begin Session'));
    await waitFor(() => expect(screen.getByText('Begin Session')).toBeInTheDocument());
    expect(screen.queryByText('End Session')).toBeNull();
}); */

it('stops all media tracks when session ends', async () => {
    const audioTrack = Object.assign(new MediaStreamTrack(), { kind: 'audio', enabled: true, stop: vi.fn() });
    const videoTrack = Object.assign(new MediaStreamTrack(), { kind: 'video', enabled: true, stop: vi.fn() });
    const dummyStream = new MediaStream([audioTrack, videoTrack]);
    (navigator.mediaDevices.getUserMedia as Mock).mockResolvedValueOnce(dummyStream);

    render(<CBTInterface />);
    fireEvent.click(screen.getByText('Begin Session'));
    await waitFor(() => screen.getByText('End Session'));

    fireEvent.click(screen.getByText('End Session'));
    expect(audioTrack.stop).toHaveBeenCalled();
    expect(videoTrack.stop).toHaveBeenCalled();
});

  it('appends assistant message on output-transcription', async () => {
    mockMessages = [{ type: 'output-transcription', text: 'Here is your reply' }];
    render(<CBTInterface />);
    expect(await screen.findByText('Here is your reply')).toBeInTheDocument();
  });

  it('plays audio on audio message', async () => {
    mockMessages = [{ type: 'audio', audio: 'blob:fake-123' }];
    render(<CBTInterface />);
    await waitFor(() => expect(Audio).toHaveBeenCalledWith('blob:fake-123'));
    const inst = (Audio as unknown as Mock).mock.results[0].value;
    expect(inst.play).toHaveBeenCalled();
  });

  // --- Controls ---
  it('toggles mic mute/unmute', async () => {
    const audioTrack = new MediaStreamTrack() as unknown as MediaStreamTrack;
    Object.assign(audioTrack, { kind: 'audio', enabled: true, stop: vi.fn() });

    const videoTrack = new MediaStreamTrack() as unknown as MediaStreamTrack;
    Object.assign(videoTrack, { kind: 'video', enabled: true, stop: vi.fn() });

    const dummyStream = new MediaStream([audioTrack, videoTrack]);
    (navigator.mediaDevices.getUserMedia as Mock).mockResolvedValueOnce(dummyStream);

    render(<CBTInterface />);
    fireEvent.click(screen.getByText('Begin Session'));
    await waitFor(() => screen.getByText('End Session'));

    const micBtn = screen.getByTestId('mic-toggle');
    expect(audioTrack.enabled).toBe(true);
    expect(micBtn).toHaveClass('bg-neutral');

    fireEvent.click(micBtn);
    expect(audioTrack.enabled).toBe(false);
    expect(micBtn).toHaveClass('bg-warning');

    fireEvent.click(micBtn);
    expect(audioTrack.enabled).toBe(true);
    expect(micBtn).toHaveClass('bg-neutral');
  });

  it('toggles camera on/off', async () => {
    const audioTrack = Object.assign(new MediaStreamTrack(), { kind: 'audio', enabled: true, stop: vi.fn() });
    const videoTrack = Object.assign(new MediaStreamTrack(), { kind: 'video', enabled: true, stop: vi.fn() });
    const dummyStream = new MediaStream([audioTrack, videoTrack]);
    (navigator.mediaDevices.getUserMedia as Mock).mockResolvedValueOnce(dummyStream);

    render(<CBTInterface />);
    fireEvent.click(screen.getByText('Begin Session'));
    await waitFor(() => screen.getByText('End Session'));

    const camBtn = screen.getByTestId('cam-toggle');
    expect(videoTrack.enabled).toBe(true);
    expect(camBtn).toHaveClass('bg-neutral');

    fireEvent.click(camBtn);
    expect(videoTrack.enabled).toBe(false);
    expect(camBtn).toHaveClass('bg-warning');

    fireEvent.click(camBtn);
    expect(videoTrack.enabled).toBe(true);
    expect(camBtn).toHaveClass('bg-neutral');
  });

  it('cleans up and resets on stop session', async () => {
    const audioTrack = Object.assign(new MediaStreamTrack(), { kind: 'audio', enabled: true, stop: vi.fn() });
    const videoTrack = Object.assign(new MediaStreamTrack(), { kind: 'video', enabled: true, stop: vi.fn() });
    const dummyStream = new MediaStream([audioTrack, videoTrack]);
    (navigator.mediaDevices.getUserMedia as Mock).mockResolvedValueOnce(dummyStream);

    const { container } = render(<CBTInterface />);
    fireEvent.click(screen.getByText('Begin Session'));
    await waitFor(() => screen.getByText('End Session'));

    const videoElem = container.querySelector('video') as HTMLVideoElement;
    act(() => { videoElem.srcObject = dummyStream; });
    const pauseSpy = vi.spyOn(videoElem, 'pause');

    fireEvent.click(screen.getByText('End Session'));
    expect(audioTrack.stop).toHaveBeenCalled();
    expect(videoTrack.stop).toHaveBeenCalled();
    expect(pauseSpy).toHaveBeenCalled();
    expect(videoElem.srcObject).toBeNull();
    expect(screen.getByText('Begin Session')).toBeInTheDocument();

    const micBtn = screen.getByTestId('mic-toggle');
    const camBtn = screen.getByTestId('cam-toggle');
    expect(micBtn).toHaveClass('bg-neutral');
    expect(camBtn).toHaveClass('bg-warning');
  });

});
