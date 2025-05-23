package com.weather.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)  // enables @PreAuthorize
public class SecurityConfig {

    /**
     * Defines two in-memory users:
     *  • user / password    → ROLE_USER
     *  • admin / adminpass  → ROLE_ADMIN
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails user = User.builder()
                .username("user")
                .password(encoder.encode("password"))
                .roles("USER")
                .build();

        UserDetails admin = User.builder()
                .username("admin")
                .password(encoder.encode("adminpass"))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(user, admin);
    }

    /** Use BCrypt for password hashing. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Main HTTP security filter chain. */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // disable CSRF for our stateless REST API
                .csrf(csrf -> csrf.disable())

                // CORS can be configured separately
                .cors(Customizer.withDefaults())

                // no sessions
                .sessionManagement(sm -> sm.sessionCreationPolicy(
                        org.springframework.security.config.http.SessionCreationPolicy.STATELESS))

                // authorize
                .authorizeHttpRequests(auth -> auth
                        // public endpoints (if any) go here:
                        .requestMatchers("/public/**").permitAll()

                        // otherwise require authentication:
                        .anyRequest().authenticated()
                )

                // basic auth
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
