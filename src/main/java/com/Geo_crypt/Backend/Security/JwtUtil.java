package com.Geo_crypt.Backend.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey key;
    private final long expirySeconds;


    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration}") long expirySeconds) {
// Use provided secret to seed key - for demo we create HMAC key from secret bytes
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirySeconds = expirySeconds;
    }


    public String generateToken(String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirySeconds * 1000);
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }


    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
