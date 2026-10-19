package com.trustmarket.handshake.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustmarket.handshake.auth.dto.AuthResponse;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.entity.UserRole;
import com.trustmarket.handshake.user.repository.UserRepository;
import com.trustmarket.handshake.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/** Sign in / sign up with a Google ID token. Google verifies the token; we check audience, issuer and email. */
@Service
public class GoogleAuthService {

    private static final String TOKENINFO = "https://oauth2.googleapis.com/tokeninfo?id_token=";
    private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @Value("${google.client-id:}")
    private String clientId;

    public GoogleAuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                             AuthService authService, UserService userService, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    public AuthResponse login(String idToken) {
        if (clientId == null || clientId.isBlank()) {
            throw new BadRequestException("Google sign-in is not configured");
        }
        JsonNode claims = fetchClaims(idToken);

        if (!clientId.equals(claims.path("aud").asText())
                || !ISSUERS.contains(claims.path("iss").asText())
                || !"true".equalsIgnoreCase(claims.path("email_verified").asText())
                || claims.path("email").asText("").isBlank()) {
            throw new BadRequestException("Invalid Google credentials");
        }

        String email = claims.path("email").asText().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

        if (user == null) {
            String given = claims.path("given_name").asText("").trim();
            String family = claims.path("family_name").asText("").trim();
            if (given.isEmpty()) given = email.substring(0, email.indexOf('@'));
            if (family.isEmpty()) family = "-";
            user = userRepository.save(User.builder()
                    .email(email)
                    // Random unusable password; they can set one via "forgot password" if they want.
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID() + UUID.randomUUID().toString()))
                    .firstName(given)
                    .lastName(family)
                    .profilePictureUrl(claims.path("picture").asText(null))
                    .role(UserRole.USER)
                    .emailVerified(true)
                    .emailVerifiedAt(LocalDateTime.now())
                    .build());
        } else {
            userService.assertNotLocked(user);
            if (user.isTwoFaEnabled()) {
                // Don't let Google sign-in bypass the account's second factor.
                throw new BadRequestException("This account uses 2FA. Please sign in with your email, password and 2FA code.");
            }
            if (!user.isEmailVerified()) {
                user.setEmailVerified(true);
                user.setEmailVerifiedAt(LocalDateTime.now());
                user = userRepository.save(user);
            }
            userService.recordLoginSuccess(user);
        }

        return authService.issueTokens(user);
    }

    private JsonNode fetchClaims(String idToken) {
        try {
            var req = HttpRequest.newBuilder(URI.create(TOKENINFO + URLEncoder.encode(idToken, StandardCharsets.UTF_8)))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            var res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) throw new BadRequestException("Invalid Google credentials");
            return objectMapper.readTree(res.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Google verification interrupted", e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not reach Google to verify sign-in", e);
        }
    }
}
