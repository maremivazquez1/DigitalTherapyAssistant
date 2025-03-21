// src/components/RegistrationFormRender.test.tsx
import '@testing-library/jest-dom';
import { describe, it, expect, vi, Mock, MockedFunction } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import RegistrationForm from './RegistrationForm';
import { register as mockRegister } from '../services/auth//authService';


// Mock the authService to prevent actual API calls
vi.mock('../services/authService', () => ({
  register: vi.fn(),
}));

// Renders inputs
describe('RegistrationForm - Render Tests', () => {
  it('renders all expected inputs', () => {
    render(
      <MemoryRouter>
        <RegistrationForm />
      </MemoryRouter>
    );

    // 1. Check First Name field
    const firstNameInput = screen.getByLabelText(/First Name/i);
    expect(firstNameInput).toBeInTheDocument();
    expect(firstNameInput).toHaveAttribute('required');

    // 2. Check Last Name field
    const lastNameInput = screen.getByLabelText(/Last Name/i);
    expect(lastNameInput).toBeInTheDocument();
    expect(lastNameInput).toHaveAttribute('required');

    // 3. Check Email field
    const emailInput = screen.getByLabelText(/Email/i);
    expect(emailInput).toBeInTheDocument();
    expect(emailInput).toHaveAttribute('required');

    // 4. Check Password field
    const passwordInput = screen.getByLabelText(/^Password$/i);
    expect(passwordInput).toBeInTheDocument();
    expect(passwordInput).toHaveAttribute('required');

    // 5. Check Confirm Password field
    const confirmPasswordInput = screen.getByLabelText(/Confirm Password/i);
    expect(confirmPasswordInput).toBeInTheDocument();
    expect(confirmPasswordInput).toHaveAttribute('required');

    // 6. Check Phone field (optional)
    const phoneInput = screen.getByLabelText(/Phone/i);
    expect(phoneInput).toBeInTheDocument();
    // Not required by default, so no .toHaveAttribute('required') here

    // 7. Check Date of Birth field (optional)
    const dobInput = screen.getByLabelText(/Date of Birth/i);
    expect(dobInput).toBeInTheDocument();
    // Also optional, so no required check
  });
});


// Required fields validation

describe('RegistrationForm - Required Fields Enforcement', () => {
  it('does not submit if a required field is missing', () => {
    render(
      <MemoryRouter>
        <RegistrationForm />
      </MemoryRouter>
    );
    // For demonstration, let's fill out everything EXCEPT "last_name"
    fireEvent.change(screen.getByLabelText(/First Name/i), { target: { value: 'John' } });
    // Omit last name
    fireEvent.change(screen.getByLabelText(/Email/i), { target: { value: 'john@example.com' } });
    fireEvent.change(screen.getByLabelText(/^Password$/i), { target: { value: 'password123' } });
    fireEvent.change(screen.getByLabelText(/Confirm Password/i), { target: { value: 'password123' } });

    // Attempt to submit
    const submitButton = screen.getByRole('button', { name: /Register/i });
    fireEvent.click(submitButton);

    // Because "last_name" is required and empty, we expect the form not to call registerService
    expect(mockRegister).not.toHaveBeenCalled();

    // Optional: If you want to test the browser's native validation message, you'd need to
    // check for it specifically, but typically that's outside the scope of unit tests.
  });
});

// Email format validation
describe('RegistrationForm - Email Field (HTML5 validation)', () => {
  it('does not call registerService if the email is invalid', () => {
    render(
      <MemoryRouter>
        <RegistrationForm />
      </MemoryRouter>
    );

    // Fill out form but with an invalid email
    fireEvent.change(screen.getByLabelText(/First Name/i), { target: { value: 'John' } });
    fireEvent.change(screen.getByLabelText(/Last Name/i), { target: { value: 'Doe' } });
    fireEvent.change(screen.getByLabelText(/Email/i), { target: { value: 'notanemail' } });
    fireEvent.change(screen.getByLabelText(/^Password$/i), { target: { value: 'password123' } });
    fireEvent.change(screen.getByLabelText(/Confirm Password/i), { target: { value: 'password123' } });

    // Submit
    const submitButton = screen.getByRole('button', { name: /Register/i });
    fireEvent.click(submitButton);

    // Because HTML5 validation blocks submission if email is invalid,
    // the registerService should not be called
    expect(mockRegister).not.toHaveBeenCalled();
  });
});


