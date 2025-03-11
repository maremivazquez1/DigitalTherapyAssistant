/**
 * speechAPI.ts
 * 
 * This file provides API functions for handling speech-related operations in the CBT session.
 * It includes:
 * - Speech-to-Text (STT): Converts user speech into text using an external API (e.g., ElevenLabs, HumeAI).
 * - Text-to-Speech (TTS): Converts AI-generated text into spoken audio.
 * 
 * Requirements:
 * - Axios must be installed (`npm install axios`).
 * - The backend API should be running and accessible at the specified `API_BASE_URL`.
 * - Ensure the `audioBlob` or `text` is correctly provided before making API calls.
 * 
 * Functions:
 * - processSpeech(audioBlob: Blob): Sends recorded audio to the backend for STT processing.
 * - convertTextToSpeech(text: string): Converts AI-generated text into speech audio.
 */

import axios from "axios";

const API_BASE_URL = "https://api.yourapp.com/speech";

export const processSpeech = async (audioBlob: Blob) => {
    try {
        const formData = new FormData();
        formData.append("audio", audioBlob);

        const response = await axios.post(`${API_BASE_URL}/stt`, formData, {
            headers: { "Content-Type": "multipart/form-data" },
        });

        return response.data;
    } catch (error) {
        console.error("Error processing speech:", error);
        throw error;
    }
};

export const convertTextToSpeech = async (text: string) => {
    try {
        const response = await axios.post(`${API_BASE_URL}/tts`, { text });

        return response.data; // Should return an audio file URL
    } catch (error) {
        console.error("Error converting text to speech:", error);
        throw error;
    }
};
