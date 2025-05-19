// src/pages/DashboardPage.test.tsx
import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';
import { describe, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';

// 1) Stub NavBar and Dashboard before importing the page
vi.mock('../components/NavBar', () => ({
  default: () => <div data-testid="navbar">Mock NavBar</div>,
}));
vi.mock('../components/Dashboard', () => ({
  default: () => <div data-testid="dashboard">Mock Dashboard</div>,
}));

import DashboardPage from './DashboardPage';

describe('DashboardPage', () => {
  it('renders NavBar and Dashboard', () => {
    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    );

    // NavBar stub
    const nav = screen.getByTestId('navbar');
    expect(nav).toBeInTheDocument();
    expect(nav).toHaveTextContent('Mock NavBar');

    // Dashboard stub
    const dash = screen.getByTestId('dashboard');
    expect(dash).toBeInTheDocument();
    expect(dash).toHaveTextContent('Mock Dashboard');
  });
});
