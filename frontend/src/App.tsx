import './App.css'

import React from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import CBTPage from "./pages/CBTPage";
import BurnoutAssessmentPage from "./pages/BurnoutAssessmentPage";
import { ProtectedRoute } from './components/ProtectedRoute';



const App: React.FC = () => {
  return (
    <div data-theme="calming">
      <Navbar />
      
      <Routes>
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/login" element={<LoginPage />} />
        {/* Protected routes that redirect to login page */}
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<CBTPage />} />
          <Route path="/cbt" element={<CBTPage />} />
          <Route path="/burnout" element={<BurnoutAssessmentPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </div>
  );
};


export default App;