package com.trustmarket.handshake.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JavaMailSender that delivers over Brevo's HTTPS API (port 443) instead of SMTP.
 * Existing callers keep using JavaMailSender / SimpleMailMessage unchanged.
 */
@Slf4j
public class BrevoApiMailSender extends JavaMailSenderImpl {

    private static final URI ENDPOINT = URI.create("https://api.brevo.com/v3/smtp/email");

    private final String apiKey;
    private final String senderEmail;
    private final String senderName;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public BrevoApiMailSender(String apiKey, String senderEmail, String senderName) {
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }

    @Override
    protected void doSend(MimeMessage[] mimeMessages, Object[] originalMessages) throws MailException {
        for (MimeMessage mime : mimeMessages) {
            sendOne(mime);
        }
    }

    private void sendOne(MimeMessage mime) {
        final String json;
        try {
            Map<String, Object> body = new LinkedHashMap<>();

            String fromEmail = senderEmail;
            if (fromEmail == null || fromEmail.isBlank()) {
                jakarta.mail.Address[] from = mime.getFrom();
                if (from != null && from.length > 0) {
                    fromEmail = ((InternetAddress) from[0]).getAddress();
                }
            }
            if (fromEmail == null || fromEmail.isBlank()) {
                throw new MailPreparationException("No sender configured (set BREVO_SENDER_EMAIL or MAIL_FROM)");
            }
            Map<String, String> sender = new LinkedHashMap<>();
            sender.put("email", fromEmail);
            if (senderName != null && !senderName.isBlank()) {
                sender.put("name", senderName);
            }
            body.put("sender", sender);

            List<Map<String, String>> to = new ArrayList<>();
            jakarta.mail.Address[] recipients = mime.getRecipients(Message.RecipientType.TO);
            if (recipients != null) {
                for (jakarta.mail.Address a : recipients) {
                    to.add(Map.of("email", ((InternetAddress) a).getAddress()));
                }
            }
            if (to.isEmpty()) {
                throw new MailPreparationException("No recipients");
            }
            body.put("to", to);

            body.put("subject", mime.getSubject() == null ? "" : mime.getSubject());

            Object content = mime.getContent();
            String text = content instanceof String ? (String) content : String.valueOf(content);
            boolean html = mime.getContentType() != null && mime.getContentType().toLowerCase().startsWith("text/html");
            body.put(html ? "htmlContent" : "textContent", text);

            json = objectMapper.writeValueAsString(body);
        } catch (MailException e) {
            throw e;
        } catch (Exception e) {
            throw new MailPreparationException("Could not prepare Brevo API email", e);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                    .timeout(Duration.ofSeconds(15))
                    .header("api-key", apiKey)
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new MailSendException("Brevo API rejected email: HTTP " + response.statusCode()
                        + " " + response.body());
            }
            log.info("Brevo API: email accepted (HTTP {})", response.statusCode());
        } catch (MailException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MailSendException("Interrupted while calling Brevo API", e);
        } catch (Exception e) {
            throw new MailSendException("Brevo API call failed: " + e.getMessage(), e);
        }
    }
}
