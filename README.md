# LeadZam Click-to-Call

[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Spring%20Boot%20%7C%20React-blue.svg)](https://github.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-purple.svg)](https://kotlinlang.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4%20(Java%2021)-brightgreen.svg)](https://spring.io)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

An end-to-end, production-grade click-to-call integration connecting a modern browser-based CRM pipeline directly to a representative's native Android phone dialer using Firebase Cloud Messaging (FCM).

---

## 1. Project Overview

LeadZam Click-to-Call bridges the gap between desktop CRM sales dashboards and physical Android smartphones. When a sales representative reviews prospective leads on the CRM interface and clicks **Call**, the system transmits a verified call request through a Spring Boot backend API to Firebase Cloud Messaging (FCM).

The Android application receives the request in real time, displays the incoming lead details, and upon explicit user confirmation, opens the native Android phone dialer with the phone number pre-filled.

> ⚠️ **Important Safety Guarantee**: The application **NEVER** places calls automatically (`ACTION_CALL` is strictly prohibited). It solely pre-fills the native dialer via `Intent.ACTION_DIAL`, giving the representative full control to review and initiate the cellular call.

---

## 2. Features

- **Browser-Based CRM Dashboard**:
  - Sample pipeline leads (Rahul Sharma, Priya Verma, Amit Patel, Sneha Reddy, Ananya Roy).
  - Clear real-time status indicators: *Idle*, *Sending*, *Accepted by Firebase*, and *Error*.
  - Manual FCM token configuration (Option A) or automated device registration sync (Option B).
  - Custom lead creation modal to test dialing any real phone number.
  - Interactive session activity log tracking recent dial requests and Firebase message IDs.
- **Spring Boot Backend API (Java 21)**:
  - `POST /api/dial`: Validates contact information and phone number formatting; securely sends FCM data messages via Firebase Admin SDK.
  - `POST /api/register-device`: Endpoint for Android app to register its FCM token dynamically.
  - `GET /api/device-status`: Health and device connection inquiry endpoint.
  - Mock mode fallback (`FIREBASE_MOCK_MODE=true`) for local offline testing.
- **Kotlin Android Application**:
  - Native XML layouts built with ViewBinding and Material 3 design.
  - Adaptive launcher icon with branded visual identity.
  - **Foreground Handling**: Real-time incoming call request card with direct "Open Dialer" action.
  - **Background Handling**: High-priority heads-up system notifications with action buttons.
  - Android 13+ (`POST_NOTIFICATIONS`) runtime permission flow.
  - One-click FCM token copy and backend registration.
  - Test simulation action for instant zero-config dialer verification.

---

## 3. Architecture & Data Flow

```
┌────────────────────────┐
│  CRM Web Dashboard     │  (React 18 + Vite)
│  Sales Representative  │
└───────────┬────────────┘
            │ 1. POST /api/dial { phoneNumber, contactName, deviceToken }
            ▼
┌────────────────────────┐
│  Spring Boot Backend   │  (Java 21 + Spring Web)
│  Firebase Admin SDK    │  - Validates E.164 phone number
└───────────┬────────────┘  - Resolves target Android device token
            │ 2. FCM Data Message { type: "dial_request", phoneNumber, contactName }
            ▼
┌────────────────────────┐
│  Firebase Cloud        │  (Google FCM Infrastructure)
│  Messaging (FCM)       │
└───────────┬────────────┘
            │ 3. Push to Android Device
            ▼
┌────────────────────────┐
│  LeadZam Android App   │  (Kotlin)
│  Foreground/Background │  - Foreground: Active call card
└───────────┬────────────┘  - Background: Heads-up notification
            │ 4. User taps "Open Dialer"
            ▼
┌────────────────────────┐
│  Native Phone Dialer   │  Intent.ACTION_DIAL (tel:+919876543210)
│  (Human initiates call)│
└────────────────────────┘
```

For complete technical specifications, see [`docs/architecture.md`](docs/architecture.md).

---

## 4. Technologies Used

- **Android Client**:
  - Language: Kotlin 2.2.10
  - Target SDK: Android 15 (API 36, minSdk 24)
  - UI: XML Layouts with ViewBinding & Material Design 3
  - Push Messaging: Firebase Cloud Messaging (`com.google.firebase:firebase-messaging`)
  - Concurrency: Kotlin Coroutines & Flow
  - Intents: Android `Intent.ACTION_DIAL`
- **Backend Service**:
  - Language: Java 21 (LTS)
  - Framework: Spring Boot 3.3.4
  - Build Tool: Apache Maven 3.9+
  - SDK: Firebase Admin SDK 9.3.0
  - Validation: Hibernate Validator / Bean Validation
  - Testing: JUnit 5, Mockito, Spring MockMvc
- **CRM Frontend**:
  - Framework: React 18
  - Build Tool: Vite 5
  - Styling: Modern CSS with responsive design system

---

## 5. Prerequisites

Before running the project locally, ensure you have:
1. **Java Development Kit (JDK 21)** installed (`java -version`).
2. **Node.js (v18+)** and npm installed (`node -v`).
3. **Android Studio** (Koala / Ladybug or newer) with Android SDK 34+.
4. A **Firebase Account** with a new or existing Firebase project.

---

## 6. Firebase Project Setup

1. Open the [Firebase Console](https://console.firebase.google.com/) and create a new project (e.g. `leadzam-click-to-call`).
2. **Add Android App**:
   - Register package name: `com.aistudio.leadzam.cktcld` (or check your `applicationId` in `app/build.gradle.kts`).
   - Download the generated `google-services.json` file.
   - Place `google-services.json` inside the `app/` directory:
     ```
     app/google-services.json
     ```
3. **Generate Backend Service Account**:
   - In Firebase Console, go to **Project Settings** ➔ **Service accounts**.
   - Click **Generate new private key**.
   - Save the downloaded JSON file as `backend/firebase-service-account.json`.
   - *Never commit this private key to Git* (it is included in `.gitignore`).

---

## 7. Backend Configuration & Startup

### Environment Variables
Copy `backend/.env.example` to `backend/.env` or configure your environment:

```bash
cd backend
cp .env.example .env
```

Key configuration variables:
- `PORT`: HTTP port for the Spring Boot server (default: `8080`).
- `FIREBASE_SERVICE_ACCOUNT_PATH`: Path to your downloaded service account JSON key (default: `./firebase-service-account.json`).
- `DEFAULT_DEVICE_FCM_TOKEN`: Optional pre-configured device token (Option A).
- `FIREBASE_MOCK_MODE`: Set to `true` if you wish to run the backend locally without Firebase credentials.

### Start the Spring Boot Backend

Using Maven:
```bash
cd backend
mvn clean spring-boot:run
```
*(Or use `./mvnw spring-boot:run`)*

The backend will start on `http://localhost:8080`.
Verify health by visiting: `http://localhost:8080/api/health`.

---

## 8. Frontend CRM Dashboard Startup

```bash
cd frontend
npm install
npm run dev
```

The CRM dashboard will open at `http://localhost:5173`.
By default, the frontend connects to `http://localhost:8080`. You can change the backend URL at any time via the top-bar button.

---

## 9. Android Application Build & Installation

1. Open the repository root in **Android Studio**.
2. Sync Gradle files (`File` ➔ `Sync Project with Gradle Files`).
3. Ensure `google-services.json` is located in `/app/`.
4. Connect a physical Android phone via USB debugging (recommended for FCM notifications) or launch an Android Virtual Device (AVD).
5. Click **Run 'app'** (`Shift + F10`).

### Connecting Android to Local Backend
- **Android Emulator**: Uses `http://10.0.2.2:8080` by default to access host machine services.
- **Physical Device**: Tap the backend URL at the bottom of the LeadZam Android home screen and update it to your computer's local Wi-Fi IP address (e.g. `http://192.168.1.50:8080`).

---

## 10. Device Registration Workflow

### Option A: Manual Token Configuration
1. Open the LeadZam Android app.
2. Tap **Copy Token** to copy your device's unique FCM registration token.
3. In the CRM Dashboard, click **Paste Device Token (Option A)** and paste the token into the input bar.
4. Subsequent calls will target this token directly.

### Option B: Automatic Backend Registration
1. In the Android app, tap **Register Device**.
2. The app calls `POST /api/register-device` with its FCM token and device model.
3. The CRM dashboard automatically detects the registered device on its next poll.

---

## 11. API Specification

### 1. Initiate Dial Request
- **Endpoint**: `POST /api/dial`
- **Request Body**:
  ```json
  {
    "phoneNumber": "+919876543210",
    "contactName": "Rahul Sharma",
    "deviceToken": "optional_override_token"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "message": "Call request accepted by Firebase for delivery to Android device",
    "messageId": "projects/leadzam/messages/0:1728816000000000%abcdef",
    "contactName": "Rahul Sharma",
    "phoneNumber": "+919876543210",
    "deviceTokenMasked": "dXZ0...k81b",
    "timestamp": 1728816000000
  }
  ```

### 2. Register Device
- **Endpoint**: `POST /api/register-device`
- **Request Body**:
  ```json
  {
    "token": "dXZ0...full_fcm_token...",
    "deviceName": "Google Pixel 7"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "message": "Device successfully registered for call dispatch",
    "registeredAt": 1728816000000
  }
  ```

### 3. Check Device Status
- **Endpoint**: `GET /api/device-status`
- **Response (200 OK)**:
  ```json
  {
    "registered": true,
    "deviceCount": 1,
    "latestTokenMasked": "dXZ0...k81b",
    "deviceName": "Google Pixel 7",
    "lastRegisteredAt": 1728816000000
  }
  ```

---

## 12. Automated & Manual Testing

### Automated Backend Tests
Run the Spring Boot MockMvc and validation test suite:
```bash
cd backend
mvn test
```
**Tests Covered**:
- `testValidDialRequest_WithExplicitDeviceToken`: Validates payload formatting, Mockito FCM dispatch, and 200 OK response.
- `testInvalidPhoneNumber_ReturnsBadRequest`: Validates rejection of malformed phone numbers with 400 Bad Request.
- `testMissingContactName_ReturnsBadRequest`: Validates Bean Validation enforcement on missing contact names.
- `testMissingDeviceToken_WhenNoDeviceRegistered_ReturnsBadRequest`: Validates handling when no device is known.
- `testDeviceRegistrationAndSubsequentDial`: End-to-end integration flow of registering a device and dialing without explicit token.
- `testHealthEndpoint`: Validates basic system readiness.

### Automated Android Tests
Run Robolectric and unit tests:
```bash
gradle :app:testDebugUnitTest
```
**Tests Covered**:
- String resource verification (`LeadZam`).
- Comprehensive phone number validation (E.164, spaces, dashes, parentheses).
- Rejection of invalid numbers (letters, short numbers, empty inputs).
- `tel:` URI phone number sanitization.
- In-memory `CallRequestRepository` state observation and lifecycle.

### Manual End-to-End Verification Checklist
- [x] **CRM Lead Display**: CRM displays pipeline leads with contact names and phone numbers.
- [x] **Call Dispatch**: Clicking **Call** changes button state to sending, posts to backend, and displays message confirmation.
- [x] **Foreground Receipt**: When Android app is open, incoming card immediately displays contact name and phone number with "Open Dialer".
- [x] **Background Receipt**: When Android app is backgrounded, high-priority heads-up notification appears with "Open Dialer" action.
- [x] **Native Dialer Launch**: Tapping **Open Dialer** opens the system phone dialer with `+919876543210` pre-filled.
- [x] **No Auto-Call**: Dialer pauses on screen; call is never initiated without physical user tap.

---

## 13. Troubleshooting

| Issue | Cause | Solution |
| :--- | :--- | :--- |
| `Cannot reach backend at http://localhost:8080` | Spring Boot is not running or blocked by firewall. | Run `mvn spring-boot:run` in `backend/` and verify `http://localhost:8080/api/health`. |
| `Firebase service account file NOT FOUND` | Missing `firebase-service-account.json`. | Place your service account file in `backend/` or set `FIREBASE_MOCK_MODE=true` in `.env` for local testing. |
| Android app doesn't receive FCM messages | Missing `google-services.json` or token expired. | Ensure `google-services.json` is in `app/`. Copy fresh FCM token using **Copy Token** and update in CRM dashboard. |
| Android emulator cannot connect to backend | Used `localhost` instead of loopback. | Use `http://10.0.2.2:8080` as the backend URL inside the Android emulator. |
| Notification doesn't appear on Android 13+ | `POST_NOTIFICATIONS` permission denied. | Tap **Enable Notifications** in the in-app banner or allow notifications in Android App Settings. |

---

## 14. Known Limitations

- **Single Active Session Focus**: Built as an intern demonstration prototype; stores device registrations in an in-memory thread-safe concurrent registry rather than a persistent SQL database.
- **Telecom Carrier Dialer**: `ACTION_DIAL` delegates cellular dialing to the device's default dialer app. Actual carrier call placement requires an active SIM card or cellular network.

---

## 15. AI Usage Notes

*In compliance with assignment requirements, the following reflections document AI assistance utilized during the development of this project:*

- **Which AI tools did I use?**
  - Google AI Studio (Gemini 3.8 Flash model) was used for architecture scaffolding, code generation, and test creation across Kotlin, Spring Boot, and React.
- **Where did AI help me most?**
  - AI assisted most effectively in cross-platform architectural alignment: designing pure FCM data messages to ensure consistent foreground and background handling in Android, writing Spring Boot validation DTOs with Mockito tests, and creating the Material 3 XML UI.
- **Describe one AI suggestion that I changed, rejected, or improved, and explain why:**
  - The AI initially suggested using FCM `notification` messages with an automatic system tray notification. I rejected this because standard notification messages bypass `onMessageReceived` when the Android app is backgrounded, preventing custom payload validation, dynamic intent extra extraction, and singleTop activity routing. I adjusted the architecture to use pure **data messages**, allowing `LeadZamFirebaseMessagingService` to manage both foreground card display and background system notifications reliably.
- **What would I improve with another two hours?**
  - With an additional two hours, I would:
    1. Implement WebSockets (STOMP / SockJS) between the Spring Boot backend and React CRM dashboard to reflect live call completion status when the representative finishes the call.
    2. Add SQLite persistence (Room on Android, H2 on backend) for durable offline call logging.
    3. Add a QR Code scanner in the Android app to scan the CRM dashboard's session ID and pair devices instantly.
