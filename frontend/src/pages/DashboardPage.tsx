import React from "react";
import Dashboard from "../components/Dashboard";
import NavBar from "../components/NavBar";

const DashboardPage: React.FC = () => {
  return (
    <>
      <NavBar />
      <Dashboard />
    </>
  );
};

export default DashboardPage;