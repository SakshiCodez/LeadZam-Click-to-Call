package com.leadzam.clicktocall;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadzam.clicktocall.dto.DeviceRegistrationRequest;
import com.leadzam.clicktocall.dto.DialRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "leadzam.firebase.mock-mode=true"
})
@AutoConfigureMockMvc
public class DialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testValidDialRequest_WithExplicitDeviceToken() throws Exception {
        DialRequest request = new DialRequest("+919876543210", "Rahul Sharma", "sample-fcm-device-token-12345");

        mockMvc.perform(post("/api/dial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.contactName").value("Rahul Sharma"))
                .andExpect(jsonPath("$.phoneNumber").value("+919876543210"))
                .andExpect(jsonPath("$.messageId").exists());
    }

    @Test
    void testInvalidPhoneNumber_ReturnsBadRequest() throws Exception {
        DialRequest request = new DialRequest("invalid-phone-num", "Rahul Sharma", "sample-token");

        mockMvc.perform(post("/api/dial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void testMissingContactName_ReturnsBadRequest() throws Exception {
        DialRequest request = new DialRequest("+919876543210", "", "sample-token");

        mockMvc.perform(post("/api/dial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.details.contactName").exists());
    }

    @Test
    void testMissingDeviceToken_WhenNoDeviceRegistered_ReturnsBadRequest() throws Exception {
        DialRequest request = new DialRequest("+919876543210", "Rahul Sharma", null);

        mockMvc.perform(post("/api/dial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Invalid Request"));
    }

    @Test
    void testDeviceRegistrationAndSubsequentDial() throws Exception {
        // 1. Register device
        DeviceRegistrationRequest reg = new DeviceRegistrationRequest("registered-fcm-token-abcdef", "Pixel 7 Pro");

        mockMvc.perform(post("/api/register-device")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 2. Check device status
        mockMvc.perform(get("/api/device-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(true))
                .andExpect(jsonPath("$.deviceName").value("Pixel 7 Pro"));

        // 3. Dial without token; should resolve registered token
        DialRequest dialReq = new DialRequest("+919876543211", "Priya Verma");

        mockMvc.perform(post("/api/dial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dialReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.contactName").value("Priya Verma"));
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
