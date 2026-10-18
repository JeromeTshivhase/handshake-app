package com.trustmarket.handshake.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Replaces the SMTP JavaMailSender with the Brevo HTTPS API sender when BREVO_API_KEY is set.
 */
@Configuration
@ConditionalOnExpression("!'${app.mail.brevo.api-key:}'.isBlank()")
public class BrevoMailConfig {

    @Bean
    @Primary
    public JavaMailSender javaMailSender(
            @Value("${app.mail.brevo.api-key}") String apiKey,
            @Value("${app.mail.brevo.sender-email:}") String senderEmail,
            @Value("${app.mail.brevo.sender-name:Handshake}") String senderName) {
        return new BrevoApiMailSender(apiKey, senderEmail, senderName);
    }
}
