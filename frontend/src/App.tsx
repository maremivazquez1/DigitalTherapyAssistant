import './App.css'

import React from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import DashboardPage from "./pages/DashboardPage";
import CBTPage from "./pages/CBTPage";
import BurnoutAssessmentPage from "./pages/BurnoutAssessmentPage";
import BurnoutSummaryPage from "./pages/BurnoutSummaryPage";
import { ProtectedRoute } from './components/ProtectedRoute';



const App: React.FC = () => {
  return (
    <div data-theme="calming">
      <Routes>
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/login" element={<LoginPage />} />
        {/* Protected routes that redirect to login page */}
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/cbt" element={<CBTPage />} />
          <Route path="/burnout" element={<BurnoutAssessmentPage />} />
          <Route path="/burnout-summary" element={<BurnoutSummaryPage/>} 
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </div>
  );
};


export default App;