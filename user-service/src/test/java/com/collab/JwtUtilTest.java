package com.collab;

import com.collab.security.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {
    private static final String TEST_SECRET = "test-only-signing-material-not-for-deployment-123456";
    private final JwtUtil jwt = new JwtUtil(TEST_SECRET);

    @Test
    void rejectsMissingAndWeakConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil(null));
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil(""));
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil(" ".repeat(40)));
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil("short"));
    }

    @Test
    void generatedTokenMatchesOnlyItsSubject() {
        String token = jwt.generateToken("alice");
        assertTrue(jwt.validateToken(token, "alice"));
        assertFalse(jwt.validateToken(token, "bob"));
        assertFalse(jwt.validateToken(token, null));
        assertFalse(jwt.validateToken(token, ""));
    }

    @Test
    void rejectsMalformedAndMissingTokens() {
        assertFalse(jwt.validateToken("not-a-token", "alice"));
        assertFalse(jwt.validateToken("", "alice"));
        assertFalse(jwt.validateToken(null, "alice"));
    }

    @Test
    void rejectsTokenFromAnotherSigningKey() {
        JwtUtil other = new JwtUtil("another-test-only-signing-material-never-use-in-deployment");
        assertFalse(jwt.validateToken(other.generateToken("alice"), "alice"));
    }

    @Test
    void rejectsExpiredToken() {
        String expired = Jwts.builder().setSubject("alice")
            .setIssuedAt(new Date(System.currentTimeMillis() - 120000))
            .setExpiration(new Date(System.currentTimeMillis() - 60000))
            .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
            .compact();
        assertFalse(jwt.validateToken(expired, "alice"));
    }
}
