package com.trustmarket.handshake.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustmarket.handshake.auth.service.TotpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TotpService totpService;

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

    @Test
    void twoFaEndpointsWorkForSetupEnableVerifyRegenerateStatusAndDisable() throws Exception {
        String email = "twofa-" + UUID.randomUUID() + "@example.com";

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Two",
                                  "lastName": "Factor",
                                  "email": "%s",
                                  "phone": "+27111222333",
                                  "password": "password123",
                                  "passwordConfirm": "password123",
                                  "city": "Cape Town",
                                  "province": "Western Cape"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String accessToken = registerJson.path("data").path("accessToken").asText();
        assertNotNull(accessToken);

        MvcResult setupAliasResult = mockMvc.perform(post("/api/auth/2fa/setup")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "method": "app"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.secretKey").isNotEmpty())
                .andExpect(jsonPath("$.data.provisioningUri").isNotEmpty())
                .andExpect(jsonPath("$.data.qrCodeUrl").isNotEmpty())
                .andReturn();

        JsonNode setupJson = objectMapper.readTree(setupAliasResult.getResponse().getContentAsString());
        String secret = setupJson.path("data").path("secretKey").asText();
        String code = totpService.generateCode(secret, Instant.now().getEpochSecond() / 30);

        mockMvc.perform(post("/api/auth/2fa/enable")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "verificationCode": "%s"
                                }
                                """.formatted(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        String verifyCode = totpService.generateCode(secret, Instant.now().getEpochSecond() / 30);
        mockMvc.perform(post("/api/auth/2fa/verify")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "%s"
                                }
                                """.formatted(verifyCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        String regenerateCode = totpService.generateCode(secret, Instant.now().getEpochSecond() / 30);
        mockMvc.perform(post("/api/auth/2fa/recovery/regenerate")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "%s"
                                }
                                """.formatted(regenerateCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/auth/2fa/status")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.method").value("app"));

        mockMvc.perform(post("/api/auth/2fa/disable")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        MvcResult disabledStatusResult = mockMvc.perform(get("/api/auth/2fa/status")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andReturn();

        JsonNode statusJson = objectMapper.readTree(disabledStatusResult.getResponse().getContentAsString());
        assertFalse(statusJson.path("data").path("enabled").asBoolean());
    }
}

