package com.campusmate.config;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {
 @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
  http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/","/login","/register","/css/**").permitAll()
    .requestMatchers("/admin/**","/dashboard/admin").hasRole("ADMIN")
    .requestMatchers("/dashboard").hasRole("STUDENT")
    .anyRequest().authenticated())
   .formLogin(form -> form.loginPage("/login").loginProcessingUrl("/login").usernameParameter("email")
    .successHandler((request,response,authentication)->{
     HttpSession session=request.getSession(true);
     session.setAttribute("userName",authentication.getName());
     session.setAttribute("userRole",authentication.getAuthorities().stream().findFirst().map(a->a.getAuthority()).orElse("ROLE_STUDENT"));
     boolean admin=authentication.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"));
     response.sendRedirect(admin?"/dashboard/admin":"/dashboard");
    }).failureUrl("/login?error").permitAll())
   .logout(logout->logout.logoutUrl("/logout").invalidateHttpSession(true).deleteCookies("JSESSIONID").logoutSuccessUrl("/login?logout").permitAll());
  return http.build();
 }
}
