package com.trustmarket.handshake.messaging.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MessageSafetyService {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern HANDLE_PATTERN = Pattern.compile("(?<![A-Za-z0-9_])@[A-Za-z0-9_]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?:\\+?\\d[\\d\\s\\-()]{7,}\\d)");

    private static final Set<String> SUSPICIOUS_TLDS = Set.of(".xyz", ".top", ".click", ".win", ".shop", ".info");
    private static final Set<String> PHISHING_KEYWORDS = Set.of("verify account", "reset password", "enter otp", "wallet unlock", "login now", "bank details");

    public MessageSafetyAssessment assess(String content) {
        if (content == null || content.isBlank()) {
            return MessageSafetyAssessment.safe(content == null ? "" : content);
        }

        Set<String> flags = new LinkedHashSet<>();
        String sanitized = maskSuspiciousUrls(content, flags);

        if (HANDLE_PATTERN.matcher(content).find()) {
            flags.add("SOCIAL_MEDIA_HANDLE");
        }
        if (PHONE_PATTERN.matcher(content).find()) {
            flags.add("PHONE_NUMBER");
        }

        String lower = content.toLowerCase();
        for (String keyword : PHISHING_KEYWORDS) {
            if (lower.contains(keyword)) {
                flags.add("PHISHING_KEYWORD");
                break;
            }
        }

        boolean phishingUrl = flags.contains("PHISHING_URL");
        return new MessageSafetyAssessment(
                sanitized,
                flags,
                phishingUrl || flags.contains("PHISHING_KEYWORD")
        );
    }

    private String maskSuspiciousUrls(String content, Set<String> flags) {
        Matcher matcher = URL_PATTERN.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String url = matcher.group();
            String replacement = url;
            if (isSuspiciousUrl(url)) {
                flags.add("PHISHING_URL");
                replacement = "[BLOCKED_URL]";
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private boolean isSuspiciousUrl(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) {
                return true;
            }
            String h = host.toLowerCase();
            for (String tld : SUSPICIOUS_TLDS) {
                if (h.endsWith(tld)) {
                    return true;
                }
            }
            return h.contains("payfa5t") || h.contains("h4ndshake") || h.contains("payf4st");
        } catch (Exception e) {
            return true;
        }
    }

    public record MessageSafetyAssessment(
            String sanitizedContent,
            Set<String> flags,
            boolean highRisk
    ) {
        public static MessageSafetyAssessment safe(String content) {
            return new MessageSafetyAssessment(content, Set.of(), false);
        }

        public boolean hasFlag(String flag) {
            return flags.contains(flag);
        }
    }
}

