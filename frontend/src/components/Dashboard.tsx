import React from 'react';
import { Link } from 'react-router-dom';

const Dashboard = () => {
  // Replace this with your actual logout logic
  const handleLogout = () => {
    // e.g., remove token from storage, update state, and redirect to /login
    console.log('Logout clicked');
  };

  return (
    <div data-theme="calming" className="min-h-screen bg-base-200 p-4">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <header className="mb-8 text-center">
          <h1 className="text-4xl font-bold text-primary">Dashboard</h1>
          <p className="text-lg text-base-content/70 mt-2">
            Welcome back, [Username]! Select an option below to continue.
          </p>
        </header>

        {/* Navigation Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
          {/* CBT Feature Card */}
          <Link to="/cbt" className="block">
            <div className="p-6 bg-base-100 rounded-lg shadow-md hover:shadow-lg transition-shadow">
              <h2 className="text-2xl font-bold text-base-content">CBT Feature</h2>
              <p className="mt-2 text-base-content/70">
                Start your CBT session and begin your journey.
              </p>
            </div>
          </Link>

          {/* Profile Card */}
          <Link to="/profile" className="block">
            <div className="p-6 bg-base-100 rounded-lg shadow-md hover:shadow-lg transition-shadow">
              <h2 className="text-2xl font-bold text-base-content">Profile</h2>
              <p className="mt-2 text-base-content/70">
                View and manage your account details.
              </p>
            </div>
          </Link>

          {/* Intake Form Card */}
          <Link to="/intake" className="block">
            <div className="p-6 bg-base-100 rounded-lg shadow-md hover:shadow-lg transition-shadow">
              <h2 className="text-2xl font-bold text-base-content">Intake Form</h2>
              <p className="mt-2 text-base-content/70">
                Fill out or update your intake information.
              </p>
            </div>
          </Link>

          {/* Logout Button */}
          <div className="flex items-center justify-center">
            <button onClick={handleLogout} className="btn btn-wide btn-error">
              Logout
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
