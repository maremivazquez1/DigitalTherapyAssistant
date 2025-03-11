

# API Documentation - Digital Therapy Assistant
 
## Base URL
- **Production:** `https://api.yourapp.com`
- **Development:** `http://localhost:8080`
 
---
 
## **1️⃣ Chat API**
 
### ➤ Send a Message
- **Endpoint:** `POST /chat/send-message`
- **Description:** Sends a user message to the AI for processing.
- **Request Body:**
  ```json
  {
    "sessionId": "abc123",
    "message": "Hello, how are you?"
  }
  ```
- **Response:**
  ```json
  {
    "id": "msg001",
    "sessionId": "abc123",
    "role": "ai",
    "content": "I'm here to help!",
    "timestamp": "2025-03-11T12:34:56Z"
  }
  ```
 
### ➤ Get Chat History
- **Endpoint:** `GET /chat/history/{sessionId}`
- **Description:** Fetches all past messages in a given session.
- **Response:**
  ```json
  {
    "sessionId": "abc123",
    "messages": [
      {
        "id": "msg001",
        "role": "user",
        "content": "Hello!",
        "timestamp": "2025-03-11T12:30:00Z"
      },
      {
        "id": "msg002",
        "role": "ai",
        "content": "Hi there! How can I assist you?",
        "timestamp": "2025-03-11T12:30:05Z"
      }
    ]
  }
 
---
 
## **2️⃣ Session API**
 
### ➤ Start a New Session
- **Endpoint:** `POST /session/start`
- **Description:** Creates a new CBT session for a user.
- **Request Body:**
  ```json
  {
    "userId": "user123"
  }
  ```
- **Response:**
  ```json
  {
    "sessionId": "abc123",
    "userId": "user123",
    "status": "active",
    "createdAt": "2025-03-11T12:00:00Z"
  }
  ```
 
### ➤ Get Session Details
- **Endpoint:** `GET /session/retrieve/{sessionId}`
- **Description:** Retrieves details of an active session.
- **Response:**
  ```json
  {
    "sessionId": "abc123",
    "userId": "user123",
    "status": "active",
    "createdAt": "2025-03-11T12:00:00Z"
  }
  ```
 
### ➤ End a Session
- **Endpoint:** `DELETE /session/end/{sessionId}`
- **Description:** Ends an active CBT session.
- **Response:**
  ```json
  {
    "success": true,
    "message": "Session ended successfully."
  }
  ```
 
---
 
## **3️⃣ Speech API**
 
### ➤ Process Speech-to-Text (STT)
- **Endpoint:** `POST /speech/stt`
- **Description:** Converts recorded speech to text.
- **Request Body:**
  - **Content-Type:** `multipart/form-data`
  - **File:** Audio file (MP3/WAV)
- **Response:**
  ```json
  {
    "text": "Hello, how can I assist you today?"
  }
  ```
 
### ➤ Convert Text-to-Speech (TTS)
- **Endpoint:** `POST /speech/tts`
- **Description:** Converts AI-generated text to speech.
- **Request Body:**
  ```json
  {
    "text": "Hello, how can I assist you today?"
  }
  ```
- **Response:**
  ```json
  {
    "audioUrl": "https://api.yourapp.com/audio/file123.mp3"
  }
  ```
 
---
 
## **4️⃣ WebSocket API**
 
### ➤ WebSocket Connection
- **URL:** `wss://api.yourapp.com/chat/stream`
- **Description:** Enables real-time chat with the AI.
- **Message Format:**
  ```json
  {
    "type": "chat",
    "sessionId": "abc123",
    "content": "Hello!"
  }
  ```
- **Response:**
  ```json
  {
    "type": "chat",
    "sessionId": "abc123",
    "content": "Hi there! How can I assist you?"
  }
  ```
 
---
 
## **5️⃣ User Preferences API**
 
### ➤ Save Preferences
- **Endpoint:** `POST /preferences/save`
- **Description:** Stores user preferences such as language and voice settings.
- **Request Body:**
  ```json
  {
    "userId": "user123",
    "enableVoice": true,
    "language": "en",
    "responseSpeed": "normal"
  }
  ```
- **Response:**
  ```json
  {
    "success": true,
    "message": "Preferences saved."
  }
  ```
 
### ➤ Load Preferences
- **Endpoint:** `GET /preferences/load/{userId}`
- **Description:** Retrieves user preferences.
- **Response:**
  ```json
  {
    "userId": "user123",
    "enableVoice": true,
    "language": "en",
    "responseSpeed": "normal"
  }
  ```