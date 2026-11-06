package com.stockrecommendation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfException;

import com.stockrecommendation.security.AppUser;
import com.stockrecommendation.security.StockSensePasswordEncoder;
import com.stockrecommendation.service.AuthService;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Who may open what - checked on the server for every request.
 *
 *   public      /, /login, /register, static files
 *   ADMIN only  /admin/**
 *   any user    everything else
 *
 * Form login with the email as user name, CSRF protection on every POST
 * (Thymeleaf adds the token to th:action forms), session id changed at login.
 * Services check the admin role a second time before changing data.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new StockSensePasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthService authService) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/css/**", "/js/**", "/img/**", "/error", "/favicon.ico").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler((request, response, authentication) -> {
                    AppUser principal = (AppUser) authentication.getPrincipal();
                    authService.recordLogin(principal.getUser());
                    response.sendRedirect(request.getContextPath()
                            + (principal.getUser().isAdmin() ? "/admin" : "/dashboard") + "?welcome");
                })
                .failureHandler((request, response, exception) -> response.sendRedirect(request.getContextPath()
                        + (exception instanceof DisabledException ? "/login?disabled" : "/login?error")))
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"))
            .exceptionHandling(ex -> ex
                // A missing/expired CSRF token is a plain 403; a USER opening an admin
                // page goes back to the dashboard with a message.
                .accessDeniedHandler((request, response, denied) -> {
                    if (denied instanceof CsrfException) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    } else {
                        response.sendRedirect(request.getContextPath() + "/dashboard?denied");
                    }
                }))
            .headers(headers -> headers.cacheControl(cache -> { }));
        return http.build();
    }
}
