import '@testing-library/jest-dom';
/// <reference types="vitest" />
import { render, screen } from '@testing-library/react';
import { describe, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import RegisterPage from './RegisterPage';

describe('RegistrationPage', () => {
  it('renders the RegisterForm component', () => {
    render(
      <MemoryRouter>
        <RegisterPage />
      </MemoryRouter>
    );
  });
});
