import '@testing-library/jest-dom';
/// <reference types="vitest" />
import { render, screen } from '@testing-library/react';
import { describe, it, vi } from 'vitest';

// Mock the CBTInterface component
vi.mock('../components/CBTInterface', () => ({
  default: () => <div data-testid="cbt-interface">Mock CBT Interface</div>,
}));

import CBTPage from './CBTPage';

describe('CBTPage', () => {
  it('renders the CBTInterface component', () => {
    render(<CBTPage />);
    const cbt = screen.getByTestId('cbt-interface');
    expect(cbt).toBeInTheDocument();
    expect(cbt).toHaveTextContent('Mock CBT Interface');
  });
});
