import '@testing-library/jest-dom';
/// <reference types="vitest" />
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import TextQuestion from './TextQuestion';
import type { BurnoutQuestion } from '../types/burnout/assessment';

describe('TextQuestion Component', () => {
  it('renders a textarea with correct placeholder and styling', () => {
    const question: BurnoutQuestion = { questionId: 10, question: 'Describe your mood', multimodal: false, domain: 'emotional' };
    const onChange = vi.fn();

    render(<TextQuestion question={question} onChange={onChange} />);

    const textarea = screen.getByPlaceholderText('Type your answer here...');
    expect(textarea).toBeInTheDocument();
    expect(textarea).toHaveClass('textarea');
    expect(textarea).toHaveAttribute('rows', '6');
  });

  it('calls onChange with questionId and text value when typing', () => {
    const question: BurnoutQuestion = { questionId: 20, question: 'Any thoughts?', multimodal: false, domain: 'cognitive' };
    const onChange = vi.fn();

    render(<TextQuestion question={question} onChange={onChange} />);

    const textarea = screen.getByPlaceholderText('Type your answer here...');
    fireEvent.change(textarea, { target: { value: 'Hello world' } });

    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledWith(20, 'Hello world');
  });
});
