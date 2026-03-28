package com.trustmarket.handshake.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.verification.email.enabled=false",
        "app.notifications.email.enabled=false",
        "app.messaging.email.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerAcceptsRequiredPayload() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Ndi",
                                  "lastName": "Ene",
                                  "email": "ndi@example.com",
                                  "phone": "+27123456789",
                                  "password": "password123",
                                  "passwordConfirm": "password123",
                                  "city": "Cape Town",
                                  "province": "Western Cape"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("ndi@example.com"));
    }

    @Test
    void registerAlsoAcceptsConfirmPasswordAlias() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Tshi",
                                  "lastName": "Venda",
                                  "email": "tshi@example.com",
                                  "phone": "+27987654321",
                                  "password": "password123",
                                  "confirmPassword": "password123",
                                  "city": "Johannesburg",
                                  "province": "Gauteng"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("tshi@example.com"));
    }
}

