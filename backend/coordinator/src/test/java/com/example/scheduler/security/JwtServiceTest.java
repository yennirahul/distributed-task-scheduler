package com.example.scheduler.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    @Test
    void signsAndVerifiesTokenWithBase64EncodedThirtyTwoByteKey() {
        byte[] keyBytes = new byte[32];
        String encodedKey = Base64.getEncoder().encodeToString(keyBytes);
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", encodedKey);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86_400_000L);

        String token = jwtService.generateToken("alice", "USER");
        Jws<Claims> parsedToken = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(keyBytes))
                .build()
                .parseSignedClaims(token);

        assertEquals("HS256", parsedToken.getHeader().getAlgorithm());
        Claims claims = parsedToken.getPayload();
        assertEquals("alice", claims.getSubject());
        assertEquals("USER", claims.get("role"));
    }

    @Test
    void rejectsSigningKeyShorterThanThirtyTwoDecodedBytes() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", Base64.getEncoder().encodeToString(new byte[31]));

        assertThrows(WeakKeyException.class, jwtService::validateSecretKey);
    }
}
