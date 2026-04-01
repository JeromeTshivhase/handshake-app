package com.trustmarket.handshake.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
public class AppDomainGuard implements ApplicationRunner {

    private final Environment environment;

    @Value("${app.domain:}")
    private String appDomain;

    @Value("${app.enforce-domain-in-prod:true}")
    private boolean enforceDomainInProd;

    public AppDomainGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isProdProfile = Arrays.stream(activeProfiles)
                .anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p));

        if (!isProdProfile) {
            if (appDomain == null || appDomain.isBlank()) {
                log.warn("APP_DOMAIN is empty. Swagger/OpenAPI will use request host automatically (OK for local dev).");
            }
            return;
        }

        if (!enforceDomainInProd) {
            log.warn("Production profile is active but app.enforce-domain-in-prod=false. APP_DOMAIN validation skipped.");
            return;
        }

        if (appDomain == null || appDomain.isBlank()) {
            throw new IllegalStateException("APP_DOMAIN must be set when running with prod/production profile.");
        }

        if (!(appDomain.startsWith("http://") || appDomain.startsWith("https://"))) {
            throw new IllegalStateException("APP_DOMAIN must start with http:// or https://. Current value: " + appDomain);
        }

        log.info("APP_DOMAIN validated for production profile: {}", appDomain);
    }
}

