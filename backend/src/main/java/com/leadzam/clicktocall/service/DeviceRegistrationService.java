package com.leadzam.clicktocall.service;

import com.leadzam.clicktocall.dto.DeviceRegistrationRequest;
import com.leadzam.clicktocall.dto.DeviceStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class DeviceRegistrationService {
    private static final Logger log = LoggerFactory.getLogger(DeviceRegistrationService.class);

    @Value("${leadzam.firebase.default-device-token:}")
    private String defaultDeviceToken;

    // Stores registered devices keyed by token
    private final ConcurrentHashMap<String, DeviceRegistrationRequest> registeredDevices = new ConcurrentHashMap<>();
    private volatile String latestRegisteredToken = null;

    public synchronized void registerDevice(DeviceRegistrationRequest request) {
        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new IllegalArgumentException("Device FCM token must not be empty.");
        }

        if (request.getRegisteredAt() == null) {
            request.setRegisteredAt(System.currentTimeMillis());
        }

        registeredDevices.put(request.getToken(), request);
        latestRegisteredToken = request.getToken();
        log.info("Registered device '{}' with token suffix ...{}",
                request.getDeviceName() != null ? request.getDeviceName() : "Unknown",
                maskToken(request.getToken()));
    }

    public String resolveTargetToken(String requestedToken) {
        // Priority 1: Token provided directly in dial request
        if (requestedToken != null && !requestedToken.isBlank()) {
            return requestedToken.trim();
        }

        // Priority 2: Latest device registered via /api/register-device (Option B)
        if (latestRegisteredToken != null && !latestRegisteredToken.isBlank()) {
            return latestRegisteredToken;
        }

        // Priority 3: Default token configured via environment variable (Option A)
        if (defaultDeviceToken != null && !defaultDeviceToken.isBlank()) {
            return defaultDeviceToken.trim();
        }

        return null;
    }

    public DeviceStatusResponse getDeviceStatus() {
        boolean hasRegistered = latestRegisteredToken != null || (defaultDeviceToken != null && !defaultDeviceToken.isBlank());
        String activeToken = latestRegisteredToken != null ? latestRegisteredToken : defaultDeviceToken;

        String deviceName = "Unknown Device";
        long regTime = 0;

        if (latestRegisteredToken != null && registeredDevices.containsKey(latestRegisteredToken)) {
            DeviceRegistrationRequest reg = registeredDevices.get(latestRegisteredToken);
            deviceName = reg.getDeviceName() != null ? reg.getDeviceName() : "Android Device";
            regTime = reg.getRegisteredAt() != null ? reg.getRegisteredAt() : 0;
        } else if (defaultDeviceToken != null && !defaultDeviceToken.isBlank()) {
            deviceName = "Pre-configured Default Device (Option A)";
        }

        return new DeviceStatusResponse(
                hasRegistered,
                registeredDevices.size(),
                maskToken(activeToken),
                deviceName,
                regTime
        );
    }

    public String maskToken(String token) {
        if (token == null || token.isBlank()) return "Not configured";
        if (token.length() <= 12) return token;
        return token.substring(0, 6) + "..." + token.substring(token.length() - 6);
    }
}
