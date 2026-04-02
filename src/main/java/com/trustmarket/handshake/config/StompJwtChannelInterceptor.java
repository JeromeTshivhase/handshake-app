package com.trustmarket.handshake.config;

import com.trustmarket.handshake.shared.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnect(accessor);
        }

        return message;
    }

    void authenticateConnect(StompHeaderAccessor accessor) {
        String rawHeader = firstHeader(accessor, HttpHeaders.AUTHORIZATION);
        if (rawHeader == null) {
            rawHeader = firstHeader(accessor, "authorization");
        }
        if (rawHeader == null) {
            rawHeader = firstHeader(accessor, "token");
        }

        if (rawHeader == null || !rawHeader.startsWith("Bearer ")) {
            return;
        }

        String token = rawHeader.substring(7).trim();
        if (token.isEmpty()) {
            return;
        }

        try {
            Claims claims = jwtService.parse(token);
            String type = claims.get("type", String.class);
            if (!"ACCESS".equals(type)) {
                return;
            }

            String userId = claims.get("userId", String.class);
            String role = claims.get("role", String.class);
            if (userId == null || userId.isBlank() || role == null || role.isBlank()) {
                return;
            }

            // Validate UUID shape for consistency with HTTP JWT auth path.
            UUID.fromString(userId);
            String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;

            var auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority(authority))
            );
            accessor.setUser(auth);
        } catch (JwtException | IllegalArgumentException ignored) {
            // Ignore malformed auth headers and continue CONNECT without a user.
        }
    }

    private String firstHeader(StompHeaderAccessor accessor, String name) {
        List<String> values = accessor.getNativeHeader(name);
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }
}


