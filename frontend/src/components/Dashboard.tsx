import { Link } from 'react-router-dom';

const Dashboard = () => {
  return (
    <div data-theme="calming" className="min-h-screen bg-base-100 p-8">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <header className="mb-16 text-center">
          <h1 className="text-6xl font-extrabold text-primary tracking-tight">
            Dashboard
          </h1>
          <p className="mt-4 text-xl text-base-content/80">
            Welcome back! Select an option below to continue.
          </p>
        </header>

        {/* 2x2 Grid of Navigation Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-2 gap-16">
          {/* Start a CBT Session Card */}
          <Link to="/cbt" className="group">
            <div className="flex flex-col justify-center p-8 bg-base-200 rounded-3xl shadow-lg transition-transform transform duration-300 ease-out group-hover:scale-105 min-h-[200px] border border-gray-200">
              <h2 className="text-3xl font-semibold text-base-content mb-4">
                Start a CBT Session
              </h2>
              <p className="text-lg text-base-content/70">
                Begin your Cognitive Behavioral Therapy session.
              </p>
            </div>
          </Link>

          {/* Take a Burnout Assessment Card */}
          <Link to="/burnout" className="group">
            <div className="flex flex-col justify-center p-8 bg-base-200 rounded-3xl shadow-lg transition-transform transform duration-300 ease-out group-hover:scale-105 min-h-[200px] border border-gray-200">
                <h2 className="text-3xl font-semibold text-base-content mb-4">
                  Take a Burnout Assessment
                </h2>
                <p className="text-lg text-base-content/70">
                  Evaluate your stress and burnout levels.
                </p>
              </div>
          </Link>

          {/* Check on Your Mood Card */}
          {/* <div className="group">
            <div className="flex flex-col justify-center p-8 bg-base-200 rounded-3xl shadow-lg opacity-50 cursor-not-allowed min-h-[200px] border border-gray-200">
              <h2 className="text-3xl font-semibold text-base-content mb-4">
                Check on Your Mood
              </h2>
              <p className="text-lg text-base-content/70">
                Coming soon: Monitor and log your mood.
              </p>
            </div>
          </div> */}

          {/* Write a Guided Journal Entry Card */}
          {/* <div className="group">
            <div className="flex flex-col justify-center p-8 bg-base-200 rounded-3xl shadow-lg opacity-50 cursor-not-allowed min-h-[200px] border border-gray-200">
              <h2 className="text-3xl font-semibold text-base-content mb-4">
                Write a Guided Journal Entry
              </h2>
              <p className="text-lg text-base-content/70">
                Coming soon: Reflect with guided journaling prompts.
              </p>
            </div>
          </div> */}
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
