import '@testing-library/jest-dom';
/// <reference types="vitest" />
import { render } from '@testing-library/react';
import { describe, it } from 'vitest';
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
