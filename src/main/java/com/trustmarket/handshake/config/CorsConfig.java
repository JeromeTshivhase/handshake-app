package com.trustmarket.handshake.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${cors.max-age-seconds:86400}")
    private long corsMaxAgeSeconds;

    @Value("${cors.allowed-origins:http://localhost:8081,https://handshake-app-bd3y.onrender.com,https://trustmarket-app-1.onrender.com,https://handshake-app-production-3b09.up.railway.app}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        cfg.setAllowedOrigins(allowedOrigins);

        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Use wildcard headers so Swagger/custom proxy headers don't break preflight.
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(true);
        // Cache successful preflight responses longer to reduce repeated OPTIONS calls.
        cfg.setMaxAge(corsMaxAgeSeconds);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}