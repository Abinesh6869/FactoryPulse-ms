package org.cts.factorypulse.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Disables Spring Security's default block-all behaviour on the Gateway.
 *
 * The Gateway does NOT use Spring Security for JWT validation.
 * All authentication/authorisation is handled by JwtAuthFilter (GlobalFilter).
 * Without this config, Spring Security auto-configuration blocks every request
 * with 401 before our GlobalFilter gets a chance to run.
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .anyExchange().permitAll()   // JwtAuthFilter handles auth, not Spring Security
            );
        return http.build();
    }
}
