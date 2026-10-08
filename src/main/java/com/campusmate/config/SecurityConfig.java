package com.campusmate.config;

import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/register", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/dashboard/admin", "/admin", "/admin/", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/faculty", "/faculty/", "/faculty/**").hasRole("FACULTY")
                        .requestMatchers("/profile", "/planner", "/planner/**").hasRole("STUDENT")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            String selectedLoginType = normalizeLoginType(request.getParameter("loginType"));
                            String actualRole = authentication.getAuthorities().stream()
                                    .map(grantedAuthority -> grantedAuthority.getAuthority())
                                    .filter(authority -> authority.startsWith("ROLE_"))
                                    .map(authority -> authority.substring(5))
                                    .findFirst()
                                    .orElse("STUDENT");

                            if (selectedLoginType != null && !selectedLoginType.equals(actualRole)) {
                                var session = request.getSession(false);
                                if (session != null) {
                                    session.invalidate();
                                }
                                response.sendRedirect("/login?errorRole");
                                return;
                            }

                            var session = request.getSession(true);
                            session.setAttribute("userName", authentication.getName());
                            session.setAttribute("userRole", actualRole);

                            String redirect = switch (actualRole) {
                                case "ADMIN" -> "/dashboard/admin";
                                case "FACULTY" -> "/faculty/dashboard";
                                default -> "/dashboard";
                            };

                            response.sendRedirect(redirect);
                        })
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll());

        return http.build();
    }

    private String normalizeLoginType(String loginType) {
        if (loginType == null || loginType.isBlank()) {
            return null;
        }
        String normalized = loginType.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("TEACHER")) {
            return "FACULTY";
        }
        return normalized;
    }
}
