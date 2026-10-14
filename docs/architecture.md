# LeadZam Click-to-Call Architecture Documentation

## 1. Executive Summary

LeadZam Click-to-Call provides a streamlined, secure bridge connecting browser-based CRM sales workflows directly with sales representatives' physical Android devices.

When a sales representative reviews a lead in the CRM dashboard and clicks **Call**, the system transmits a structured request through the Spring Boot API service to Firebase Cloud Messaging (FCM). The Android application receives this data message, displays the incoming call alert, and allows the user with one explicit tap to populate the native Android phone dialer (`Intent.ACTION_DIAL`).

---

## 2. High-Level System Architecture

```
┌─────────────────────────────────┐
│     LeadZam CRM Web UI          │
│    (React 18 + Vite SPA)        │
│    - Leads List & Pipeline      │
│    - One-Click Call Dispatch    │
│    - Real-Time Device Status    │
└────────────────┬────────────────┘
                 │ HTTP POST /api/dial
                 │ JSON Payload: { phoneNumber, contactName, deviceToken }
                 ▼
┌─────────────────────────────────┐
│   Spring Boot Backend Service   │
│   (Java 21 + Spring Web)        │
│   - Phone Number Validation     │
│   - Target Token Resolution     │
│   - Firebase Admin SDK Client   │
└────────────────┬────────────────┘
                 │ FCM Data Message
                 │ Payload: { type: "dial_request", phoneNumber, contactName, requestId }
                 ▼
┌─────────────────────────────────┐
│   Firebase Cloud Messaging      │
│   (Google FCM Infrastructure)   │
└────────────────┬────────────────┘
                 │ Push Notification to Target Token
                 ▼
┌─────────────────────────────────┐
│     LeadZam Android App         │
│   (Kotlin + FCM Messaging)      │
│   - FirebaseMessagingService    │
│   - Foreground: Live Card State │
│   - Background: System Alert    │
└────────────────┬────────────────┘
                 │ Explicit User Interaction ("Call Now")
                 │ Intent: ACTION_DIAL (tel:+919876543210)
                 ▼
┌─────────────────────────────────┐
│   Native Android Phone Dialer   │
│   - Pre-filled Phone Number     │
│   - User confirms call button   │
└─────────────────────────────────┘
```

---

## 3. End-to-End Sequence Diagram

```
Sales Agent        CRM Dashboard         Backend API             Firebase Cloud Messaging      Android Device        Native Dialer
    │                   │                     │                             │                        │                     │
    │── Clicks Call ───>│                     │                             │                        │                     │
    │                   │── POST /api/dial ──>│                             │                        │                     │
    │                   │                     │── Validate Payload ─────────│                        │                     │
    │                   │                     │── Resolve Device Token ─────│                        │                     │
    │                   │                     │── Build Data Message ──────>│                        │                     │
    │                   │                     │                             │── Push to Token ──────>│                     │
    │                   │<─ 200 OK (Msg ID) ──│                             │                        │                     │
    │<─ Request Sent ───│                     │                             │                        │                     │
    │                   │                     │                             │                        │── Foreground: Card ─│
    │                   │                     │                             │                        │── Background: Notif │
    │                   │                     │                             │                        │                     │
    │──────────────────────── Taps "Call Now" on Phone ─────────────────────────────────────────────>│                     │
    │                   │                     │                             │                        │── Intent.ACTION_DIAL>│
    │                   │                     │                             │                        │                     │── Pre-fills
    │                   │                     │                             │                        │                     │   Number
```

---

## 4. Message Payload Specification

### CRM to Backend (`POST /api/dial`)

```json
{
  "phoneNumber": "+919876543210",
  "contactName": "Rahul Sharma",
  "deviceToken": "optional_fcm_token_override"
}
```

- **Validation Rules**:
  - `phoneNumber`: Must not be blank; validated via regex `^\+?[0-9\s\-\(\)]{7,25}$` ensuring between 7 and 15 digits conform to E.164 or standardized local formats.
  - `contactName`: Must not be blank.
  - `deviceToken`: Optional. If omitted, the backend resolves the latest device registered via `/api/register-device` (Option B) or the pre-configured `DEFAULT_DEVICE_FCM_TOKEN` (Option A).

### Backend to FCM (Data Payload)

```json
{
  "token": "eX_...target_android_fcm_token...",
  "data": {
    "type": "dial_request",
    "phoneNumber": "+919876543210",
    "contactName": "Rahul Sharma",
    "requestId": "9c82b9e6-0b8c-4a37-b4d4-539cfdfa164b",
    "timestamp": "1728816000000"
  }
}
```

---

## 5. Foreground vs Background Lifecycle Architecture

A critical design consideration in Android FCM integration is the distinction between **Notification Messages** and **Data Messages**:

| Message Type | Foreground Behavior | Background Behavior |
| :--- | :--- | :--- |
| **Notification Message** (`notification: { title, body }`) | Handled by `onMessageReceived` | Automatically intercepted by the Android System Tray. `onMessageReceived` is **not called**. |
| **Data Message** (`data: { ... }`) | Delivered directly to `onMessageReceived` | Delivered directly to `onMessageReceived`. **Full developer control**. |

LeadZam utilizes **pure Data Messages**. This guarantees that:
1. `LeadZamFirebaseMessagingService.onMessageReceived` is consistently invoked across all application states.
2. The app inspects its foreground state (`CallRequestRepository.isAppInForeground`):
   - **When in Foreground**: The app directly updates the live `activeRequest` UI state, displaying the prominent "Incoming Call Request" card.
   - **When in Background**: The app displays an interactive, high-priority system notification on Android channel `leadzam_call_requests_channel` featuring the lead's name, phone number, and an "Open Dialer" action.
3. The activity launch mode is configured as `android:launchMode="singleTop"`. Tapping a background notification delivers the payload to `onNewIntent` without recreating the activity or leaking multiple activity instances.

---

## 6. Native Dialer Safety Architecture

### Intent.ACTION_DIAL vs Intent.ACTION_CALL

- **`Intent.ACTION_CALL` (Forbidden)**:
  - Requires dangerous runtime permission `android.permission.CALL_PHONE`.
  - Initiates the cellular or telecom call immediately upon execution without user interaction.
  - High risk of accidental calls or automated abuse.
- **`Intent.ACTION_DIAL` (Implemented)**:
  - Requires **zero special permissions**.
  - Populates the user's default system phone dialer with `tel:+919876543210`.
  - The human user must physically press the call button on their dialer to place the call.
  - Provides complete transparency and agency to the representative.

---

## 7. Security & Credential Isolation

1. **Zero Secret Leakage**:
   - The Firebase Service Account private key (`firebase-service-account.json`) is never bundled into the Android APK or the React frontend build.
   - It is loaded strictly on the Spring Boot backend host via the `FIREBASE_SERVICE_ACCOUNT_PATH` environment variable.
2. **Token Masking**:
   - FCM registration tokens are masked in API responses (`subst...6chars`) and logs to prevent credential leakage in transit or telemetry.
3. **CORS Isolation**:
   - Spring Boot defines explicit CORS headers allowing local Vite dev server communication while preventing cross-origin script injection.
