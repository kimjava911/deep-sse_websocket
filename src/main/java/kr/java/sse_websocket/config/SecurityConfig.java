package kr.java.sse_websocket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 최소 보안 설정(학습/데모용).
 *
 * 목표:
 * - Form Login(세션 기반) 사용
 * - /admin/** 는 ADMIN만 접근
 * - /chat/**, /notifications/**, /sse/** 등은 로그인 사용자만 접근
 * - WebSocket은 같은 세션/쿠키 컨텍스트로 인증이 유지되므로, 핸드셰이크도 인증 정책에 포함됨
 *
 * 주의:
 * - 현재는 InMemoryUserDetailsService로 계정 2개(admin/user1)만 제공
 * - 이후 JPA(UserDetailsService)로 교체하는 것이 정상적인 확장 방향
 */
@EnableMethodSecurity // @PreAuthorize 등 메서드 보안 활성화
@Configuration
public class SecurityConfig {

    /**
     * HTTP 보안 필터 체인.
     * - 요청 URL 단위로 접근 제어를 설정한다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                // API POST(읽음 처리 등) 403 해결용
                                "/api/**",
                                // SockJS/WS는 환경에 따라 CSRF에 걸릴 수 있어(학습 단계) 제외 권장
                                "/ws/**",
                                // SSE 엔드포인트(일반적으로 GET이라 CSRF 영향 적지만, 학습 단계에서는 같이 제외해도 무방)
                                "/sse/**"
                        )
                )
                // (개발 편의) 정적 리소스와 로그인 페이지는 누구나 접근 가능
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico"
                        ).permitAll()

                        // Admin 전용
                        .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")

                        // 로그인 사용자 전용(알림/SSE/채팅/일반 API)
                        .requestMatchers("/notifications/**", "/chat/**", "/sse/**", "/api/**").authenticated()

                        // 그 외는 우선 허용(데모용)
                        .anyRequest().permitAll()
                )
                // Spring Security 기본 로그인 폼 사용
                .formLogin(Customizer.withDefaults())
                // 기본 로그아웃 처리
                .logout(Customizer.withDefaults());

        return http.build();
    }

    /**
     * 최소 구동 체크를 위한 InMemory 사용자 2개.
     * - admin / admin1234 (ROLE_ADMIN)
     * - user1 / user1234 (ROLE_USER)
     *
     * 이후에는 DB 기반 UserDetailsService로 교체한다.
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails admin = User.withUsername("admin")
                .password(encoder.encode("admin1234"))
                .roles("ADMIN")
                .build();

        UserDetails user = User.withUsername("user1")
                .password(encoder.encode("user1234"))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(admin, user);
    }

    /**
     * 비밀번호 인코더(BCrypt).
     * - 운영 환경에서도 표준적으로 권장되는 방식.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        //         return new BCryptPasswordEncoder();
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}