import React, { useContext } from "react";
import { useNavigate } from "react-router-dom";
import RegistrationForm from "../components/RegistrationForm";
import { AuthContext } from "../context/AuthContext";

const RegistrationPage: React.FC = () => {
  const { login } = useContext(AuthContext);
  const navigate = useNavigate();

  const handleRegisterSuccess = (token: string) => {
    login(token);
    navigate("/");
  };

  return (
    <div>
      <RegistrationForm onSuccess={handleRegisterSuccess} />
    </div>
  );
};

export default RegistrationPage;
