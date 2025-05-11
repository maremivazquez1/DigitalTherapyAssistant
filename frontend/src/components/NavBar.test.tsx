import '@testing-library/jest-dom';
/// <reference types="vitest" />
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';

// Partial mock: override only useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

import NavBar from './NavBar';

// --- Test Suite ---
describe('NavBar Component', () => {
  beforeEach(() => {
    // Set a dummy authToken so logout can remove it
    localStorage.setItem('authToken', 'dummy-token');
    mockNavigate.mockClear();
  });

  it('renders brand link pointing to home', () => {
    render(
      <MemoryRouter>
        <NavBar />
      </MemoryRouter>
    );
    const brandLink = screen.getByRole('link', { name: /Digital Therapy Assistant/i });
    expect(brandLink).toBeInTheDocument();
    expect(brandLink).toHaveAttribute('href', '/');
  });

  it('renders Profile link in dropdown', () => {
    render(
      <MemoryRouter>
        <NavBar />
      </MemoryRouter>
    );
    const profileLink = screen.getByRole('link', { name: /Profile/i });
    expect(profileLink).toBeInTheDocument();
    expect(profileLink).toHaveAttribute('href', '/profile');
  });

  it('clears authToken and navigates to login on logout click', () => {
    render(
      <MemoryRouter>
        <NavBar />
      </MemoryRouter>
    );
    const logoutButton = screen.getByRole('button', { name: /Logout/i });
    fireEvent.click(logoutButton);

    // authToken should be removed from localStorage
    expect(localStorage.getItem('authToken')).toBeNull();
    // navigate('/login') should have been called
    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });
});