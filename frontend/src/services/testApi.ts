// This file contains a service function to test the connection to the backend API.
// It sends a GET request to the `/home` endpoint and logs the response.

import api from './config/axiosConfig';

/**
 * Sends a GET request to the backend `/home` endpoint to test connectivity.
 * Logs the response and returns the response data.
 * Returns null if an error occurs.
 */
export const testConnection = async () => {
  // Attempt to send a request to the backend and retrieve the response.
  try {
    const response = await api.get('/home'); // Change `/test` to a real backend endpoint
    console.log('Backend Response:', response.data);
    return response.data;
  } catch (error) {
    // Log and handle connection errors by returning null.
    console.error('Error connecting to backend:', error);
    return null;
  }
};