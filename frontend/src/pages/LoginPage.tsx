import React, { useContext } from "react";
import { useNavigate } from "react-router-dom";
import { AuthContext } from "../context/AuthContext";
import LoginForm from "../components/LoginForm";

const LoginPage: React.FC = () => {
  const { login } = useContext(AuthContext);
  const navigate = useNavigate();

  const handleLoginSuccess = (token: string) => {
    login(token);
    navigate("/");
  };

  return (
      <LoginForm onSuccess={handleLoginSuccess} />
  );
};

export default LoginPage;