import '@testing-library/jest-dom';
/// <reference types="vitest" />
import { useEffect} from 'react';
import { render, screen, act, } from '@testing-library/react';
import { useWebSocket } from './useWebSocket';
import { describe, it, expect, vi, beforeEach, type Mock  } from 'vitest';

// Holder for hook return values
declare global {
  interface Window { __ws: any; }
}

interface Message {
  type: string;
  text?: string;
  audio?: string;
}

// Stub global.WebSocket before each test
beforeEach(() => {
  // Clear localStorage
  localStorage.clear();

  // Stub WebSocket constructor
  let socketInstance: any;
  global.WebSocket = class {
    static OPEN = 1;
    readyState = 1;
    binaryType = '';
    send = vi.fn().mockImplementation((data: any) => socketInstance?.sent.push(data));
    close = vi.fn();
    onopen: () => void = () => {};
    onmessage: (ev: any) => void = () => {};
    onerror: (ev: any) => void = () => {};
    onclose: (ev: any) => void = () => {};
    sent: any[] = [];
    constructor(public url: string) {
      socketInstance = this;
      socketInstance.sent = [];
      // simulate open on next tick
      setTimeout(() => this.onopen(), 0);
    }
  } as any;

  // Clear any previous window hook state
  delete (window as any).__ws;
});

// Test wrapper component that uses the hook
const TestComponent = ({ url, connect }: { url: string; connect: boolean }) => {
  const { isConnected, messages, sendMessage } = useWebSocket(url, connect);
  useEffect(() => {
    (window as any).__ws = { isConnected, messages, sendMessage, socket: WebSocket };
  }, [isConnected, messages, sendMessage]);
  return (
    <>
      <div data-testid="status">{isConnected ? 'connected' : 'disconnected'}</div>
      <div>
        {messages.map((m: Message, i: number) => (
          <div key={i} data-testid="msg">
            {JSON.stringify(m)}
          </div>
        ))}
      </div>
    </>
  );

};

// Helper to get the WebSocket instance from window
const getHook = () => (window as any).__ws;

// ------------------ Tests ------------------

