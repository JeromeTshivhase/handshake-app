package com.trustmarket.handshake.user.sms;

import com.trustmarket.handshake.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.*;

/**
 * Africa's Talking SMS sender that will try to use the official SDK when available on the classpath.
 * If the SDK is not present, it falls back to an HTTP POST to the REST endpoint.
 * Supports optional async dispatch via a small thread pool controlled by properties.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "verification.sms.africastalking", name = "enabled", havingValue = "true")
public class AfricasTalkingSmsSender implements SmsSender {

    @Value("${verification.sms.africastalking.username:}")
    private String username;

    @Value("${verification.sms.africastalking.api-key:}")
    private String apiKey;

    @Value("${verification.sms.africastalking.from:}")
    private String from;

    @Value("${verification.sms.africastalking.async.enabled:true}")
    private boolean asyncEnabled;

    @Value("${verification.sms.africastalking.async.pool-size:2}")
    private int asyncPoolSize;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private volatile ExecutorService asyncExecutor;

    private static final String REST_API_URL = "https://api.africastalking.com/version1/messaging";

    @Override
    public void sendSms(User user, String phone, String message) {
        if (username == null || username.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Africa's Talking configuration is incomplete: username/api-key required");
        }

        if (asyncEnabled) {
            ensureAsyncExecutor();
            try {
                asyncExecutor.submit(() -> sendSync(user, phone, message));
            } catch (RejectedExecutionException e) {
                log.warn("Async SMS submission rejected, attempting sync send: {}", e.getMessage());
                sendSync(user, phone, message);
            }
        } else {
            sendSync(user, phone, message);
        }
    }

    private void ensureAsyncExecutor() {
        if (asyncExecutor == null) {
            synchronized (this) {
                if (asyncExecutor == null) {
                    asyncExecutor = Executors.newFixedThreadPool(Math.max(1, asyncPoolSize));
                }
            }
        }
    }

    private void sendSync(User user, String phone, String message) {
        // First try to use the official SDK via reflection to avoid a hard compile dependency.
        try {
            Class<?> atClass = Class.forName("com.africastalking.AfricasTalking");
            // AfricasTalking.initialize(username, apiKey);
            Method init = atClass.getMethod("initialize", String.class, String.class);
            init.invoke(null, username, apiKey);

            // SmsService sms = AfricasTalking.getService(AfricasTalking.SERVICE_SMS);
            Method getService = atClass.getMethod("getService", String.class);
            // SERVICE_SMS constant
            String svcConst = null;
            try {
                svcConst = (String) atClass.getField("SERVICE_SMS").get(null);
            } catch (NoSuchFieldException ignored) {
            }
            Object smsService = getService.invoke(null, svcConst == null ? "sms" : svcConst);

            // Call smsService.send(message, recipients, from, null)
            Method sendMethod = smsService.getClass().getMethod("send", String.class, String.class, String.class, Object.class);
            Object result = sendMethod.invoke(smsService, message, phone, from, null);
            log.debug("Africa's Talking SDK send result: {}", result);
            return;
        } catch (ClassNotFoundException e) {
            log.debug("Africa's Talking SDK not found on classpath, falling back to REST API");
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            log.warn("Error invoking Africa's Talking SDK via reflection: {}", e.getMessage());
            // fallthrough to REST fallback
        }

        // REST fallback (simple blocking POST). We keep this minimal and robust with timeouts.
        try {
            java.net.URL url = new java.net.URL(REST_API_URL);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
            conn.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("apiKey", apiKey);

            StringBuilder body = new StringBuilder();
            body.append("username=").append(urlEncode(username));
            body.append("&to=").append(urlEncode(phone));
            body.append("&message=").append(urlEncode(message));
            if (from != null && !from.isBlank()) {
                body.append("&from=").append(urlEncode(from));
            }

            byte[] out = body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(out.length);
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(out);
            }

            int status = conn.getResponseCode();
            java.io.InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
            String resp = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            if (status >= 200 && status < 300) {
                log.debug("Africa's Talking REST response: {}", resp);
            } else {
                throw new RuntimeException("Africa's Talking REST API returned status " + status + " body=" + resp);
            }
        } catch (Exception e) {
            log.warn("Africa's Talking SMS send failed for {}: {}", phone, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}


