package com.leadzam.clicktocall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class DialRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(
        regexp = "^\\+?[0-9\\s\\-\\(\\)]{7,25}$",
        message = "Phone number must be valid (7-25 characters including digits and optional standard separators)"
    )
    private String phoneNumber;

    @NotBlank(message = "Contact name is required")
    private String contactName;

    private String deviceToken;

    public DialRequest() {
    }

    public DialRequest(String phoneNumber, String contactName) {
        this.phoneNumber = phoneNumber;
        this.contactName = contactName;
    }

    public DialRequest(String phoneNumber, String contactName, String deviceToken) {
        this.phoneNumber = phoneNumber;
        this.contactName = contactName;
        this.deviceToken = deviceToken;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }
}
