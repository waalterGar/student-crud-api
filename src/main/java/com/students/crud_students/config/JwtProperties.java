package com.students.crud_students.config;

import io.jsonwebtoken.io.Decoders;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtProperties {

    private String secret;
    private long expirationMs;
    private long refreshExpirationMs;

    @PostConstruct
    public void validateSecretKey() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret key must be configured in properties (app.jwt.secret)");
        }
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret key must be at least 256 bits (32 bytes) for HMAC-SHA256 algorithm");
        }
    }
}