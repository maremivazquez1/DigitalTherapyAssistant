// src/components/RegistrationForm.test.tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import RegistrationForm from './RegistrationForm';

// Mock the auth service module
vi.mock('../services/authService', () => ({
  register: vi.fn(),
}));

// Import the mocked register service for use in our tests
import { register as mockRegister } from '../services/authService';

describe('RegistrationForm component', () => {
  it('shows an error message if passwords do not match', async () => {
    render(
      <MemoryRouter>
        <RegistrationForm />
      </MemoryRouter>
    );
    
    // Get form elements by their label text or role
    const nameInput = screen.getByLabelText(/Name/i);
    const emailInput = screen.getByLabelText(/Email/i);
    const passwordInput = screen.getByLabelText(/^Password$/i); // Matches the Password label exactly
    const confirmPasswordInput = screen.getByLabelText(/Confirm Password/i);
    const submitButton = screen.getByRole('button', { name: /Register/i });
    
    // Fill out the form with mismatched passwords
    fireEvent.change(nameInput, { target: { value: 'Test User' } });
    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
    fireEvent.change(passwordInput, { target: { value: 'password1' } });
    fireEvent.change(confirmPasswordInput, { target: { value: 'password2' } });
    
    // Submit the form
    fireEvent.click(submitButton);
    
    // Wait for the error message to appear
    await waitFor(() => {
      expect(screen.getByText(/Passwords do not match/i)).toBeDefined();
    });
  });

  it('submits the form successfully when passwords match', async () => {
    // Set up the mock to resolve successfully
    (mockRegister as any).mockResolvedValueOnce({
      token: 'dummy-token',
      user: { id: 1, username: 'Test User', email: 'test@example.com' }
    });
    
    render(
      <MemoryRouter>
        <RegistrationForm />
      </MemoryRouter>
    );
    
    const nameInput = screen.getByLabelText(/Name/i);
    const emailInput = screen.getByLabelText(/Email/i);
    const passwordInput = screen.getByLabelText(/^Password$/i);
    const confirmPasswordInput = screen.getByLabelText(/Confirm Password/i);
    const submitButton = screen.getAllByRole('button', { name: /Register/i })[0];

    // Fill out the form with matching passwords
    fireEvent.change(nameInput, { target: { value: 'Test User' } });
    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
    fireEvent.change(passwordInput, { target: { value: 'password123' } });
    fireEvent.change(confirmPasswordInput, { target: { value: 'password123' } });
    
    // Submit the form
    fireEvent.click(submitButton);
    
    // Wait for the success message to appear
    await waitFor(() => {
      expect(screen.getByText(/Registration successful!/i)).toBeDefined();
    });
  });
});
