package com.leadzam.clicktocall.controller;

import com.leadzam.clicktocall.dto.DeviceRegistrationRequest;
import com.leadzam.clicktocall.dto.DeviceStatusResponse;
import com.leadzam.clicktocall.dto.DialRequest;
import com.leadzam.clicktocall.dto.DialResponse;
import com.leadzam.clicktocall.service.DeviceRegistrationService;
import com.leadzam.clicktocall.service.FcmService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class DialController {
    private static final Logger log = LoggerFactory.getLogger(DialController.class);

    private final FcmService fcmService;
    private final DeviceRegistrationService deviceRegistrationService;

    public DialController(FcmService fcmService, DeviceRegistrationService deviceRegistrationService) {
        this.fcmService = fcmService;
        this.deviceRegistrationService = deviceRegistrationService;
    }

    /**
     * Endpoint invoked by CRM Dashboard to initiate a click-to-call request.
     */
    @PostMapping("/dial")
    public ResponseEntity<DialResponse> initiateDial(@Valid @RequestBody DialRequest request) {
        log.info("Received /api/dial request for contact '{}' ({})", request.getContactName(), request.getPhoneNumber());
        DialResponse response = fcmService.sendDialRequest(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint invoked by Android app to register its FCM token (Option B).
     */
    @PostMapping("/register-device")
    public ResponseEntity<Map<String, Object>> registerDevice(@Valid @RequestBody DeviceRegistrationRequest request) {
        log.info("Received /api/register-device request from device '{}'", request.getDeviceName());
        deviceRegistrationService.registerDevice(request);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Device successfully registered for call dispatch",
                "registeredAt", System.currentTimeMillis()
        ));
    }

    /**
     * Endpoint for CRM Dashboard to check whether a target Android device is online/registered.
     */
    @GetMapping("/device-status")
    public ResponseEntity<DeviceStatusResponse> getDeviceStatus() {
        return ResponseEntity.ok(deviceRegistrationService.getDeviceStatus());
    }

    /**
     * Simple health check.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "LeadZam Backend API",
                "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }
}
