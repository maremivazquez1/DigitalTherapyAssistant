import React, { useState, ChangeEvent, FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { register as registerService } from '../services/auth/authService';
import { RegisterData } from '../types/auth/auth';

const RegistrationForm: React.FC = () => {
  // Local state matches RegisterData exactly
  const [formData, setFormData] = useState<RegisterData>({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    confirmPassword: '',
    phone: '',
    dateOfBirth: '',

  });

  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // This handleChange function checks if the field is part of address or a top-level field
  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;

    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    // Basic client-side check for matching passwords
    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    try {
      const response = await registerService(formData);
      if (response.status === 'success') {
        setSuccess(response.message);
      } else {
        setError(response.message);
      }
    } catch (err: any) {
      setError(err.message || 'Registration failed');
    }
  };

  return (
    <div data-theme="calming" className="flex flex-col items-center justify-center min-h-screen bg-base-200 p-4">
      <h1 className="text-4xl font-bold text-primary text-center">Welcome to Digital Therapy Assistant</h1>
      <p className="text-lg text-base-content/70 text-center">Experience personalized mental health support - anytime, anywhere.</p>
      <div className="w-full max-w-md p-8 mt-8 space-y-6 bg-base-100 rounded-lg shadow-md">
      
        <h2 className="text-2xl font-bold text-center text-base-content">Register</h2>

        {error && <p className="text-sm text-center text-error">{error}</p>}
        {success && <p className="text-sm text-center text-success">{success}</p>}

        <form onSubmit={handleSubmit} className="space-y-6 mt-6">
          {/* First Name */}
          <div>
            <label htmlFor="first_name" className="block text-sm font-medium text-base-content">
              First Name
            </label>
            <input
              type="text"
              id="first_name"
              name="first_name"
              placeholder="Enter your first name"
              required
              value={formData.firstName}
              onChange={handleChange}
              className="mt-1 block w-full px-3 py-2 border border-base-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
            />
          </div>

          {/* Last Name */}
          <div>
            <label htmlFor="last_name" className="block text-sm font-medium text-base-content">
              Last Name
            </label>
            <input
              type="text"
              id="last_name"
              name="last_name"
              placeholder="Enter your last name"
              required
              value={formData.lastName}
              onChange={handleChange}
              className="mt-1 block w-full px-3 py-2 border border-base-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
            />
          </div>

          {/* Email */}
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-base-content">
              Email
            </label>
            <input
              type="email"
              id="email"
              name="email"
              placeholder="Enter your email"
              required
              value={formData.email}
              onChange={handleChange}
              className="mt-1 block w-full px-3 py-2 border border-base-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
            />
          </div>

          {/* Password */}
          <div>
            <label htmlFor="password" className="block text-sm font-medium text-base-content">
              Password
            </label>
            <input
              type="password"
              id="password"
              name="password"
              placeholder="Enter your password"
              required
              value={formData.password}
              onChange={handleChange}
              className="mt-1 block w-full px-3 py-2 border border-base-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
            />
          </div>

          {/* Confirm Password */}
          <div>
            <label htmlFor="confirm_password" className="block text-sm font-medium text-base-content">
              Confirm Password
            </label>
            <input
              type="password"
              id="confirm_password"
              name="confirm_password"
              placeholder="Confirm your password"
              required
              value={formData.confirmPassword}
              onChange={handleChange}
              className="mt-1 block w-full px-3 py-2 border border-base-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
            />
          </div>

          {/* Phone */}
          <div>
            <label htmlFor="phone" className="block text-sm font-medium text-base-content">
              Phone
            </label>
            <input
              type="text"
              id="phone"
              name="phone"
              placeholder="Enter your phone number"
              value={formData.phone}
              onChange={handleChange}
              className="mt-1 block w-full px-3 py-2 border border-base-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
            />
          </div>

          {/* Date of Birth */}
          <div>
            <label htmlFor="date_of_birth" className="block text-sm font-medium text-base-content">
              Date of Birth
            </label>
            <input
              type="date"
              id="date_of_birth"
              name="date_of_birth"
              value={formData.dateOfBirth}
              onChange={handleChange}
              className="mt-1 block w-full px-3 py-2 border border-base-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
            />
          </div>

          <div className="flex justify-center">
            <button data-testid="submit-button" type="submit" className="btn btn-wide btn-primary">
            Register
            </button>
          </div>
        </form>

        <p className="text-sm text-center text-base-content">
          Already have an account?{" "}
          <Link to="/login" className="text-primary hover:underline">
            Login
          </Link>
        </p>
      </div>
    </div>
  );
};

export default RegistrationForm;
