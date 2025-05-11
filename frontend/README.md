# Frontend
This repository contains the frontend application of the Digital Therapy Assistant project, built using React, TypeScript, and Vite.

## Project Overview
This project is a Digital Therapy Assistant, designed to provide users with a platform for mental health assessments and Cognitive Behavioral Therapy (CBT) sessions. The frontend is built using React and TypeScript, ensuring a modern and efficient user experience.
The application includes features such as user authentication, a burnout assessment questionnaire, and a CBT interface for real-time interaction with a virtual therapist. The frontend communicates with the backend via REST APIs and WebSocket for real-time communication.

## Prerequisites
- Node.js (v16 or later)
- npm (v8 or later)
- Vite (v4 or later)
- React (v18 or later)
- TypeScript (v4 or later)

## Getting Started
To get started with the project, follow these steps:
1. Clone the repository to your local machine.
2. Navigate to the project directory.
3. Install the required dependencies using npm:
   ```bash
   npm install
   ```
4. Start the development server:
   ```bash
   npm run dev
   ```
5. Open your browser and navigate to `http://localhost:5173` (or the port specified in your Vite configuration).
6. You should see the Digital Therapy Assistant application running.
7. You can now explore the application, including the login and registration features, burnout assessment, and CBT sessions.

## Folder Structure
src
├── App.css
├── App.tsx
├── assets
│   ├── harvard.wav
│   ├── react.svg
│   ├── therapy-room-1.png
│   ├── therapy-room-1.svg
│   └── therapy-room-2.png
├── components
│   ├── BurnoutAssessment.tsx
│   ├── BurnoutSummary.tsx
│   ├── CBTInterface.test.tsx
│   ├── CBTInterface.tsx
│   ├── Dashboard.tsx
│   ├── LikertQuestion.tsx
│   ├── LoginForm.test.tsx
│   ├── LoginForm.tsx
│   ├── NavBar.tsx
│   ├── PrivateRoute.tsx
│   ├── ProtectedRoute.tsx
│   ├── RegistrationForm.test.tsx
│   ├── RegistrationForm.tsx
│   ├── TextQuestion.tsx
│   └── VlogQuestion.tsx
├── context
│   └── AuthContext.tsx
├── hooks
│   └── useWebSocket.ts
├── index.css
├── main.tsx
├── pages
│   ├── BurnoutAssessmentPage.tsx
│   ├── BurnoutSummaryPage.tsx
│   ├── CBTPage.tsx
│   ├── DashboardPage.tsx
│   ├── LoginPage.tsx
│   └── RegisterPage.tsx
├── services
│   ├── auth
│   │   └── authService.ts
│   ├── axiosConfig.ts
│   ├── config
│   │   └── axiosConfig.ts
│   ├── testApi.ts
│   └── useCBTWebSocket.ts
├── setup.ts
├── store
│   └── store.ts
├── types
│   ├── CBTSession
│   │   ├── chat.ts
│   │   ├── preferences.ts
│   │   ├── session.ts
│   │   └── webSocketMessage.ts
│   ├── auth
│   │   └── auth.ts
│   └── burnout
│       └── assessment.ts
└── vite-env.d.ts

## File Details
### src/components
This directory contains the main React components used in the application. Each component is responsible for a specific part of the user interface.
- **BurnoutAssessment.tsx**: Component for the burnout assessment questionnaire flow.
- **BurnoutSummary.tsx**: Displays results and recommendations after assessment.
- **CBTInterface.tsx**: Main interface for CBT sessions, handling user input and AI responses.
- **Dashboard.tsx**: Landing dashboard showing user overview and navigation.
- **LikertQuestion.tsx**: Renders a Likert scale question for assessments.
- **TextQuestion.tsx**: Renders an open-ended text question.
- **VlogQuestion.tsx**: Renders a video-log question interface.
- **LoginForm.tsx**: Form component for user login.
- **RegistrationForm.tsx**: Form component for user registration.
- **NavBar.tsx**: Top navigation bar, shows login/logout and navigation links.
- **ProtectedRoute.tsx**: Wrapper ensuring only authenticated users can access certain routes.
- **PrivateRoute.tsx**: Alias or similar to ProtectedRoute (describe difference if any).
### src/context
This directory contains the React Context API files used for managing global state across the application.
- **AuthContext.tsx**: React Context for authentication state (login, logout, token) across the app.
### src/hooks
This directory contains custom React hooks used in the application.
- **useWebSocket.ts**: Custom React hook to manage WebSocket connections for real-time CBT.
### src/pages
This directory contains the main page components that wrap and orchestrate the various components.
- **DashboardPage.tsx**: Page container for the dashboard view.
- **LoginPage.tsx**: Page container for login, uses LoginForm.
- **RegisterPage.tsx**: Page container for registration, uses RegistrationForm.
- **CBTPage.tsx**: Page container for CBT sessions, uses CBTInterface.
- **BurnoutAssessmentPage.tsx**: Page wrapping BurnoutAssessment component.
- **BurnoutSummaryPage.tsx**: Page wrapping BurnoutSummary component.
### src/services
This directory contains the service files responsible for API calls and configurations.
- **auth/authService.ts**: API calls for login, logout, register, and token handling.
- **axiosConfig.ts**: Axios instance configuration (base URL, interceptors).
- **useCBTWebSocket.ts**: Hook or utility for opening CBT WebSocket connection.
### src/store
This directory contains the Redux store configuration for global state management.
- **store.ts**: Centralized Redux store configuration for global state.
### src/types
This directory contains TypeScript interfaces and types used throughout the application.
- **auth/**: Contains types related to authentication (e.g., User, Credentials).
- **burnout/**: Contains types for the burnout questionnaire and results.
- **CBTSession/**: Contains types related to CBT sessions, including chat messages, user preferences, and session metadata.
- **webSocketMessage.ts**: Message formats over WebSocket.
### src/assets
This directory contains static files such as images and audio used in the application.
- **react.svg**: React logo.
- **harvard.wav**: Test sound.
- **therapy-room-1.png/svg**: Backgrounds or illustrations.
- **therapy-room-2.png**: Backgrounds or illustrations.
### Root Files
- **App.tsx**: Main app component, renders NavBar and sets up routes.
- **main.tsx**: Entry point, renders `<App />` into the DOM.
- **index.css**: Global styles for the application.
- **App.css**: Component-specific styles for the App component.
- **setup.ts**: Test setup (e.g., Jest, testing-library config).
- **vite-env.d.ts**: Vite environment type definitions.

## Testing
This project uses Vitest and React Testing Library for unit and integration testing. The tests are located in the same directory as the components they test, with a `.test.tsx` suffix.
To run the tests, use the following command:
```bash
npm run test
```