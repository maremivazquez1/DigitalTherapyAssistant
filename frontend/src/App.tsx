import './App.css'

import React from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import Login from "./pages/Login";
import Register from "./pages/Register";
import CBT from "./pages/CBT";
import { ProtectedRoute } from './components/ProtectedRoute';

const App: React.FC = () => {
  return (
    <div data-theme="calming">
      <Routes>
        <Route path="/register" element={<Register />} />
        <Route path="/login" element={<Login />} />
        {/* Protected routes that redirect to login page */}
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<CBT />} />
          <Route path="/cbt" element={<CBT />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </div>
  );
};


export default App;