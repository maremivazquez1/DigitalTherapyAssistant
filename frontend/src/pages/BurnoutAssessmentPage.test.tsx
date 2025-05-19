import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';

// Stub out the assessment
vi.mock('../components/BurnoutAssessment', () => ({
  default: () => <div data-testid="burnout-assessment">Mock Assessment</div>,
}));

import BurnoutAssessmentPage from './BurnoutAssessmentPage';

describe('BurnoutAssessmentPage', () => {
  it('renders the assessment inside its page container', () => {
    render(
      <MemoryRouter>
        <BurnoutAssessmentPage />
      </MemoryRouter>
    );

    expect(document.querySelector('.page-container')).not.toBeNull();

    expect(screen.getByTestId('burnout-assessment')).toHaveTextContent('Mock Assessment');
  });
});
