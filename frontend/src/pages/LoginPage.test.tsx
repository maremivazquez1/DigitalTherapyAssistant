import '@testing-library/jest-dom';
/// <reference types="vitest" />
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';

// Mock LoginForm to expose an onSuccess button
vi.mock('../components/LoginForm', () => ({
  default: ({ onSuccess }: any) => (
    <button data-testid="login-success" onClick={() => onSuccess('dummy-token')}>
      Mock Login
    </button>
  ),
}));

// Mock useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

import LoginPage from './LoginPage';

describe('LoginPage', () => {
  it('calls context login and navigates home on successful login', () => {
    const loginMock = vi.fn();
    render(
      <AuthContext.Provider value={{ login: loginMock, isLoggedIn: false, logout: vi.fn() }}>
        <MemoryRouter>
          <LoginPage />
        </MemoryRouter>
      </AuthContext.Provider>
    );

    // Click the mock login button
    fireEvent.click(screen.getByTestId('login-success'));

    // Expect context login called with token
    expect(loginMock).toHaveBeenCalledWith('dummy-token');
    // Expect navigation to '/'
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });
});
