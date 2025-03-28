import './App.css'

import React from "react";
import { Route, Routes } from "react-router-dom";
import Login from "./pages/Login";
import Register from "./pages/Register";
import CBT from "./pages/CBT";

const App: React.FC = () => {
  return (
    <div data-theme="calming">
      <Routes>
        <Route path="/" element={<CBT />} />
        <Route path="/register" element={<Register />} />
        <Route path="/login" element={<Login />} />
        <Route path="/cbt" element={<CBT />} />
      </Routes>
    </div>
  );
};


export default App;