package org.cts.factorypulse.operations.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtFilter jwtFilter;
    private final ServiceKeyFilter serviceKeyFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                // ── INTERNAL (service-to-service) ─────────────────────────────
                .requestMatchers("/internal/**").hasRole("INTERNAL")

                // Root Causes
                .requestMatchers(HttpMethod.POST,   "/api/rootcauses/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/rootcauses/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,    "/api/rootcauses/**")
                    .hasAnyRole("ADMIN", "SUPERVISOR", "OPERATOR", "TECHNICIAN", "QUALITY_ENGINEER", "MANAGER")

                // Alert Rules
                .requestMatchers("/api/alertrules/**").hasRole("ADMIN")

                // Downtimes
                .requestMatchers(HttpMethod.POST,  "/api/downtimes/**").hasAnyRole("ADMIN", "SUPERVISOR", "OPERATOR")
                .requestMatchers(HttpMethod.PATCH, "/api/downtimes/**").hasAnyRole("ADMIN", "SUPERVISOR", "OPERATOR")
                .requestMatchers(HttpMethod.GET,   "/api/downtimes/**")
                    .hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR", "OPERATOR", "TECHNICIAN", "QUALITY_ENGINEER")

                // Work Orders
                .requestMatchers(HttpMethod.POST,   "/api/workorders/**").hasAnyRole("ADMIN", "SUPERVISOR")
                .requestMatchers(HttpMethod.PATCH,  "/api/workorders/**").hasAnyRole("ADMIN", "SUPERVISOR", "TECHNICIAN")
                .requestMatchers(HttpMethod.GET,    "/api/workorders/**")
                    .hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR", "TECHNICIAN")

                // Maintenance Logs
                .requestMatchers(HttpMethod.POST,   "/api/maintenance-logs/**").hasAnyRole("ADMIN", "TECHNICIAN")
                .requestMatchers(HttpMethod.GET,    "/api/maintenance-logs/**")
                    .hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR", "TECHNICIAN")

                // Alerts & Notifications
                .requestMatchers(HttpMethod.PATCH, "/api/alerts/*/resolve").hasAnyRole("ADMIN", "SUPERVISOR")
                .requestMatchers(HttpMethod.PATCH, "/api/alerts/notifications/**")
                    .hasAnyRole("ADMIN", "SUPERVISOR", "TECHNICIAN", "OPERATOR")
                .requestMatchers(HttpMethod.GET,   "/api/alerts/**")
                    .hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR", "OPERATOR", "TECHNICIAN")

                .anyRequest().authenticated()
            )
            .addFilterBefore(serviceKeyFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
