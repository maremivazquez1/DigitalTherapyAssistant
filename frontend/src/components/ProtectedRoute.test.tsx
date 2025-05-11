import '@testing-library/jest-dom';
/// <reference types="vitest" />
import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, type Mock } from 'vitest';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';
import { isAuthenticated } from '../services/auth/authService';
// Mock the authService's isAuthenticated
vi.mock('../services/auth/authService', () => ({
  isAuthenticated: vi.fn(),
}));

// Helper to render with routes
function renderWithRoutes(initialEntries: string[]) {
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <Routes>
        <Route element={<ProtectedRoute />}>   
          <Route path="/protected" element={<div>Protected Content</div>} />
        </Route>
        <Route path="/login" element={<div>Login Page</div>} />
      </Routes>
    </MemoryRouter>
  );
}

describe('ProtectedRoute', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('redirects unauthenticated users to /login', () => {
    // Simulate not authenticated
    (isAuthenticated as Mock).mockReturnValue(false);

    renderWithRoutes(['/protected']);
    // Should show the login page content
    expect(screen.getByText('Login Page')).toBeInTheDocument();
    // Protected content should not be rendered
    expect(screen.queryByText('Protected Content')).toBeNull();
  });

  it('renders child routes when authenticated', () => {
    // Simulate authenticated
    (isAuthenticated as Mock).mockReturnValue(true);

    renderWithRoutes(['/protected']);
    // Should render protected content
    expect(screen.getByText('Protected Content')).toBeInTheDocument();
    // Login page should not appear
    expect(screen.queryByText('Login Page')).toBeNull();
  });
});