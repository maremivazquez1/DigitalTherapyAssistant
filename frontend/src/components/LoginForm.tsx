import React, { useState, ChangeEvent, FormEvent } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { login as loginService } from '../services/auth/authService';

interface LocationState {
  from?: {
    pathname: string;
  };
}

interface LoginFormProps {
  onSuccess: (token: string) => void;
}

const LoginForm: React.FC<LoginFormProps> = ({ onSuccess }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);

  const navigate = useNavigate();
  const location = useLocation();
  // go back to attempted accessed page, but default to /
  const state = location.state as LocationState;
  const from  = state?.from?.pathname || '/';

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError(null);

    if (!email || !password) return;

    const payload = {
      username: email,
      password,
    };

    try {
      const authResponse = await loginService(payload);
      onSuccess(authResponse.token);
      navigate(from, { replace: true });
    } catch (err: any) {
      setError(err.message || 'Login failed');
    }
  };

  return (
    <div data-theme="calming" className="flex h-screen flex-col items-center justify-center gap-4 bg-base-200 p-4">
      <h1 className="text-4xl font-bold text-primary">Welcome to Digital Therapy Assistant</h1>
      <p className="text-lg text-base-content/70">
        Experience personalized mental health support - anytime, anywhere.
      </p>
      <div className="w-full max-w-md p-6 bg-base-100 rounded-lg shadow-lg">
        <h2 className="text-2xl font-bold text-center text-base-content">Login</h2>
        <form className="space-y-6 mt-6" onSubmit={handleSubmit}>
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-base-content">
              Email
            </label>
            <input
              type="email"
              id="email"
              placeholder="Enter your email"
              required
              value={email}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setEmail(e.target.value)}
              className="mt-1 block w-full px-3 py-2 border border-base-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
            />
          </div>
          <div>
            <label htmlFor="password" className="block text-sm font-medium text-base-content">
              Password
            </label>
            <input
              type="password"
              id="password"
              placeholder="Enter your password"
              required
              value={password}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setPassword(e.target.value)}
              className="mt-1 block w-full px-3 py-2 border border-base-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
            />
          </div>
          <div>
            <button
              type="submit"
              className="w-full py-2 px-4 bg-primary text-white font-semibold rounded-md hover:bg-primary-focus focus:outline-none focus:ring-2 focus:ring-primary-focus focus:ring-offset-2"
            >
              Login
            </button>
          </div>
        </form>
        {error && <p className="mt-4 text-center text-error">{error}</p>}
        <p className="text-sm text-center text-base-content mt-4">
          {"Don't have an account? "}
          <Link to="/register" className="text-primary hover:underline">
            Register
          </Link>
        </p>
      </div>
    </div>
  );
};

export default LoginForm;
