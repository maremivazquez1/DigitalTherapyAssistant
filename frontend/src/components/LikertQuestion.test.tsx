import '@testing-library/jest-dom';
/// <reference types="vitest" />
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import LikertQuestion from './LikertQuestion';
import type { BurnoutQuestion } from '../types/burnout/assessment';

// Shared options from component
const LIKERT_OPTIONS = ['Never', 'Rarely', 'Sometimes', 'Often', 'Always'];

describe('LikertQuestion Component', () => {
  const baseQuestion: BurnoutQuestion = { questionId: 1, question: 'Test?' } as any;

  it('renders all likert options as buttons', () => {
    render(<LikertQuestion question={baseQuestion} onChange={() => {}} />);
    const buttons = screen.getAllByRole('button');
    expect(buttons).toHaveLength(LIKERT_OPTIONS.length);
    LIKERT_OPTIONS.forEach((opt, idx) => {
      expect(buttons[idx]).toHaveTextContent(opt);
      // default to outline style
      expect(buttons[idx]).toHaveClass('btn-outline');
      expect(buttons[idx]).not.toHaveClass('btn-active');
    });
  });

  it('calls onChange and applies active style when an option is clicked', () => {
    const onChange = vi.fn();
    render(<LikertQuestion question={baseQuestion} onChange={onChange} />);
    const button = screen.getByText('Sometimes');
    fireEvent.click(button);
    expect(onChange).toHaveBeenCalledWith(1, 'Sometimes');
    // After click, that button should have active styling
    expect(button).toHaveClass('btn-active', 'btn-ghost');
    // Others remain outline
    LIKERT_OPTIONS.filter(o => o !== 'Sometimes').forEach(o => {
      expect(screen.getByText(o)).toHaveClass('btn-outline');
    });
  });

  it('resets selection when questionId changes', () => {
    const { rerender } = render(<LikertQuestion question={baseQuestion} onChange={() => {}} />);
    // click one
    fireEvent.click(screen.getByText('Rarely'));
    expect(screen.getByText('Rarely')).toHaveClass('btn-active', 'btn-ghost');
    // rerender with new questionId
    const newQuestion = { questionId: 2, question: 'Another?' } as any;
    rerender(<LikertQuestion question={newQuestion} onChange={() => {}} />);
    // previous selection cleared
    LIKERT_OPTIONS.forEach(opt => {
      expect(screen.getByText(opt)).toHaveClass('btn-outline');
      expect(screen.getByText(opt)).not.toHaveClass('btn-active');
    });
  });
});
