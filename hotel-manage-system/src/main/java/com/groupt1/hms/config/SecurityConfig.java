package com.groupt1.hms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static final String[] PUBLIC_ENDPOINTS = {
            "/login",
            "/register",
            "/logout",
            "/css/**",
            "/js/**",
            "/images/**",
            "/fonts/**",
            "/style/**",
            "/",
            "/*.html",
            "/forgot-password/**",
            "/api/auth/**",
            "/api/public/**"
    };

    private static final String[] RECEPTIONIST_ENDPOINTS = {
            "/receptionist/**"
    };

    private static final String[] MANAGER_ENDPOINTS = {
            "/manager/**"
    };

    private static final String[] ADMIN_ENDPOINTS = {
            "/admin/**"
    };
    private static final String[] CUSTOMER_ENDPOINTS = {
            "/customer/**"
    };



    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .addFilterBefore(new SessionAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(ADMIN_ENDPOINTS).hasRole("ADMIN")
                        .requestMatchers(MANAGER_ENDPOINTS).hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(RECEPTIONIST_ENDPOINTS).hasAnyRole("RECEPTIONIST", "ADMIN")
                        .requestMatchers(CUSTOMER_ENDPOINTS).hasAnyRole("CUSTOMER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendRedirect("/login.html");
                        })
                );

        return http.build();
    }
}
