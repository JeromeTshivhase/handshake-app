package com.trustmarket.handshake.config;

import com.trustmarket.handshake.shared.security.JwtService;
import io.jsonwebtoken.Claims;
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

@Component
@RequiredArgsConstructor
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String rawHeader = firstHeader(accessor, HttpHeaders.AUTHORIZATION);
            if (rawHeader == null) {
                rawHeader = firstHeader(accessor, "authorization");
            }
            if (rawHeader == null) {
                rawHeader = firstHeader(accessor, "token");
            }

            if (rawHeader != null && rawHeader.startsWith("Bearer ")) {
                String token = rawHeader.substring(7);
                if (jwtService.isValid(token)) {
                    Claims claims = jwtService.parse(token);
                    String type = (String) claims.get("type");
                    if ("ACCESS".equals(type)) {
                        String userId = (String) claims.get("userId");
                        String role = (String) claims.get("role");

                        var auth = new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                        accessor.setUser(auth);
                    }
                }
            }
        }

        return message;
    }

    private String firstHeader(StompHeaderAccessor accessor, String name) {
        List<String> values = accessor.getNativeHeader(name);
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }
}


