package com.ahni.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // supabase jwt 토큰 검증 구조로 csrf 토큰이 필요 없음
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 서버가 세션을 저장하지 않고 모든 요청에 Bearer 헤더를 포함해야 함
            .authorizeHttpRequests(
                authorize ->
                    authorize
                        // 공개 API
                        .requestMatchers(
                            HttpMethod.GET,
                            "/api/v1/departments"
                        ).permitAll()
                        .requestMatchers(
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/actuator/health/**"
                        ).permitAll()
                        .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults())); // JWT 기반 서버로 동작

        return http.build();
    }
}