describe('useWebSocket (manual wrapper)', () => {
  
  let originalWebSocket: any;

  beforeEach(() => {
    // Stub global.WebSocket
    originalWebSocket = global.WebSocket;
    interface WSMockType {
      url: string;
      binaryType: string;
      close: Mock;
    }

    const WSMock = vi.fn(function (this: WSMockType, url: string) {
      this.url = url;
      this.binaryType = '';
      this.close = vi.fn();
    });
    global.WebSocket = WSMock as any;

    localStorage.clear();
  });

  afterEach(() => {
    // Restore real WebSocket
    global.WebSocket = originalWebSocket;
    vi.resetAllMocks();
  });

  // Tests to set up websocket without token
  it('appends token when present in localStorage', () => {
    localStorage.setItem('token', 'abc123');
    render(<TestComponent url="ws://example.com/socket" connect={true} />);
    expect(global.WebSocket).toHaveBeenCalledWith(
      'ws://example.com/socket?token=abc123'
    );
    const instance = (global.WebSocket as any).mock.instances[0];
    expect(instance.binaryType).toBe('arraybuffer');
  });

  it('uses the plain URL when no token exists', () => {
    render(<TestComponent url="ws://example.com/socket" connect={true} />);
    expect(global.WebSocket).toHaveBeenCalledWith('ws://example.com/socket');
  });

  // Test on open
  it('starts disconnected, then becomes connected when socket.onopen is called', () => {
    render(<TestComponent url="ws://example.com/ws" connect={true} />);

    // Initially, the status should be "disconnected"
    expect(screen.getByTestId('status')).toHaveTextContent('disconnected');

    // Grab the mock instance and simulate onopen
    const instance = (global.WebSocket as any).mock.instances[0];
    act(() => {
      instance.onopen();
    });

    // Now the status should update
    expect(screen.getByTestId('status')).toHaveTextContent('connected');
  });

  // Test on message
  it('parses and appends a regular text message', () => {
    render(<TestComponent url="ws://test" connect={true} />);
    const instance = (global.WebSocket as any).mock.instances[0];

    act(() => {
      instance.onmessage({ data: JSON.stringify({ foo: 'bar', type: 'text' }) });
    });

    const msg = screen.getByTestId('msg');
    expect(msg).toHaveTextContent(
      JSON.stringify({ foo: 'bar', type: 'text' })
    );
  });

  it('detects AWS error and appends fallback audio message', () => {
    render(<TestComponent url="ws://test" connect={true} />);
    const instance = (global.WebSocket as any).mock.instances[0];

    act(() => {
      instance.onmessage({ data: JSON.stringify({ error: 'oops', code: 500 }) });
    });

    const msg = screen.getByTestId('msg');
    const expected = {
      type: 'audio',
      text:
        'Sorry, something went wrong while processing your audio. Please try again later.',
    };
    expect(msg).toHaveTextContent(JSON.stringify(expected));
  });

  it('creates an audio Blob of type "audio/mpeg" and appends the audio message', () => {
    // Stub URL.createObjectURL
    const createObjURLSpy = vi.fn(() => 'blob://audio-url');
    const originalCreateObjURL = URL.createObjectURL;
    URL.createObjectURL = createObjURLSpy;
  
    // Render and grab the WebSocket instance
    render(<TestComponent url="ws://example.com/ws" connect={true} />);
    const instance = (global.WebSocket as any).mock.instances[0];
  
    // Fire the onmessage handler with our buffer
    const buffer = new Uint8Array([1, 2, 3]).buffer;
    act(() => instance.onmessage({ data: buffer }));
  
    // 1) Verify it was called exactly once
    expect(createObjURLSpy).toHaveBeenCalledTimes(1);
  
    // 2) Destructure the first call’s first arg
    const [ [ blobArg ] ] = createObjURLSpy.mock.calls as unknown as [ [ unknown ] , ...unknown[] ][];
    // Now blobArg is whatever was passed to createObjectURL
  
    // 3) Assert it’s a Blob of the right type
    expect(blobArg).toBeInstanceOf(Blob);
    expect(((blobArg as unknown) as Blob).type).toBe('audio/mpeg');
  
    // Restore
    URL.createObjectURL = originalCreateObjURL;
  });
  

  // Test on error
  it('appends a system error message when socket.onerror is called', () => {
    // 1. Stub console.error to avoid noisy logs
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
  
    // 2. Render and get the WebSocket instance
    render(<TestComponent url="ws://example.com/ws" connect={true} />);
    const instance = (global.WebSocket as any).mock.instances[0];
  
    // 3. Simulate an error event
    act(() => {
      instance.onerror(new Event('error') as any);
    });
  
    // 4. There should now be exactly one message rendered
    const msgs = screen.getAllByTestId('msg');
    expect(msgs).toHaveLength(1);
  
    // 5. Assert its contents
    const expected = {
      type: 'system',
      text: 'Connection error occurred. Please check your connection.',
    };
    expect(msgs[0]).toHaveTextContent(JSON.stringify(expected));
  
    // 6. Restore console.error
    consoleErrorSpy.mockRestore();
  });

  // Test on close
  it('handles socket.onclose by marking disconnected and appending a system-close message', () => {
    // mute the console.log call
    const consoleLogSpy = vi.spyOn(console, 'log').mockImplementation(() => {});
  
    // render and grab the fake socket instance
    render(<TestComponent url="ws://example.com/ws" connect={true} />);
    const instance = (global.WebSocket as any).mock.instances[0];
  
    // fire the close event
    act(() => {
      instance.onclose({ code: 1006, reason: 'Network lost' } as CloseEvent);
    });
  
    // the status div should now say "disconnected"
    expect(screen.getByTestId('status')).toHaveTextContent('disconnected');
  
    // and the single msg div should contain your system-close JSON
    expect(screen.getByTestId('msg')).toHaveTextContent(
      JSON.stringify({
        type: 'system',
        text: 'Connection closed. Please try reconnecting.',
      })
    );
  
    consoleLogSpy.mockRestore();
  });
  
  // Test sendMessage
  it('calls socket.send when socket is open', () => {
    const consoleLogSpy = vi.spyOn(console, 'log').mockImplementation(() => {});
    render(<TestComponent url="ws://example.com/ws" connect={true} />);
    const wsInstance = (global.WebSocket as any).mock.instances[0];
    wsInstance.send = vi.fn(); // Mock the send method
    // ensure it appears “open”
    wsInstance.readyState = WebSocket.OPEN;
  
    const { sendMessage } = getHook();
    const payload = { foo: 'bar' };
  
    act(() => {
      sendMessage(payload);
    });
  
    expect(consoleLogSpy).toHaveBeenCalledWith(
      '[useWebSocket] sendMessage called with data:',
      payload
    );
    expect(wsInstance.send).toHaveBeenCalledWith(payload);
  
    consoleLogSpy.mockRestore();
  });

});
