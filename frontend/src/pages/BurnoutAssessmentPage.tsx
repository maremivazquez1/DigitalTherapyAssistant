import React from "react";
import BurnoutAssessment from "../components/BurnoutAssessment";
import NavBar from "../components/NavBar";

const BurnoutAssessmentPage: React.FC = () => {
  return (
    <div className="page-container">
       <NavBar />
      <BurnoutAssessment />
    </div>
  );
};

export default BurnoutAssessmentPage;
