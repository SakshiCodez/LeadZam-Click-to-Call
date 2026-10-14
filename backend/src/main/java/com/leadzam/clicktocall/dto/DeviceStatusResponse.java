package com.leadzam.clicktocall.dto;

public class DeviceStatusResponse {
    private boolean registered;
    private int deviceCount;
    private String latestTokenMasked;
    private String deviceName;
    private long lastRegisteredAt;

    public DeviceStatusResponse() {
    }

    public DeviceStatusResponse(boolean registered, int deviceCount, String latestTokenMasked, String deviceName, long lastRegisteredAt) {
        this.registered = registered;
        this.deviceCount = deviceCount;
        this.latestTokenMasked = latestTokenMasked;
        this.deviceName = deviceName;
        this.lastRegisteredAt = lastRegisteredAt;
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public int getDeviceCount() {
        return deviceCount;
    }

    public void setDeviceCount(int deviceCount) {
        this.deviceCount = deviceCount;
    }

    public String getLatestTokenMasked() {
        return latestTokenMasked;
    }

    public void setLatestTokenMasked(String latestTokenMasked) {
        this.latestTokenMasked = latestTokenMasked;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public long getLastRegisteredAt() {
        return lastRegisteredAt;
    }

    public void setLastRegisteredAt(long lastRegisteredAt) {
        this.lastRegisteredAt = lastRegisteredAt;
    }
}
