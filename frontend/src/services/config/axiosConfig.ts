// src/services/axiosConfig.ts
/**
 * axiosConfig.ts
 * 
 * This file sets up and configures the global Axios instance for making API requests.
 * It ensures all requests use a consistent base URL, handles authentication, and 
 * provides global error handling.
 * 
 * Purpose:
 * - Centralizes API configuration so that all requests use the same Axios instance.
 * - Automatically attaches authentication tokens to requests (if available).
 * - Implements global error handling to catch authentication failures.
 * 
 * What This File Should Do:
 * - Create and export an Axios instance with a predefined `baseURL`.
 * - Attach request interceptors to:
 *   - Add authentication tokens to each request.
 *   - Modify requests before they are sent.
 * - Attach response interceptors to:
 *   - Handle global error cases (e.g., unauthorized access, API failures).
 *   - Provide a way to handle authentication failures and log users out if needed.
 * 
 * What Can Be Added in the Future:
 * - Token refresh mechanism to extend user sessions without requiring re-login.
 * - Logging system for tracking API requests and errors.
 * - Request timeout handling to prevent excessive waiting on failed requests.
 * - Automatic request cancellation when components unmount.
 * - Support for additional custom headers if needed.
 */

import axios from 'axios';

const baseURL = ("http://" + import.meta.env.VITE_API_BASE_URL) || '/api';

const api = axios.create({
  baseURL,
});

// Optional: Add a request interceptor to attach an auth token if available
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Optional: Global error handling in a response interceptor
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // For instance, clear token or redirect to login
      // localStorage.removeItem('token');
      // window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
