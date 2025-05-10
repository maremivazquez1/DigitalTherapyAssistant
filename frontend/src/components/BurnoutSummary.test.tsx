import '@testing-library/jest-dom';
/// <reference types="vitest" />
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';

// Variables to control router mocks
let mockLocationState: any = {};
const mockNavigate = vi.fn();

// Mock react-router-dom before importing component
vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
  useLocation: () => ({ state: mockLocationState }),
}));

// Now import the component under test
import BurnoutSummary from './BurnoutSummary';

// --- Test Suite ---
describe('BurnoutSummary', () => {
  beforeEach(() => {
    // Reset mocks
    mockLocationState = {};
    mockNavigate.mockClear();
  });

  it('redirects to assessment when no result in location.state', async () => {
    // Arrange: no result provided
    mockLocationState = {};

    // Act: render the component
    render(<BurnoutSummary />);

    // Assert: effect should navigate back
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/burnout-assessment');
    });
  });

  it('renders summary and handles restart click', async () => {
    // Arrange: provide result
    mockLocationState = { result: { type: 'foo', sessionId: 'sess1', score: 42, summary: 'You did great' } };

    // Act: render component
    render(<BurnoutSummary />);

    // Expect header
    expect(await screen.findByText('Assessment Summary')).toBeInTheDocument();
    // Score displayed
    expect(screen.getByText('Score: 42')).toHaveTextContent(/score: 42/i);
    // Summary text
    expect(screen.getByText('You did great')).toBeInTheDocument();

    // Restart button present
    const btn = screen.getByRole('button', { name: /restart assessment/i });
    expect(btn).toBeInTheDocument();

    // Act: click restart
    fireEvent.click(btn);

    // Assert: navigate called to assessment
    expect(mockNavigate).toHaveBeenCalledWith('/burnout-assessment');
  });
});