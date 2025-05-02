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
    // Store token, sessionId, and userId in localStorage
    localStorage.setItem('token', response.data.token);
    localStorage.setItem('sessionId', response.data.sessionId);
    localStorage.setItem('userId', response.data.userId);
    return response.data;
  } catch (error: unknown) {
    return Promise.reject(error); // Ensure a rejected Promise is returned
  }
};

export const logout = (): void => {
  localStorage.removeItem('token');
  localStorage.removeItem('sessionId');
  localStorage.removeItem('userId');
};

/**
 * Retrieves the authentication token from local storage.
 */
export const getAuthToken = (): string | null => {
  return localStorage.getItem('token');
};

export const getSessionId = (): string | null => {
  return localStorage.getItem('sessionId');
};

export const getUserId = (): string | null => {
  return localStorage.getItem('userId');
};

/**
 * Checks if a user is authenticated by verifying if a token exists.
 */
export const isAuthenticated = (): boolean => {
  const token = localStorage.getItem('token');
  return !!token && token !== 'undefined';
};
