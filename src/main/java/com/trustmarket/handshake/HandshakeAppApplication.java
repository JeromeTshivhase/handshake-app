package com.trustmarket.handshake;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HandshakeAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(HandshakeAppApplication.class, args);
	}

}
