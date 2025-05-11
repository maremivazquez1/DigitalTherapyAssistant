import '@testing-library/jest-dom';
/// <reference types="vitest" />
import { render, screen } from '@testing-library/react';
import { describe, it, vi } from 'vitest';

// Stub the actual summary component
vi.mock('../components/BurnoutSummary', () => ({
  default: () => <div data-testid="burnout-summary">Mock Summary</div>,
}));

import BurnoutSummaryPage from './BurnoutSummaryPage';

describe('BurnoutSummaryPage', () => {
  it('renders the BurnoutSummary component', () => {
    render(<BurnoutSummaryPage />);

    // Verify that our stubbed summary appears
    const summary = screen.getByTestId('burnout-summary');
    expect(summary).toBeInTheDocument();
    expect(summary).toHaveTextContent('Mock Summary');
  });
});
