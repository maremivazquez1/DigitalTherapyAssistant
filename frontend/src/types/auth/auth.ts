// src/types/auth.ts

// Request payloads
export interface RegisterData {
    first_name: string;
    last_name: string;
    email: string;
    password: string;
    confirm_password: string;
    phone: string;
    date_of_birth: string;
  }
  
  export interface LoginData {
    email: string;
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
  