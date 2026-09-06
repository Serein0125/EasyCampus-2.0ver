package com.campus.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security配置
 * JWT无状态认证，按接口类型配置访问权限
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configure(http))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ========== 公开接口（无需认证） ==========
                // 用户注册、登录、密码重置
                .requestMatchers("/api/v2/users/register", "/api/v2/users/login").permitAll()
                .requestMatchers("/api/v2/users/reset-password/**").permitAll()
                // 商品浏览（只读）
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v2/products/**").permitAll()
                // 帖子浏览（只读）
                .requestMatchers(HttpMethod.GET, "/api/v2/posts/**").permitAll()
                // 分类浏览（只读）
                .requestMatchers(HttpMethod.GET, "/api/v2/categories/**").permitAll()
                // 搜索
                .requestMatchers("/api/v2/search/**").permitAll()
                // Feed/推荐
                .requestMatchers("/api/v2/feed/**").permitAll()
                // 活动浏览（只读）
                .requestMatchers(HttpMethod.GET, "/api/v2/activities/**").permitAll()
                // 组织浏览（只读）
                .requestMatchers(HttpMethod.GET, "/api/v2/organizations/**").permitAll()
                // 用户公开信息
                .requestMatchers(HttpMethod.GET, "/api/v2/users/{id}").permitAll()
                // 关注/点赞统计（只读）
                .requestMatchers(HttpMethod.GET, "/api/v2/follows/stats").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v2/likes/count").permitAll()
                // 广告套餐查询
                .requestMatchers(HttpMethod.GET, "/api/v2/ads/packages").permitAll()
                // 静态资源
                .requestMatchers("/", "/index.html", "/static/**", "/uploads/**", "/*.js", "/*.css").permitAll()
                // Swagger
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Actuator
                .requestMatchers("/actuator/**").permitAll()

                // ========== 管理员接口 ==========
                .requestMatchers("/api/v2/categories/post", "/api/v2/categories/put", "/api/v2/categories/delete").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v2/categories").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v2/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v2/categories/**").hasRole("ADMIN")
                // 组织审批
                .requestMatchers("/api/v2/organizations/*/approve").hasRole("ADMIN")
                .requestMatchers("/api/v2/organizations/*/reject").hasRole("ADMIN")
                // 帖子加精/置顶
                .requestMatchers("/api/v2/posts/*/pin").hasRole("ADMIN")
                .requestMatchers("/api/v2/posts/*/essence").hasRole("ADMIN")

                // ========== 需要认证的接口 ==========
                .requestMatchers("/api/**").authenticated()

                // ========== 其他请求 ==========
                .anyRequest().permitAll()
            )
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
        config.setAllowedOriginPatterns(java.util.List.of("*"));
        config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(java.util.List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
