package cn.aiclub.security;

import cn.aiclub.mapper.AccountMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  SecurityFilterChain chain(HttpSecurity http, AccountMapper accounts) throws Exception {
    http.csrf(c -> c.csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/api/auth/csrf",
                        "/api/auth/login",
                        "/api/public/**",
                        "/api/media/**",
                        "/error")
                    .permitAll()
                    .requestMatchers("/api/admin/accounts", "/api/admin/accounts/**")
                    .hasRole("SUPER_ADMIN")
                    .requestMatchers(
                        "/api/admin/**",
                        "/api/auth/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                    .hasAnyRole("ADMIN", "SUPER_ADMIN")
                    .anyRequest()
                    .denyAll())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint((q, s, x) -> json(s, 401, "请先登录"))
                    .accessDeniedHandler((q, s, x) -> json(s, 403, "权限不足或安全令牌已失效，请刷新重试")))
        .requestCache(c -> c.disable())
        .formLogin(c -> c.disable())
        .httpBasic(c -> c.disable())
        .logout(c -> c.disable());
    http.addFilterBefore(
        new OncePerRequestFilter() {
          protected void doFilterInternal(
              HttpServletRequest q, HttpServletResponse s, FilterChain chain)
              throws ServletException, IOException {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof ClubPrincipal p) {
              var account = accounts.selectById(p.id());
              if (account == null
                  || !account.getEnabled()
                  || account.getAuthVersion() != p.version()) {
                var session = q.getSession(false);
                if (session != null) session.invalidate();
                SecurityContextHolder.clearContext();
              }
            }
            chain.doFilter(q, s);
          }
        },
        AnonymousAuthenticationFilter.class);
    return http.build();
  }

  private static void json(HttpServletResponse response, int status, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write("{\"message\":\"" + message + "\"}");
  }
}
