import React from "react";
import therapyRoom from "../assets/therapy-room-1.svg";

const CBTSession: React.FC = () => {
  return (
    <div className="flex items-center justify-center w-full h-screen bg-gray-900">
      {/* Video Call Container */}
      <div className="relative w-[800px] h-[450px] bg-gray-700 rounded-3xl overflow-hidden">
        {/* Background Image */}
        <img
          src={therapyRoom}
          alt="Therapy Room"
          className="w-full h-full object-cover"
        />

        {/* Static Self-View (Top Right Box) */}
        <div className="absolute top-4 right-4 w-48 h-28 bg-gray-600 rounded-2xl overflow-hidden">
          <img
            src="https://placehold.co/160x100"
            alt="User Self-View"
            className="w-full h-full object-cover"
          />
        </div>

        {/* Call Controls */}
        <div className="absolute bottom-6 left-1/2 transform -translate-x-1/2 flex gap-6">
          {/* Mic Button */}
          <div className="w-16 h-16 bg-gray-500 rounded-full flex items-center justify-center">
            <svg
              width="34"
              height="45"
              viewBox="0 0 34 45"
              fill="white"
              xmlns="http://www.w3.org/2000/svg"
              className="w-8 h-8"
            >
              <path d="M16.2471 0.0230268C16.0009 0.0627663 15.7576 0.118902 15.5189 0.19106C14.2554 0.475607 13.1295 1.18923 12.333 2.21046C11.5364 3.23169 11.1184 4.49741 11.15 5.79218V16.9944C11.15 18.4799 11.7401 19.9046 12.7906 20.955C13.841 22.0054 15.2656 22.5955 16.7512 22.5955C18.2367 22.5955 19.6613 22.0054 20.7117 20.955C21.7622 19.9046 22.3523 18.4799 22.3523 16.9944V5.79218C22.3794 4.99822 22.2373 4.20757 21.9354 3.47274C21.6335 2.73791 21.1788 2.07569 20.6014 1.53007C20.024 0.984442 19.3371 0.567887 18.5864 0.308057C17.8356 0.0482265 17.0382 -0.0489337 16.2471 0.0230268Z" />
            </svg>
          </div>

          {/* Camera Button */}
          <div className="w-16 h-16 bg-gray-500 rounded-full flex items-center justify-center">
            <svg
              width="45"
              height="28"
              viewBox="0 0 45 28"
              fill="white"
              xmlns="http://www.w3.org/2000/svg"
              className="w-10 h-6"
            >
              <path d="M2.8 0C1.232 0 0 1.288 0 2.8V25.2C0 26.768 1.288 28 2.8 28H30.8C32.368 28 33.6 26.768 33.6 25.2V16.8L39.2 22.4H44.8V5.6H39.2L33.6 11.2V2.8C33.6 1.232 32.368 0 30.8 0H2.8Z" />
            </svg>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CBTSession;