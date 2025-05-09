import { useEffect, useRef } from 'react';

export const useCBTWebSocket = () => {
    const socketRef = useRef<WebSocket | null>(null);

    useEffect(() => {
        const socket = new WebSocket('ws://localhost:8080/ws/cbt');
        socketRef.current = socket;

        socket.onopen = () => {
            console.log('Connected to CBT WebSocket');
            setTimeout(() => {
                if (socket.readyState === WebSocket.OPEN) {
                    socket.send(JSON.stringify({
                        type: "text",
                        text: "Hello, CBT session is ready!",
                        requestId: "req-123" // this can be any unique string
                    }));
                } else {
                    console.warn('WebSocket not open, message not sent');
                }
            }, 250); // delay to ensure backend is ready
        };

        socket.onmessage = (event) => {
            console.log('Server message:', event.data);
        };

        socket.onerror = (error) => {
            console.error('WebSocket error:', error);
        };

        socket.onclose = () => {
            console.log('WebSocket disconnected');
        };

        return () => {
            socket.close();
        };
    }, []);

    return socketRef;
};