package com.leadzam.clicktocall.dto;

import jakarta.validation.constraints.NotBlank;

public class DeviceRegistrationRequest {

    @NotBlank(message = "Device FCM token is required")
    private String token;

    private String deviceName;

    private Long registeredAt;

    public DeviceRegistrationRequest() {
    }

    public DeviceRegistrationRequest(String token, String deviceName) {
        this.token = token;
        this.deviceName = deviceName;
        this.registeredAt = System.currentTimeMillis();
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public Long getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Long registeredAt) {
        this.registeredAt = registeredAt;
    }
}
