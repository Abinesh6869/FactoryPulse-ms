package org.cts.factorypulse.operations.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.time.LocalDateTime;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String SECRET_KEY;
    private Key key;

    @PostConstruct
    public void init() { this.key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes()); }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    public String extractEmail(String token) { return getClaims(token).getSubject(); }
    public Long extractUserId(String token) { return getClaims(token).get("userId", Long.class); }
    public String extractRole(String token) { return getClaims(token).get("role", String.class); }

    public boolean validateToken(String token) {
        try { Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token); return true; }
        catch (Exception e) { return false; }
    }
}
