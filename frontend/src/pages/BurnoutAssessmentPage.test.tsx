import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';             // ← add this import
import { describe, it, vi } from 'vitest';

// Stub out the actual assessment—we’ve already tested it thoroughly
vi.mock('../components/BurnoutAssessment', () => ({
  default: () => <div data-testid="burnout-assessment">Mock Assessment</div>,
}));

import BurnoutAssessmentPage from './BurnoutAssessmentPage';

describe('BurnoutAssessmentPage', () => {
  it('renders the assessment inside its page container', () => {
    render(<BurnoutAssessmentPage />);

    // or if you really want to use a class selector:
    expect(document.querySelector('.page-container')).not.toBeNull();

    // fix the matcher name and syntax here:
    expect(screen.getByTestId('burnout-assessment')).toHaveTextContent('Mock Assessment');
  });
});
