# LeadZam CRM Frontend Dashboard

A modern, responsive React + Vite web dashboard for sales agents to trigger one-click calls directly to their connected Android smartphone.

## Features

- **Pipeline Overview**: View sample leads with contact names, stages, and phone numbers.
- **Click-to-Call**: Sends selected lead's contact information to the Spring Boot backend (`POST /api/dial`).
- **Device Connectivity Bar**: Displays real-time Android device registration status (Option B) or allows pasting an FCM token directly (Option A).
- **Clear Request States**:
  - *Sending*: Visual spinner while the request is transmitted.
  - *Accepted by Firebase*: Displays Firebase Cloud Messaging message ID.
  - *Error Handling*: Explicit error messages for connection failures, missing tokens, or invalid phone numbers.
- **Custom Lead Creation**: Add real test phone numbers to test dialing any number.
- **Session Activity Log**: Historical table of all dial requests dispatched during the current session.

## Getting Started

### 1. Install Dependencies
```bash
npm install
```

### 2. Run Development Server
```bash
npm run dev
```
The application will launch at `http://localhost:5173`.

### 3. Build for Production
```bash
npm run build
```
