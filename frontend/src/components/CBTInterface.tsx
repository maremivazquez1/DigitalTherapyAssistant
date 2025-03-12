import React, { useState, useEffect, useRef } from "react";
import therapyRoom from "../assets/therapy-room-1.svg";
import { FaMicrophone, FaVideo } from "react-icons/fa";

interface ChatMessage {
  id: number;
  sender: string;
  message: string;
  timestamp: Date;
}

const CBTInterface: React.FC = () => {
  // Initialize with an initial AI message
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([
    {
      id: Date.now(),
      sender: "AI",
      message: "Hello, how can I help you today?",
      timestamp: new Date(),
    },
  ]);

  // Create a ref for the chat container
  const chatContainerRef = useRef<HTMLDivElement>(null);

  // Automatically scroll to the bottom when chatMessages change
  useEffect(() => {
    if (chatContainerRef.current) {
      chatContainerRef.current.scrollTop =
        chatContainerRef.current.scrollHeight;
    }
  }, [chatMessages]);

  // Simulated function for handling mic input
  const handleMicClick = async () => {
    const userMessage: ChatMessage = {
      id: Date.now(),
      sender: "User",
      message: "User's captured audio message (simulated)",
      timestamp: new Date(),
    };
    setChatMessages((prev) => [...prev, userMessage]);

    // Simulate a delayed API response
    setTimeout(() => {
      const aiResponse: ChatMessage = {
        id: Date.now() + 1,
        sender: "AI",
        message: "This is the AI's response (simulated)",
        timestamp: new Date(),
      };
      setChatMessages((prev) => [...prev, aiResponse]);
    }, 1500);
  };

  // Simulated function for handling camera input
  const handleCamClick = async () => {
    console.log("Camera button clicked");
  };

  return (
    <div
      className="hero h-screen relative bg-base-100"
      style={{ backgroundImage: `url(${therapyRoom})`, backgroundSize: "cover" }}
    >
      {/* Semi-transparent overlay using base-200 */}
      <div className="hero-overlay bg-neutral opacity-75"></div>

      {/* Chat container centered and spanning 80% of the viewport width */}
      <div className="absolute top-0 left-0 w-full h-full flex items-center justify-center">
        <div className="w-4/5">
          <div
            ref={chatContainerRef}
            className="chat-container max-h-[60vh] overflow-y-auto px-4"
          >
            {chatMessages.map((msg) => (
              <div
                key={msg.id}
                className={`chat mb-4 ${
                  msg.sender === "User" ? "chat-end" : "chat-start"
                }`}
              >
                <div className="chat-header flex justify-between items-center">
                  <span>{msg.sender}</span>
                  <time className="text-xs opacity-50">
                    {msg.timestamp.toLocaleTimeString()}
                  </time>
                </div>
                {/* Added extra padding, shadow, and a semi-transparent background */}
                <div
                  className={`chat-bubble p-3 shadow-md rounded-full ${
                    msg.sender === "User"
                      ? "bg-base-300 bg-opacity-80 text-primary-content"
                      : "bg-base-300 bg-opacity-80 text-secondary-content"
                  }`}
                >
                  {msg.message}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Center bottom controls for mic and video */}
      <div className="absolute bottom-5 left-1/2 transform -translate-x-1/2 flex items-center gap-4">
        <button
          onClick={handleMicClick}
          className="btn btn-circle btn-sm bg-neutral hover:bg-neutral-focus"
        >
          <FaMicrophone className="text-neutral-content" />
        </button>
        <button
          onClick={handleCamClick}
          className="btn btn-circle btn-sm bg-neutral hover:bg-neutral-focus"
        >
          <FaVideo className="text-neutral-content" />
        </button>
      </div>

      {/* Avatar positioned at the bottom right using the primary border color */}
      <div className="absolute bottom-5 right-5 avatar">
        <div className="w-38 rounded border-4 border-neutral">
          <img
            src="https://img.daisyui.com/images/stock/photo-1534528741775-53994a69daeb.webp"
            alt="User Avatar"
          />
        </div>
      </div>
    </div>
  );
};

export default CBTInterface;
