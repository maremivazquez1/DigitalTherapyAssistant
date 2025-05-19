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

export interface AuthResponse {
    status: string;
    message: string;
    token: string;
    sessionId: string;
    userId: string;
}
  
// In case errors are structured:
export interface ApiError {
    message: string;
}