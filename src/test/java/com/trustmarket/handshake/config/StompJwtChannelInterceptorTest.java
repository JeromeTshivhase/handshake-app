package com.trustmarket.handshake.config;

import com.trustmarket.handshake.shared.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompJwtChannelInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Test
    void setsUserOnValidAccessToken() {
        StompJwtChannelInterceptor interceptor = new StompJwtChannelInterceptor(jwtService);

        UUID userId = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        when(jwtService.parse("valid-token")).thenReturn(claims);
        when(claims.get("type", String.class)).thenReturn("ACCESS");
        when(claims.get("userId", String.class)).thenReturn(userId.toString());
        when(claims.get("role", String.class)).thenReturn("USER");

        StompHeaderAccessor accessor = connectAccessor("Bearer valid-token");
        interceptor.authenticateConnect(accessor);

        assertNotNull(accessor.getUser());
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) accessor.getUser();
        assertEquals(userId.toString(), auth.getPrincipal());
        assertEquals("ROLE_USER", auth.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void ignoresMalformedUserIdClaim() {
        StompJwtChannelInterceptor interceptor = new StompJwtChannelInterceptor(jwtService);

        Claims claims = mock(Claims.class);
        when(jwtService.parse("bad-claims")).thenReturn(claims);
        when(claims.get("type", String.class)).thenReturn("ACCESS");
        when(claims.get("userId", String.class)).thenReturn("not-a-uuid");
        when(claims.get("role", String.class)).thenReturn("USER");

        StompHeaderAccessor accessor = connectAccessor("Bearer bad-claims");
        interceptor.authenticateConnect(accessor);

        assertNull(accessor.getUser());
    }

    @Test
    void ignoresParseErrors() {
        StompJwtChannelInterceptor interceptor = new StompJwtChannelInterceptor(jwtService);

        when(jwtService.parse("bad-token")).thenThrow(new JwtException("invalid") {
        });

        StompHeaderAccessor accessor = connectAccessor("Bearer bad-token");
        interceptor.authenticateConnect(accessor);

        assertNull(accessor.getUser());
    }

    private StompHeaderAccessor connectAccessor(String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.setNativeHeader("Authorization", authorizationHeader);
        return accessor;
    }
}







