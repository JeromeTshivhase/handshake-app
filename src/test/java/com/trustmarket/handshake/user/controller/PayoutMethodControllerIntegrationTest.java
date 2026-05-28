package com.trustmarket.handshake.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.UserRepository;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
class PayoutMethodControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void upsertAndGetPayoutMethodWithMockedAuth() throws Exception {
        // create a user directly to avoid full registration flow
        User u = User.builder()
                .email("pm-" + java.util.UUID.randomUUID() + "@example.com")
                .passwordHash("x")
                .firstName("P")
                .lastName("M")
                .build();
        u = userRepository.save(u);

        TestingAuthenticationToken auth = new TestingAuthenticationToken(u.getId().toString(), null);
        auth.setAuthenticated(true);

        // upsert payout method
        mockMvc.perform(post("/api/payout-methods/me")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bankName": "Test Bank",
                                  "branchCode": "123456",
                                  "accountNumber": "0123456789",
                                  "identityFingerprint": "fp-test"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bankName").value("Test Bank"));

        // get payout method
        mockMvc.perform(get("/api/payout-methods/me")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}


