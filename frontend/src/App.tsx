import './App.css'

import React from "react";
import { Route, Routes } from "react-router-dom";
import PrivateRoute from "./components/PrivateRoute";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import CBTPage from "./pages/CBTPage";

const App: React.FC = () => {
  return (
    <div data-theme="calming">
      <Routes>

        {/* Public routes */}
        <Route path="/" element={<CBTPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/login" element={<LoginPage />} />
        
        {/* Protected routes: */}
        <Route element={<PrivateRoute />}>
          <Route path="/cbt" element={<CBTPage />} />
        </Route>
        
      </Routes>
    </div>
  );
};


export default App;