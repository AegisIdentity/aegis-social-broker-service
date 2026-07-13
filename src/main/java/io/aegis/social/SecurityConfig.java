package io.aegis.social;

import io.aegis.commons.security.SecurityHardening;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Resource-server security baseline (default-deny, shared hardening headers, 401-not-302).
 * Endpoint-specific scopes are added as real endpoints are implemented. */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        SecurityHardening.applyHardeningHeaders(http);
        SecurityHardening.statelessBearerApi(http);
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        // Per-tenant identity-provider registry (console self-service).
                        .requestMatchers("/api/v1/identity-providers", "/api/v1/identity-providers/**")
                        .hasAuthority("SCOPE_idp:admin")
                        // Service-to-service: the authorization-server resolves provider config to broker
                        // federated logins. Only the AS's own service token carries idp:resolve.
                        .requestMatchers("/internal/**").hasAuthority("SCOPE_idp:resolve")
                        .requestMatchers("/api/**").hasAuthority("SCOPE_social:admin")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
