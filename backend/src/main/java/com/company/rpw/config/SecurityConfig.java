package com.company.rpw.config;

import com.company.rpw.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 配置类
 * 配置JWT认证、密码加密等安全相关功能
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 安全过滤链配置
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF（JWT不需要）
                .csrf(csrf -> csrf.disable())
                // 设置会话管理为无状态（JWT方式不需要session）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置请求授权规则
                .authorizeHttpRequests(auth -> auth
                        // 放行认证相关接口（含适配 yudao-ui-admin-vben 的 /system/auth/*）
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/auth/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/system/auth/login")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/system/auth/refresh-token")).permitAll()
                        // 放开 bpm 流程「谁可以发起」选择器所需的用户/部门/角色/岗位精简列表接口
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/system/user/simple-list")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/system/dept/simple-list")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/system/role/simple-list")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/system/post/simple-list")).permitAll()
                        // 放开字典精简列表（前端路由守卫加载字典缓存用，只读）
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/system/dict-data/simple-list")).permitAll()
                        // 放开字典类型列表（前端表单设计器「字典选择器」下拉用，只读）
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/system/dict-type/**")).permitAll()
                        // 放行Knife4j文档接口
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/doc.html")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/webjars/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/v3/api-docs/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/swagger-resources/**")).permitAll()
                        // 其余请求必须携带有效 JWT（前端 request.ts 自动附加 Bearer Token）
                        .anyRequest().authenticated()
                )
                // 允许跨域（前端 vite dev server 直连时备用；默认走 vite proxy 也可）
                .cors(Customizer.withDefaults())
                // 添加JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 跨域配置：允许前端开发服务器与本地访问
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * 密码编码器（BCrypt）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
