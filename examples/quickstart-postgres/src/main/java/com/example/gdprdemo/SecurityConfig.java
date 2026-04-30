package com.example.gdprdemo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import com.iambilotta.gdpr.starter.audit.ActorResolver;

/**
 * Demo-grade Spring Security wiring. NOT a production template:
 * - In-memory users with hardcoded credentials.
 * - HTTP basic auth, no rotation, no MFA.
 * - CSRF disabled on the GDPR endpoints to keep curl friendly.
 *
 * <p>Production pattern: replace InMemoryUserDetailsManager with your IdP (OAuth2 /
 * SAML / LDAP), restrict /gdpr/** to a role granted only to DPO/legal/ops, log all
 * /gdpr/** invocations to the audit trail itself.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/gdpr/**").hasRole("DPO")
                        .requestMatchers("/customers/**").authenticated()
                        .anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    InMemoryUserDetailsManager users(PasswordEncoder encoder) {
        UserDetails app = User.withUsername("app")
                .password(encoder.encode("app-secret"))
                .roles("USER")
                .build();
        UserDetails dpo = User.withUsername("dpo")
                .password(encoder.encode("dpo-secret"))
                .roles("DPO")
                .build();
        return new InMemoryUserDetailsManager(app, dpo);
    }

    /**
     * Replaces the starter's default {@link ActorResolver} (which returns "system")
     * with one that pulls the principal name from the Spring Security context.
     * Audit rows now carry the actual authenticated user.
     */
    @Bean
    ActorResolver gdprActorResolver() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null ? auth.getName() : "system";
        };
    }
}
