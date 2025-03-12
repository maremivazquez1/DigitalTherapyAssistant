// src/services/authService.ts
import api from './axiosConfig';
import { RegisterData, LoginData, AuthResponse } from '../types/auth/auth';

export const register = async (userData: RegisterData): Promise<AuthResponse> => {
  try {
    const response = await api.post<AuthResponse>('/auth/register', userData);
    return response.data;
  } catch (error: any) {
    // Customize error handling; you can type error if you have a specific type
    throw error.response?.data || error.message;
  }
};

export const login = async (credentials: LoginData): Promise<AuthResponse> => {
  try {
    const response = await api.post<AuthResponse>('/auth/login', credentials);
    localStorage.setItem('token', response.data.token);
    return response.data;
  } catch (error: any) {
    throw error.response?.data || error.message;
  }
};
