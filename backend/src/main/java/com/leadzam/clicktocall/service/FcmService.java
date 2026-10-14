package com.leadzam.clicktocall.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.leadzam.clicktocall.config.FirebaseConfig;
import com.leadzam.clicktocall.dto.DialRequest;
import com.leadzam.clicktocall.dto.DialResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FcmService {
    private static final Logger log = LoggerFactory.getLogger(FcmService.class);

    private final FirebaseConfig firebaseConfig;
    private final DeviceRegistrationService deviceRegistrationService;
    private FirebaseMessaging mockMessaging; // For test injection

    public FcmService(FirebaseConfig firebaseConfig, DeviceRegistrationService deviceRegistrationService) {
        this.firebaseConfig = firebaseConfig;
        this.deviceRegistrationService = deviceRegistrationService;
    }

    // Setter for unit test mock injection
    public void setFirebaseMessagingForTest(FirebaseMessaging messaging) {
        this.mockMessaging = messaging;
    }

    public DialResponse sendDialRequest(DialRequest request) {
        String targetToken = deviceRegistrationService.resolveTargetToken(request.getDeviceToken());

        if (targetToken == null || targetToken.isBlank()) {
            throw new IllegalArgumentException(
                    "No destination Android device token found. Please register the Android device via the app, " +
                    "provide 'deviceToken' in the request, or configure DEFAULT_DEVICE_FCM_TOKEN."
            );
        }

        String requestId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        log.info("Preparing FCM dial_request for contact '{}' ({}) with requestId '{}' to token suffix ...{}",
                request.getContactName(), request.getPhoneNumber(), requestId, deviceRegistrationService.maskToken(targetToken));

        // Handle Mock Mode or Unit Test fallback
        if (firebaseConfig != null && firebaseConfig.isMockMode()) {
            String mockMessageId = "projects/leadzam-demo/messages/mock-" + UUID.randomUUID().toString().substring(0, 8);
            log.info("MOCK MODE: Simulated FCM send to token. MessageId: {}", mockMessageId);
            return DialResponse.success(
                    "Call request queued with Firebase Cloud Messaging (Simulated in Mock Mode)",
                    mockMessageId,
                    request.getContactName(),
                    request.getPhoneNumber(),
                    targetToken
            );
        }

        // If real Firebase is not initialized and no mock messaging injected
        if (mockMessaging == null && (firebaseConfig == null || !firebaseConfig.isInitialized())) {
            throw new IllegalStateException(
                    "Firebase Admin SDK is not initialized. Place your 'firebase-service-account.json' key in the " +
                    "backend directory or set FIREBASE_SERVICE_ACCOUNT_PATH. For local testing without Firebase, set FIREBASE_MOCK_MODE=true."
            );
        }

        try {
            Message message = Message.builder()
                    .setToken(targetToken)
                    .putData("type", "dial_request")
                    .putData("phoneNumber", request.getPhoneNumber().trim())
                    .putData("contactName", request.getContactName().trim())
                    .putData("requestId", requestId)
                    .putData("timestamp", String.valueOf(now))
                    .build();

            String messageId;
            if (mockMessaging != null) {
                messageId = mockMessaging.send(message);
            } else {
                messageId = FirebaseMessaging.getInstance().send(message);
            }

            log.info("✅ FCM message successfully accepted by Firebase. MessageId: {}", messageId);

            return DialResponse.success(
                    "Call request accepted by Firebase for delivery to Android device",
                    messageId,
                    request.getContactName(),
                    request.getPhoneNumber(),
                    targetToken
            );

        } catch (FirebaseMessagingException e) {
            String errorCode = e.getErrorCode() != null ? e.getErrorCode().name() : "UNKNOWN";
            log.error("FCM dispatch failed with FirebaseMessagingException [{}]: {}", errorCode, e.getMessage());

            String userMessage;
            if ("UNREGISTERED".equalsIgnoreCase(errorCode) || "INVALID_ARGUMENT".equalsIgnoreCase(errorCode)) {
                userMessage = "FCM Token is invalid or expired. Please refresh the FCM token on the Android device and re-register.";
            } else if ("AUTHENTICATION_ERROR".equalsIgnoreCase(errorCode)) {
                userMessage = "Firebase Admin authentication failed. Verify service account credentials and permissions.";
            } else {
                userMessage = "Firebase send failure: " + e.getMessage();
            }

            throw new RuntimeException(userMessage, e);
        } catch (Exception e) {
            log.error("Unexpected failure sending FCM message", e);
            throw new RuntimeException("Unexpected error communicating with Firebase: " + e.getMessage(), e);
        }
    }
}
