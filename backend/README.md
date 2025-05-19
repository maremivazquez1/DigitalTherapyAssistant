# Backend
This backend is a Spring Boot microservice written in Java and built with Maven. 
It powers the core logic for a digital therapy assistant application.

## Stack Overview
- **Language:** Java 17
- **Framework:** Spring Boot
- **Build Tool:** Maven (`mvn`)
- **Database:** MySQL
- **Cache:** Redis
- **AI Integration:** LangChain4j + LLMs (for dialogue, sentiment, and burnout analysis)
   - **AWS Polly:** TTS service to transcribe LLM response to user as audio
   - **AWS Rekognition:** Video facial analysis service
   - **HUME:** Audio intonation analysis service
   - **GEMINI:** CBT message worker, CBT multi-modal synthesizer, Burnout assessment

## Folder Structure
src
├── main
│   ├── java\harvard\capstone\digitaltherapy
│   │   ├── authentication
│   │   │   ├── controller
│   │   │   │   ├── UserLoginController.java
│   │   │   │   └── UserRegistrationController.java
│   │   │   ├── exception
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── UserAlreadyExistsException.java
│   │   │   ├── model
│   │   │   │   ├── ApiResponse.java
│   │   │   │   ├── LoginRequest.java
│   │   │   │   └── Users.java
│   │   │   └── service
│   │   │       ├── JwtHandshakeInterceptor.java
│   │   │       ├── JwtTokenProvider.java
│   │   │       ├── PasswordEncoderService.java
│   │   │       ├── TokenService.java
│   │   │       ├── UserLoginService.java
│   │   │       └── UserRegistrationService.java
│   │   ├── aws
│   │   │   ├── config
│   │   │   │   └── AwsConfig.java
│   │   │   └── service
│   │   │       ├── PollyService.java
│   │   │       ├── RekognitionService.java
│   │   │       └── TranscribeService.java
│   │   ├── burnout
│   │   │   ├── ai
│   │   │   │   ├── BurnoutScoreCalculator.java
│   │   │   │   ├── BurnoutSummaryGenerator.java
│   │   │   │   ├── MultimodalQuestionGenerator.java
│   │   │   │   └── StandardQuestionGenerator.java
│   │   │   ├── controller
│   │   │   │   └── BurnoutController.java
│   │   │   ├── fhir
│   │   │   │   └── BurnoutAssessmentFhirConverter.java
│   │   │   ├── model
│   │   │   │   ├── AssessmentDomain.java
│   │   │   │   ├── BurnoutAssessment.java
│   │   │   │   ├── BurnoutAssessmentResult.java
│   │   │   │   ├── BurnoutAssessmentSession.java
│   │   │   │   ├── BurnoutQuestion.java
│   │   │   │   ├── BurnoutScore.java
│   │   │   │   ├── BurnoutSessionCreationResponse.java
│   │   │   │   ├── BurnoutSummary.java
│   │   │   │   └── BurnoutUserResponse.java
│   │   │   ├── orchestration
│   │   │   │   └── BurnoutAssessmentOrchestrator.java
│   │   │   ├── service
│   │   │   │   └── BurnoutFhirService.java
│   │   │   └── workers
│   │   │       └── BurnoutWorker.java
│   │   ├── cbt
│   │   │   ├── controller
│   │   │   │   └── CBTController.java
│   │   │   ├── model
│   │   │   │   └── AnalysisResult.java
│   │   │   └── service
│   │   │       ├── CBTHelper.java
│   │   │       └── OrchestrationService.java
│   │   ├── config
│   │   │   ├── JwtTokenFilter.java
│   │   │   ├── RedisConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── WebSocketConfig.java
│   │   ├── llm
│   │   │   ├── config
│   │   │   │   └── BedrockConfig.java
│   │   │   └── service
│   │   │       ├── BedrockService.java
│   │   │       ├── LLMProcessingService.java
│   │   │       └── S3StorageService.java
│   │   ├── persistence
│   │   │   └── VectorDatabaseService.java
│   │   ├── utility
│   │   │   └── S3Utils.java
│   │   ├── websocket
│   │   │   ├── BurnoutWebSocketHandler.java
│   │   │   └── CBTWebSocketHandler.java
│   │   ├── workers
│   │   │   ├── AudioAnalysisWorker.java
│   │   │   ├── MessageWorker.java
│   │   │   ├── MultiModalSynthesizer.java
│   │   │   ├── TextAnalysisWorker.java
│   │   │   └── VideoAnalysisWorker.java
│   │   └── CbtApplication.java
│   └── resources
│       ├── static
│       │   └── websocket-cbt-audio.html
│       └── application.properties
└── test

