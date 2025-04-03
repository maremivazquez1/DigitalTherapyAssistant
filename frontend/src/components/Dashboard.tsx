import React from 'react';
import { Link } from 'react-router-dom';

const Dashboard = () => {
  return (
    <div data-theme="calming" className="min-h-screen bg-base-200 p-6">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <header className="mb-10 text-center">
          <h1 className="text-4xl md:text-5xl font-extrabold text-primary">
            Dashboard
          </h1>
          <p className="mt-3 text-lg text-base-content/70">
            Welcome back, [Username]! Select an option below to continue.
          </p>
        </header>

        {/* 2x2 Grid of Navigation Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-8">
          {/* Start a CBT Session Card */}
          <Link to="/cbt" className="group">
            <div className="flex flex-col justify-center p-6 bg-base-100 rounded-xl shadow-lg border-2 border-primary transition transform duration-200 ease-in-out group-hover:scale-105 min-h-[160px]">
              <h2 className="text-2xl font-bold text-base-content mb-2">
                Start a CBT Session
              </h2>
              <p className="text-base text-base-content/70">
                Begin your Cognitive Behavioral Therapy session.
              </p>
            </div>
          </Link>

          {/* Take a Burnout Assessment Card */}
          <div className="group">
            <div className="flex flex-col justify-center p-6 bg-base-100 rounded-xl shadow-lg min-h-[160px] opacity-50 cursor-not-allowed">
              <h2 className="text-2xl font-bold text-base-content mb-2">
                Take a Burnout Assessment
              </h2>
              <p className="text-base text-base-content/70">
                Coming soon: Evaluate your stress and burnout levels.
              </p>
            </div>
          </div>

          {/* Check on Your Mood Card */}
          <div className="group">
            <div className="flex flex-col justify-center p-6 bg-base-100 rounded-xl shadow-lg min-h-[160px] opacity-50 cursor-not-allowed">
              <h2 className="text-2xl font-bold text-base-content mb-2">
                Check on Your Mood
              </h2>
              <p className="text-base text-base-content/70">
                Coming soon: Monitor and log your mood.
              </p>
            </div>
          </div>

          {/* Write a Guided Journal Entry Card */}
          <div className="group">
            <div className="flex flex-col justify-center p-6 bg-base-100 rounded-xl shadow-lg min-h-[160px] opacity-50 cursor-not-allowed">
              <h2 className="text-2xl font-bold text-base-content mb-2">
                Write a Guided Journal Entry
              </h2>
              <p className="text-base text-base-content/70">
                Coming soon: Reflect with guided journaling prompts.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
