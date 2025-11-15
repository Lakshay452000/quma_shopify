package com.quma.quma_shopify_backend.security;

import com.quma.quma_shopify_backend.filters.JwtAuthenticationFilter;
import com.quma.quma_shopify_backend.filters.RateLimitingFilter;
import com.quma.quma_shopify_backend.filters.UserContextFilter;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Autowired
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @Autowired
        private RateLimitingFilter rateLimitingFilter;

        @Autowired
        private UserContextFilter userContextFilter;

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(Arrays.asList(
                                "https://quma-verse.vercel.app", // fixed production domain
                                "http://localhost:5173" // for local dev
                ));
                config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(Collections.singletonList("*"));
                config.setAllowCredentials(true);
                config.setExposedHeaders(Arrays.asList("Set-Cookie"));
                config.setMaxAge(Duration.ofSeconds(3600));
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http.cors() // ✅ this enables CORS for Spring Security
                                .and()
                                .csrf(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/user/login", "/user/register",
                                                                "/user/reset-password", "/auth/**",
                                                                "/products/list", "/products/search/batch",
                                                                "/search/**",
                                                                "/payment/webhook", "/product-analytics/**",
                                                                "/health/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // preflight
                                                .anyRequest().authenticated())
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        response.setContentType("application/json");
                                                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                                        response.getWriter().write("{\"message\":\"Unauthorized\"}");
                                                }))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(rateLimitingFilter, JwtAuthenticationFilter.class)
                                .addFilterBefore(userContextFilter, RateLimitingFilter.class);

                return http.build();
        }
}