## File Details
### src\main\java\harvard\capstone\digitaltherapy\
- **CbtApplication.java**: Main Spring Boot application entry point that starts the service.
#### authentication\
##### controller\
- **UserLoginController.java**: Handles user login requests and token issuance.
- **UserRegistrationController.java**: Manages new user registration and validation.
##### exception\
- **GlobalExceptionHandler.java**: Centralized handler for application-wide exceptions.
- **UserAlreadyExistsException.java**: Custom exception thrown when a duplicate user is detected.
##### model\
- **ApiResponse.java**: Generic wrapper for standard API response messages.
- **LoginRequest.java**: DTO containing user login credentials.
- **Users.java**: JPA entity representing user account data.
##### service\
- **JwtHandshakeInterceptor.java**: Intercepts WebSocket handshakes to validate JWTs.
- **JwtTokenProvider.java**: Utility for creating and validating JWT tokens.
- **PasswordEncoderService.java**: Encapsulates password hashing and verification.
- **TokenService.java**: Manages creationand management of access tokens.
- **UserLoginService.java**: Logic for verifying user login credentials.
- **UserRegistrationService.java**: Handles persistence and uniqueness checks during registration.
#### aws\
##### config\
- **AwsConfig.java**: Configures AWS clients for service integrations.
##### service\
- **PollyService.java**: Interfaces with AWS Polly for text-to-speech synthesis.
- **RekognitionService.java**: Interfaces with AWS Rekognition for video/image emotion analysis.
- **TranscribeService.java**: Interfaces with AWS Transcribe for speech-to-text transcription.
#### burnout\
##### ai\
- **BurnoutScoreCalculator.java**: Computes burnout score from user responses and model output.
- **BurnoutSummaryGenerator.java**: Generates a summary based on burnout assessment data.
- **MultimodalQuestionGenerator.java**: Produces burnout questions with audio/video context.
- **StandardQuestionGenerator.java**: Generates evaluation burnout self-assessment questions.
##### controller\
- **BurnoutController.java**: Manages burnout assessment endpoints and workflows. Communicates between the websocket and Burnout orchestration.
##### fhir\
- **BurnoutAssessmentFhirConverter.java**: Converts burnout results into FHIR-compliant format.
##### model\
- **AssessmentDomain.java**: Enum defining burnout question domains (Work, Personal, Lifestyle)
- **BurnoutAssessment.java**: Represents a full burnout assessment instance. Contains a collection of questions associated with a Burnout session.
- **BurnoutAssessmentResult.java**: Captures computed results of a burnout assessment.
- **BurnoutAssessmentSession.java**: Tracks ongoing or completed assessment sessions.
- **BurnoutQuestion.java**: Represents an individual burnout-related question.
- **BurnoutScore.java**: Model holding scores and explanations for assessed Burnout risk.
- **BurnoutSessionCreationResponse.java**: Response object for the creation of a new burnout assessment session. Contains the session ID and the list of questions to be presented to the user.
- **BunroutSummary.java**: Contains a narrative summary of a user’s burnout state.
- **BurnoutUserResponse.java**: Stores user responses to burnout questions including either text responses or modality insights.
##### orchestration\
- **BurnoutAssessmentOrchestrator.java**: Coordinates the burnout assessment lifecycle and agents.
##### service\
- **BurnoutFhirService.java**: Service for converting Burnout Assessment data to FHIR format, validating it against FHIR specifications, and storing it in S3.
##### workers\
- **BurnoutWorker.java**: Assembles and executes a full burnout assessment workflow.
#### cbt\
##### controller\
- **CBTController.java**: Handles CBT session endpoints and orchestration.
##### model\
- **AnalysisResult.java**: Stores multimodal analysis results including detected dominant emotions and cognitive distortions.
##### service\
- **CBTHelper.java**: Provides common utilities for CBT workflow execution.
- **OrchestrationService.java**: Manages coordination across CBT analysis components.
#### config\
- **JwtTokenFilter.java**: Filters incoming requests to validate and extract JWT credentials.
- **RedisConfig.java**: Sets up Redis for caching and session tracking.
- **SecurityConfig.java**: Configures authentication, authorization, and CORS policies.
- **WebSocketConfig.java**: Registers WebSocket handlers and applies security filters.
#### llm\
##### config\
- **BedrockConfig.java**: Configures access to Amazon Bedrock LLM services.
##### service\
- **BedrockService.java**: Manages calls to LLMs via Bedrock for generative tasks.
- **LLMProcessingService.java**: Processes Bedrock prompts and responses.
- **S3StorageService.java**: Some preliminary S3 utility methods.
#### persistence\
- **VectorDatabaseService.java**: Manages vector storage and similarity queries for embedding-based search.
#### utility\
- **S3Utils.java**: Contains helper methods for parsing and constructing S3 URLs and performing S3 web requests.
#### websockets\
- **BurnoutWebSocketHandler.java**: Processes real-time WebSocket messages for burnout assessments.
- **CBTWebSocketHandler.java**: Processes real-time WebSocket messages for CBT sessions.
#### workers\
- **AudioAnalysisWorker.java**: Performs prosodic and emotional analysis on audio inputs using Hume AI.
- **MessageWorker.java**: Uses user modality assessments to generate directed CBT prompts.
- **MultiModalSynthesizer.java**: Aggregates and synthesizes multimodal data into unified output.
- **TextAnalysisWorker.java**: Specialized worker that analyzes text inputs to extract insights using Gemini AI.
- **VideoAnalysisWorker.java**: Analyzes facial expressions and emotions from video frames useing AWS Rekognition AI.


## Testing
To run the tests, use the following command from the backend directory:
'mvn test'