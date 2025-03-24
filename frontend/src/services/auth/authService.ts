// src/services/authService.ts
/**
 * authService.ts
 * 
 * This file handles user authentication services, including registration, login, 
 * logout, token management, and authentication status checks.
 * 
 * Responsibilities:
 * - Register new users (`register`).
 * - Authenticate users and store tokens (`login`).
 * - Logout users by clearing authentication data (`logout`).
 * - Retrieve authentication tokens (`getAuthToken`).
 * - Check if a user is authenticated (`isAuthenticated`).
 * - Refresh authentication tokens if supported (`refreshToken`).
 * - Handle authentication-related errors (`handleAuthError`).
 */

import api from '../config/axiosConfig';
import { RegisterData, LoginData, AuthResponse } from '../../types/auth/auth';

export const register = async (userData: RegisterData): Promise<AuthResponse> => {
  try {
    const response = await api.post<AuthResponse>('/register', userData);
    return response.data;
  } catch (error: unknown) {
    return Promise.reject(error); // Ensure a rejected Promise is returned
  }
};

export const login = async (credentials: LoginData): Promise<AuthResponse> => {
  try {
    const response = await api.post<AuthResponse>('/login', credentials);
    localStorage.setItem('token', response.data.token);
    return response.data;
  } catch (error: unknown) {
    return Promise.reject(error); // Ensure a rejected Promise is returned
  }
};

export const logout = (): void => {
  localStorage.removeItem('token');
};

/**
 * Retrieves the authentication token from local storage.
 */
export const getAuthToken = (): string | null => {
  return localStorage.getItem('token');
};

/**
 * Checks if a user is authenticated by verifying if a token exists.
 */
export const isAuthenticated = (): boolean => {
  return !!localStorage.getItem('token');
};

/**
 * Attempts to refresh the authentication token.
 * If the refresh fails, the user is logged out.
 */
export const refreshToken = async (): Promise<AuthResponse | null> => {
  try {
    const response = await api.post<AuthResponse>('/auth/refresh');
    localStorage.setItem('token', response.data.token);
    return response.data;
  } catch (error: unknown) {
    logout(); // Logout if refresh fails
    handleAuthError(error);
    return null;
  }
};

/**
 * Centralized error handling for authentication requests.
 */
const handleAuthError = (error: unknown): string => {
  if (error instanceof Error) {
    return error.message;
  } else if (typeof error === "object" && error !== null && "response" in error) {
    const axiosError = error as { response?: { data?: { message?: string } } };
    return axiosError.response?.data?.message || "Authentication error";
  } else {
    return "An unknown authentication error occurred";
  }
};
