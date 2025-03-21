// src/components/LoginFormRender.test.tsx
import '@testing-library/jest-dom';
import { describe, it, expect, vi, Mock } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import LoginForm from './LoginForm';
import { login as mockLogin } from '../services/auth/authService';

// Mock the authService to prevent actual API calls
vi.mock('../services/authService', () => ({
  login: vi.fn(),
}));

// Renders inputs
describe('LoginForm - Render Tests', () => {
  it('renders username and password inputs and login button', () => {
    render(
      <MemoryRouter>
        <LoginForm />
      </MemoryRouter>
    );

    // Check Username field
    const usernameInput = screen.getByLabelText(/Username/i);
    expect(usernameInput).toBeInTheDocument();
    expect(usernameInput).toHaveAttribute('required');

    // Check Password field
    const passwordInput = screen.getByLabelText(/Password/i);
    expect(passwordInput).toBeInTheDocument();
    expect(passwordInput).toHaveAttribute('required');

    // Check Login button
    const loginButton = screen.getByRole('button', { name: /Login/i });
    expect(loginButton).toBeInTheDocument();
  });
});

// Required fields validation
describe('LoginForm - Required Fields Enforcement', () => {
  it('does not call login if the username is missing', () => {
    render(
      <MemoryRouter>
        <LoginForm />
      </MemoryRouter>
    );

    // Only fill in the password
    fireEvent.change(screen.getByLabelText(/Password/i), { target: { value: 'password123' } });

    // Attempt to submit the form
    fireEvent.click(screen.getByRole('button', { name: /Login/i }));

    // login service should not be called because username is missing
    expect(mockLogin).not.toHaveBeenCalled();
  });

  it('does not call login if the password is missing', () => {
    render(
      <MemoryRouter>
        <LoginForm />
      </MemoryRouter>
    );

    // Only fill in the username
    fireEvent.change(screen.getByLabelText(/Username/i), { target: { value: 'johnDoe' } });

    // Attempt to submit the form
    fireEvent.click(screen.getByRole('button', { name: /Login/i }));

    // login service should not be called because password is missing
    expect(mockLogin).not.toHaveBeenCalled();
  });
});

// Successful submission
describe('LoginForm - Successful Submission', () => {
  it('calls the login service with the correct payload on valid submission', async () => {
    // Mock a successful login response from the backend
    (mockLogin as Mock).mockResolvedValueOnce({
      token: 'fakeToken',
      message: 'login success',
    });

    render(
      <MemoryRouter>
        <LoginForm />
      </MemoryRouter>
    );

    // Fill out the form with valid data
    fireEvent.change(screen.getByLabelText(/Username/i), { target: { value: 'johnDoe' } });
    fireEvent.change(screen.getByLabelText(/Password/i), { target: { value: 'password123' } });

    // Submit the form
    fireEvent.click(screen.getByRole('button', { name: /Login/i }));

    // Wait for the login service to be called with the correct payload
    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith({
        username: 'johnDoe',
        password: 'password123',
      });
    });
  });
});

// Server error response
describe('LoginForm - Server Error Response', () => {
  it('displays an error message when login fails', async () => {
    // Mock a server error response
    (mockLogin as Mock).mockRejectedValueOnce(new Error('invalid credentials'));

    render(
      <MemoryRouter>
        <LoginForm />
      </MemoryRouter>
    );

    // Fill out the form with invalid credentials
    fireEvent.change(screen.getByLabelText(/Username/i), { target: { value: 'johnDoe' } });
    fireEvent.change(screen.getByLabelText(/Password/i), { target: { value: 'wrongPassword' } });

    // Submit the form
    fireEvent.click(screen.getByRole('button', { name: /Login/i }));

    // Wait for the error message to be displayed
    await waitFor(() => {
      expect(screen.getByText(/invalid credentials/i)).toBeInTheDocument();
    });
  });
});
