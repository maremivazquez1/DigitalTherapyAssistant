/**
 * speechUtils.ts
 * 
 * This file provides helper functions for handling speech-related operations in the CBT session.
 * It supports:
 * - Validating audio formats before processing.
 * - Converting audio blobs to Base64 for API requests.
 * - Estimating speech duration based on text input.
 * - Playing generated speech from a URL.
 * 
 * Functions:
 * - isValidAudioFormat(file: File): Checks if the audio file format is supported.
 * - convertAudioBlobToBase64(blob: Blob): Converts an audio Blob to a Base64 string.
 * - getEstimatedSpeechDuration(text: string): Estimates the playback duration of generated speech.
 * - playAudioFromUrl(url: string): Plays AI-generated speech from a given URL.
 */

export const isValidAudioFormat = (file: File): boolean => {
    const allowedFormats = ["audio/mp3", "audio/wav", "audio/mpeg"];
    return allowedFormats.includes(file.type);
};

export const convertAudioBlobToBase64 = (blob: Blob): Promise<string> => {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.readAsDataURL(blob);
        reader.onloadend = () => {
            if (typeof reader.result === "string") {
                resolve(reader.result.split(",")[1]); // Extract Base64 part
            } else {
                reject("Failed to convert blob to Base64.");
            }
        };
        reader.onerror = reject;
    });
};

export const getEstimatedSpeechDuration = (text: string): number => {
    const wordsPerMinute = 150; // Approximate human speech speed
    const wordCount = text.split(/\s+/).length;
    return Math.round((wordCount / wordsPerMinute) * 60); // Returns duration in seconds
};

export const playAudioFromUrl = (url: string): void => {
    const audio = new Audio(url);
    audio.play().catch(error => console.error("Error playing audio:", error));
};