import '@testing-library/jest-dom';
/// <reference types="vitest" />
import { render, screen } from '@testing-library/react';
import { describe, it, vi } from 'vitest';

// Stub the Dashboard component
vi.mock('../components/Dashboard', () => ({
  default: () => <div data-testid="dashboard">Mock Dashboard</div>,
}));

import DashboardPage from './DashboardPage';

describe('DashboardPage', () => {
  it('renders the Dashboard component', () => {
    render(<DashboardPage />);
    const dashboard = screen.getByTestId('dashboard');
    expect(dashboard).toBeInTheDocument();
    expect(dashboard).toHaveTextContent('Mock Dashboard');
  });
});
