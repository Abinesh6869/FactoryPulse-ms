package org.cts.factorypulse.gateway.filter;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.factorypulse.gateway.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Global JWT validation filter.
 *
 * <p>Flow:
 * <ol>
 *   <li>If the path is whitelisted (e.g. /api/auth/login), pass through immediately.</li>
 *   <li>Otherwise require a valid Bearer token in the Authorization header.</li>
 *   <li>On success, forward userId, email, and role as downstream headers so individual
 *       services never have to re-parse the JWT.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    /** Paths that do NOT require a JWT. */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/"         // covers /api/auth/login, /api/auth/register, etc.
    );

    @Override
    public int getOrder() {
        // Run before routing so downstream services always get the enriched headers.
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Bypass JWT check for public endpoints
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // 2. Extract the Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("GW: missing or malformed Authorization header for {}", path);
            return unauthorized(exchange);
        }

        String token = authHeader.substring(7);

        // 3. Validate the token
        if (!jwtUtil.isValid(token)) {
            log.warn("GW: invalid or expired JWT for {}", path);
            return unauthorized(exchange);
        }

        // 4. Extract claims and forward as headers to downstream services
        Claims claims = jwtUtil.extractAllClaims(token);
        Long userId = jwtUtil.extractUserId(token);
        String email = claims.getSubject();
        String role  = jwtUtil.extractRole(token);

        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id",    userId != null ? userId.toString() : "")
                .header("X-User-Email", email  != null ? email  : "")
                .header("X-User-Role",  role   != null ? role   : "")
                .build();

        log.debug("GW: JWT valid — userId={}, role={}, path={}", userId, role, path);
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }
}
