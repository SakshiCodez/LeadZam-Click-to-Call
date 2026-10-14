# LeadZam Click-to-Call Backend API

Spring Boot 3 service powered by Java 21 and the Firebase Admin SDK to dispatch click-to-call requests to Android devices via Firebase Cloud Messaging.

## Architecture

- **Controller**: `DialController` (`/api/dial`, `/api/register-device`, `/api/device-status`, `/api/health`)
- **Services**:
  - `FcmService`: Validates payloads, handles Firebase messaging, converts errors.
  - `DeviceRegistrationService`: In-memory thread-safe device token registry.
- **DTOs**: Validated request and response schemas with token masking.
- **Security**:
  - `firebase-service-account.json` loaded from external environment variable.
  - CORS filter configured for local React dashboard.

## Running Locally

```bash
# 1. Copy environment template
cp .env.example .env

# 2. Run with Maven
mvn spring-boot:run
```

The server starts on port `8080`.

## Testing

```bash
mvn test
```
All unit tests mock Firebase messaging using Mockito to enable deterministic CI/CD verification without external cloud credentials.
