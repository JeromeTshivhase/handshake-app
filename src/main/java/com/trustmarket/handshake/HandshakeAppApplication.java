package com.trustmarket.handshake;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.time.ZoneId;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class HandshakeAppApplication {

	public static void main(String[] args) {
		// Respect APP_TIMEZONE env var (default UTC) and set JVM default timezone early
		String tz = System.getenv().getOrDefault("APP_TIMEZONE", "UTC");
		try {
			TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(tz)));
		} catch (Exception e) {
			// Fallback to UTC if invalid
			TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")));
		}
		SpringApplication.run(HandshakeAppApplication.class, args);
	}

}
