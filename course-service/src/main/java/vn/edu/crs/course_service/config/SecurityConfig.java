package vn.edu.crs.course_service.config;

import vn.edu.crs.course_service.security.JwtAuthFilter;

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

                        // API nội bộ
                        .requestMatchers("/internal/**")
                        .permitAll()

                        // Xem danh sách / chi tiết môn học
                        .requestMatchers(
                                HttpMethod.GET,
                                "/courses/**"
                        )
                        .permitAll()

                        // Chỉ ADMIN được tạo môn học
                        .requestMatchers(
                                HttpMethod.POST,
                                "/courses/**"
                        )
                        .hasRole("ADMIN")

                        // Chỉ ADMIN được cập nhật môn học
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/courses/**"
                        )
                        .hasRole("ADMIN")

                        // Chỉ ADMIN được xóa môn học
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/courses/**"
                        )
                        .hasRole("ADMIN")

                        // Các endpoint còn lại phải đăng nhập
                        .anyRequest()
                        .authenticated()
                )

                // Chạy JwtAuthFilter trước filter đăng nhập mặc định
                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}

