package vn.edu.crs.registration_service.config;

import vn.edu.crs.registration_service.security.JwtAuthFilter;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Tất cả endpoint registration đều yêu cầu đăng nhập
                        .requestMatchers("/registrations/**")
                        .authenticated()

                        // Các endpoint khác không yêu cầu xác thực
                        .anyRequest()
                        .permitAll()
                )

                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}