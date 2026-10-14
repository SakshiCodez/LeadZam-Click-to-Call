package com.leadzam.clicktocall.dto;

public class DialResponse {
    private boolean success;
    private String message;
    private String messageId;
    private String contactName;
    private String phoneNumber;
    private String deviceTokenMasked;
    private long timestamp;

    public DialResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public DialResponse(boolean success, String message, String messageId, String contactName, String phoneNumber, String deviceTokenMasked) {
        this.success = success;
        this.message = message;
        this.messageId = messageId;
        this.contactName = contactName;
        this.phoneNumber = phoneNumber;
        this.deviceTokenMasked = deviceTokenMasked;
        this.timestamp = System.currentTimeMillis();
    }

    public static DialResponse success(String message, String messageId, String contactName, String phoneNumber, String token) {
        String masked = maskToken(token);
        return new DialResponse(true, message, messageId, contactName, phoneNumber, masked);
    }

    public static DialResponse failure(String message, String contactName, String phoneNumber) {
        return new DialResponse(false, message, null, contactName, phoneNumber, null);
    }

    private static String maskToken(String token) {
        if (token == null || token.isBlank()) return "none";
        if (token.length() <= 12) return token;
        return token.substring(0, 6) + "..." + token.substring(token.length() - 6);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDeviceTokenMasked() {
        return deviceTokenMasked;
    }

    public void setDeviceTokenMasked(String deviceTokenMasked) {
        this.deviceTokenMasked = deviceTokenMasked;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
