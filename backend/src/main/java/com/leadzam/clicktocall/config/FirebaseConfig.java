package com.leadzam.clicktocall.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {
    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${leadzam.firebase.service-account-path:firebase-service-account.json}")
    private String serviceAccountPath;

    @Value("${leadzam.firebase.mock-mode:false}")
    private boolean mockMode;

    private boolean initialized = false;

    @PostConstruct
    public void initializeFirebase() {
        if (mockMode) {
            log.info("Firebase running in MOCK MODE (leadzam.firebase.mock-mode=true). FCM messages will be simulated locally.");
            return;
        }

        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                initialized = true;
                log.info("FirebaseApp is already initialized.");
                return;
            }

            File serviceAccountFile = new File(serviceAccountPath);
            InputStream serviceAccountStream = null;

            if (serviceAccountFile.exists()) {
                log.info("Found Firebase service account file at: {}", serviceAccountFile.getAbsolutePath());
                serviceAccountStream = new FileInputStream(serviceAccountFile);
            } else {
                // Try classpath
                serviceAccountStream = getClass().getClassLoader().getResourceAsStream(serviceAccountPath);
            }

            if (serviceAccountStream == null) {
                log.warn("⚠️ Firebase service account file NOT FOUND at: '{}'. Real FCM dispatch requires this file. " +
                        "Place your service account JSON file at '{}' or configure FIREBASE_SERVICE_ACCOUNT_PATH. " +
                        "For local testing without credentials, mock mode will handle dial requests gracefully.",
                        serviceAccountPath, serviceAccountPath);
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                    .build();

            FirebaseApp.initializeApp(options);
            initialized = true;
            log.info("✅ FirebaseApp initialized successfully with service account.");

        } catch (Exception e) {
            log.error("Failed to initialize FirebaseApp with credentials from: {}", serviceAccountPath, e);
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isMockMode() {
        return mockMode;
    }
}