// Password mismatch handling
describe('RegistrationForm - Password Mismatch', () => {
  it('shows an error and does not call registerService if passwords do not match', async () => {
    render(
      <MemoryRouter>
        <RegistrationForm />
      </MemoryRouter>
    );

    // Fill out all required fields except for matching passwords
    fireEvent.change(screen.getByLabelText(/First Name/i), { target: { value: 'John' } });
    fireEvent.change(screen.getByLabelText(/Last Name/i), { target: { value: 'Doe' } });
    fireEvent.change(screen.getByLabelText(/Email/i), { target: { value: 'john@example.com' } });
    fireEvent.change(screen.getByLabelText(/^Password$/i), { target: { value: 'password123' } });
    fireEvent.change(screen.getByLabelText(/Confirm Password/i), { target: { value: 'differentPass' } });

    // Optionally fill out phone & date_of_birth if needed
    fireEvent.change(screen.getByLabelText(/Phone/i), { target: { value: '1234567890' } });
    fireEvent.change(screen.getByLabelText(/Date of Birth/i), { target: { value: '1990-01-01' } });

    // Submit the form
    fireEvent.click(screen.getByRole('button', { name: /Register/i }));

    // Wait for the error message to appear
    await waitFor(() => {
      expect(screen.getByText(/Passwords do not match/i)).toBeInTheDocument();
    });

    // Ensure registerService was NOT called
    expect(mockRegister).not.toHaveBeenCalled();
  });
});


// Successful submission
describe('RegistrationForm - Successful Submission', () => {
  it('submits the form successfully with matching passwords', async () => {
    // Mock a successful response from the backend
    (mockRegister as Mock).mockResolvedValueOnce({
      status: 'success',
      message: 'Registration successful!',
    });

    render(
      <MemoryRouter>
        <RegistrationForm />
      </MemoryRouter>
    );

    // Fill out all required fields with valid data
    fireEvent.change(screen.getByLabelText(/First Name/i), { target: { value: 'John' } });
    fireEvent.change(screen.getByLabelText(/Last Name/i), { target: { value: 'Doe' } });
    fireEvent.change(screen.getByLabelText(/Email/i), { target: { value: 'john@example.com' } });
    fireEvent.change(screen.getByLabelText(/^Password$/i), { target: { value: 'password123' } });
    fireEvent.change(screen.getByLabelText(/Confirm Password/i), { target: { value: 'password123' } });

    // (Optional) Fill out phone & date_of_birth if you want them tested
    fireEvent.change(screen.getByLabelText(/Phone/i), { target: { value: '1234567890' } });
    fireEvent.change(screen.getByLabelText(/Date of Birth/i), { target: { value: '1990-01-01' } });

    // Submit the form
    fireEvent.click(screen.getByRole('button', { name: /Register/i }));

    // Wait for the success message
    await waitFor(() => {
      expect(screen.getByText(/Registration successful!/i)).toBeInTheDocument();
    });

    // Verify registerService was called with the correct payload
    expect(mockRegister).toHaveBeenCalledWith({
      first_name: 'John',
      last_name: 'Doe',
      email: 'john@example.com',
      password: 'password123',
      confirm_password: 'password123',
      phone: '1234567890',
      date_of_birth: '1990-01-01',
    });
  });
});

// Server error response
describe('RegistrationForm - Server Error Response', () => {
  it('displays an error message from the server', async () => {
    // Mock a server error response
    (mockRegister as Mock).mockResolvedValueOnce({
      status: 'error',
      message: 'Email already in use',
    });

    render(
      <MemoryRouter>
        <RegistrationForm />
      </MemoryRouter>
    );

    // Fill out the form with valid data and matching passwords
    fireEvent.change(screen.getByLabelText(/First Name/i), { target: { value: 'Jane' } });
    fireEvent.change(screen.getByLabelText(/Last Name/i), { target: { value: 'Smith' } });
    fireEvent.change(screen.getByLabelText(/Email/i), { target: { value: 'jane@example.com' } });
    fireEvent.change(screen.getByLabelText(/^Password$/i), { target: { value: 'password123' } });
    fireEvent.change(screen.getByLabelText(/Confirm Password/i), { target: { value: 'password123' } });

    // (Optional) Fill out phone & date_of_birth
    fireEvent.change(screen.getByLabelText(/Phone/i), { target: { value: '5551234567' } });
    fireEvent.change(screen.getByLabelText(/Date of Birth/i), { target: { value: '1995-05-05' } });

    // Submit
    fireEvent.click(screen.getByRole('button', { name: /Register/i }));

    // Wait for the error message from the server to appear
    await waitFor(() => {
      expect(screen.getByText(/Email already in use/i)).toBeInTheDocument();
    });
  });
});

