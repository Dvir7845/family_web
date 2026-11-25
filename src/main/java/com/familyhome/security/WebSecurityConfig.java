package com.familyhome.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher sessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http, SessionRegistry sessions)
            throws Exception {
        http.authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers(
                                        "/login.html",
                                        "/assets/**",
                                        "/csrf",
                                        "/error",
                                        "/swagger-ui.html",
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**")
                                .permitAll()
                                .anyRequest()
                                .authenticated())
                .formLogin(form ->
                        form.loginPage("/login.html")
                                .loginProcessingUrl("/login")
                                .defaultSuccessUrl("/", true)
                                .failureUrl("/login.html?error")
                                .permitAll())
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.maximumSessions(-1)
                                .sessionRegistry(sessions)
                                .expiredSessionStrategy(event -> {
                                    event.getSessionInformation().expireNow();
                                    event.getRequest().getSession().invalidate();
                                    String accept = event.getRequest().getHeader("Accept");
                                    if (accept != null && accept.contains("application/json"))
                                        event.getResponse().setStatus(401);
                                    else event.getResponse().sendRedirect("/login.html?expired");
                                }))
                .logout(logout ->
                        logout.logoutSuccessUrl("/login.html?logout")
                                .invalidateHttpSession(true)
                                .clearAuthentication(true)
                                .deleteCookies("JSESSIONID"))
                .exceptionHandling(exceptions ->
                        exceptions.defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                request -> {
                                    String accept = request.getHeader("Accept");
                                    return accept != null && accept.contains("application/json");
                                }));

        return http.build();
    }
}
