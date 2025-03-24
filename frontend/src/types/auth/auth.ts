// src/types/auth.ts

// Request payloads
export interface RegisterData {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    phone: string;
    dateOfBirth: string;
  }
  
  export interface LoginData {
    username: string;
    password: string;
  }
  
  // Response payloads (adjust based on your API contract)
  export interface AuthResponse {
    token: string;
    status: 'success' | 'error';
    message: string;
  }
  
  // In case errors are structured:
  export interface ApiError {
    message: string;
    // Add more fields if your API returns more info
  }