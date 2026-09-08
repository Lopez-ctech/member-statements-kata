package com.cu.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Core Accounts API.
 *
 * <p>Start it with {@code ./mvnw spring-boot:run}, or with
 * {@code scripts/run.sh java} ({@code scripts\run.ps1 java} on Windows).
 */
@SpringBootApplication
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
