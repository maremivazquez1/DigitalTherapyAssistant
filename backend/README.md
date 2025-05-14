# Backend
This backend is a Spring Boot microservice written in Java and built with Maven. 
It powers the core logic for a digital therapy assistant application.

### Stack Overview
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


## Testing
To run the tests, use the following command from the backend directory:
'mvn test'