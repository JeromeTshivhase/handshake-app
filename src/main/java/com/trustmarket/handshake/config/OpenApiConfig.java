package com.trustmarket.handshake.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.domain:}")
    private String appDomain;

    @Bean
    public OpenAPI handshakeOpenApi() {
        final String schemeName = "bearerAuth";

        OpenAPI api = new OpenAPI()
                .info(new Info()
                        .title("Handshake API")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));

        // If no explicit domain is configured, Swagger UI uses same-origin host automatically.
        if (appDomain != null && !appDomain.isBlank()) {
            api.servers(List.of(new Server().url(appDomain).description("API server")));
        }

        return api;
    }
}

