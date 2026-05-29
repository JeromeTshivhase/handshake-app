package com.trustmarket.handshake;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.time.ZoneId;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@EnableScheduling
public class HandshakeAppApplication {

	private static final Logger log = LoggerFactory.getLogger(HandshakeAppApplication.class);

	public static void main(String[] args) {
		// Respect APP_TIMEZONE env var (default UTC) and set JVM default timezone early
		String tz = System.getenv().getOrDefault("APP_TIMEZONE", "UTC");
		ZoneId zone = null;
		try {
			// Try direct ZoneId (supports region ids like Europe/Paris and offsets like +02:00)
			zone = ZoneId.of(tz);
		} catch (Exception e) {
			// Accept a few common offset-like inputs such as "UTC+2", "GMT+2", "+2", "2"
			try {
				String s = tz == null ? "" : tz.trim();
				if (s.startsWith("UTC") || s.startsWith("GMT")) {
					s = s.substring(3).trim();
				}
				if (s.isEmpty()) {
					throw new IllegalArgumentException("empty timezone");
				}

				// Ensure string starts with sign
				if (!s.startsWith("+") && !s.startsWith("-")) {
					s = "+" + s;
				}

				// Normalize hour-only inputs like "+2" or "+02" into "+02:00"
				if (s.matches("[+-]\\d{1,2}$")) {
					int hours = Integer.parseInt(s);
					String off = String.format("%+03d:00", hours);
					zone = ZoneId.ofOffset("UTC", java.time.ZoneOffset.of(off));
				} else {
					// Try parsing as an offset (e.g. "+02:00") and convert to a ZoneId like UTC+02:00
					java.time.ZoneOffset zo = java.time.ZoneOffset.of(s);
					zone = ZoneId.ofOffset("UTC", zo);
				}
			} catch (Exception ex2) {
				// Fallback to UTC
				zone = ZoneId.of("UTC");
				log.warn("Invalid APP_TIMEZONE='{}'. Falling back to UTC", tz);
			}
		}

		TimeZone.setDefault(TimeZone.getTimeZone(zone));
		log.info("JVM default timezone set to: {} (from APP_TIMEZONE='{}')", TimeZone.getDefault().getID(), tz);
		SpringApplication.run(HandshakeAppApplication.class, args);
	}

}
