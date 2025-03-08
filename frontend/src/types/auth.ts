// src/types/auth.ts

// Request payloads
export interface RegisterData {
    username: string;
    email: string;
    password: string;
  }
  
  export interface LoginData {
    email: string;
    password: string;
  }
  
  // Response payloads (adjust based on your API contract)
  export interface AuthResponse {
    token: string;
    user: {
      id: number;
      username: string;
      email: string;
    };
  }
  
  // In case errors are structured:
  export interface ApiError {
    message: string;
    // Add more fields if your API returns more info
  }
  