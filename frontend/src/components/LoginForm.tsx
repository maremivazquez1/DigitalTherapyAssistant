import React, { useState, ChangeEvent, FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { login } from '../services/auth/authService'; // Adjust the path if needed

const LoginForm: React.FC = () => {
  const [username, setUsername] = useState(''); // State for username
  const [password, setPassword] = useState(''); // State for password
  const [error, setError] = useState('');       // State for error messages
  const navigate = useNavigate();

  // Handle input changes
  const handleInputChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { id, value } = e.target;
    if (id === 'username') {
      setUsername(value);
    } else if (id === 'password') {
      setPassword(value);
    }
  };

  // Handle form submission
  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      const response = await login({ username, password });
      // Check if login was successful based on the message returned from the backend
      if (response.message === 'login success') {
        // Redirect to dashboard (or any other protected route)
        navigate('/dashboard');
      } else {
        setError(response.message);
      }
    } catch (err: any) {
      setError(err.message || 'Invalid credentials');
    }
  };

  return (
    <div data-theme="calming" className="flex h-screen flex-col items-center justify-center gap-4 bg-base-200 p-4">
      <h1 className="text-4xl font-bold text-primary">Welcome to Digital Therapy Assistant</h1>
      <p className="text-lg text-base-content/70">
        Experience personalized mental health support - anytime,
      </p>
      <div className="w-full max-w-md p-6 bg-base-100 rounded-lg shadow-lg">
        <h2 className="text-2xl font-bold text-center text-base-content">Login</h2>
        <form className="space-y-6 mt-6" onSubmit={handleSubmit}>
          <div>
            <label htmlFor="username" className="block text-sm font-medium text-base-content">
              Email
            </label>
            <input
              type="text"
              id="username"
              placeholder="Enter your email"
              required
              value={username}
              onChange={handleInputChange}
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
              onChange={handleInputChange}
              className="mt-1 block w-full px-3 py-2 border border-base-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
            />
          </div>
          {error && <div className="text-red-500 text-sm">{error}</div>}
          <div>
            <button
              type="submit"
              className="w-full py-2 px-4 bg-primary text-white font-semibold rounded-md hover:bg-primary-focus focus:outline-none focus:ring-2 focus:ring-primary-focus focus:ring-offset-2"
            >
              Login
            </button>
          </div>
        </form>
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
