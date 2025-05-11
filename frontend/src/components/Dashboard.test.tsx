import '@testing-library/jest-dom';
/// <reference types="vitest" />
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Dashboard from './Dashboard';

describe('Dashboard Component', () => {
  beforeEach(() => {
    // Render component inside MemoryRouter for Link support
    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    );
  });

  it('renders header title and subtitle', () => {
    // Main title
    const title = screen.getByRole('heading', { level: 1, name: /Dashboard/i });
    expect(title).toBeInTheDocument();

    // Subtitle text
    expect(
      screen.getByText(/Welcome back! Select an option below to continue\./i)
    ).toBeInTheDocument();
  });

  it('links Start a CBT Session card to /cbt', () => {
    const cbtLink = screen.getByRole('link', { name: /Start a CBT Session/i });
    expect(cbtLink).toBeInTheDocument();
    expect(cbtLink).toHaveAttribute('href', '/cbt');
  });

  it('links Take a Burnout Assessment card to /burnout', () => {
    const burnLink = screen.getByRole('link', { name: /Take a Burnout Assessment/i });
    expect(burnLink).toBeInTheDocument();
    expect(burnLink).toHaveAttribute('href', '/burnout');
  });

  /* it('renders disabled mood card', () => {
    const moodHeading = screen.getByText(/Check on Your Mood/i);
    // The card container has opacity-50 and cursor-not-allowed
    const moodCard = moodHeading.closest('div')!.closest('div')!;
    expect(moodCard).toHaveClass('opacity-50');
    expect(moodCard).toHaveClass('cursor-not-allowed');
    // It should not be a link
    expect(
      screen.queryByRole('link', { name: /Check on Your Mood/i })
    ).toBeNull();
  });

  it('renders disabled journal card', () => {
    const journalHeading = screen.getByText(/Write a Guided Journal Entry/i);
    const journalCard = journalHeading.closest('div')!.closest('div')!;
    expect(journalCard).toHaveClass('opacity-50');
    expect(journalCard).toHaveClass('cursor-not-allowed');
    // It should not be a link
    expect(
      screen.queryByRole('link', { name: /Write a Guided Journal Entry/i })
    ).toBeNull();
  }); */
});