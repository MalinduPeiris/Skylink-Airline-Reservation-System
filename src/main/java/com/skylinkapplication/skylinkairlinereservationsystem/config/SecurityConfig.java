package com.skylinkapplication.skylinkairlinereservationsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationSuccessHandler authenticationSuccessHandler,
                                                   AuthenticationProvider authenticationProvider) throws Exception {

        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");

        http
                .authorizeHttpRequests(authorize -> authorize

                         .requestMatchers(
                                "/",
                                "/index",
                                "/index.html",
                                "/register.html",
                                "/css/**",
                                "/js/**",
                                "/static/**",
                                "/images/**",
                                "/favicon.ico"
                        ).permitAll()

                         .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/password-reset",
                                "/api/auth/logout",
                                "/logout"
                        ).permitAll()

                         .requestMatchers(
                                "/api/flight/search",
                                "/api/flight/all",
                                "/flights",
                                "/deals",
                                "/api/marketing/promotions",
                                "/api/marketing/promotion/**"
                        ).permitAll()

                         .requestMatchers("/dashboard/it-system-engineer/**")
                        .hasRole("IT_SYSTEM_ENGINEER")

                        .requestMatchers("/dashboard/flight-admin/**")
                        .hasAnyRole("IT_SYSTEM_ENGINEER", "RESERVATION_MANAGER")

                        .requestMatchers("/dashboard/reservation-manager/**")
                        .hasAnyRole("RESERVATION_MANAGER", "IT_SYSTEM_ENGINEER")

                        .requestMatchers(
                                "/dashboard/finance",
                                "/dashboard/finance/**",
                                "/api/finance/**"
                        ).hasAnyRole("FINANCE_EXECUTIVE", "IT_SYSTEM_ENGINEER")

                        .requestMatchers(
                                "/dashboard/marketing-manager/**",
                                "/dashboard/promotion-management/**"
                        ).hasAnyRole("MARKETING_EXECUTIVE", "IT_SYSTEM_ENGINEER")

                        .requestMatchers("/dashboard/customer-support/**")
                        .hasAnyRole("CUSTOMER_SUPPORT_OFFICER", "IT_SYSTEM_ENGINEER")

                        .requestMatchers(
                                "/dashboard/admin/**",
                                "/dashboard/user-management/**"
                        ).hasRole("IT_SYSTEM_ENGINEER")

                         .requestMatchers("/dashboard/**").authenticated()

                         .requestMatchers("/api/booking/**").authenticated()
                        .requestMatchers("/api/payment/**").authenticated()
                        .requestMatchers("/api/user/**").authenticated()
                        .requestMatchers("/api/support/**").authenticated()
                        .requestMatchers("/api/feedback/**").authenticated()
                        .requestMatchers("/api/marketing/**").authenticated()

                         .requestMatchers(
                                "/booking",
                                "/booking-confirmation",
                                "/traveler-payment"
                        ).authenticated()

                         .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/api/auth/login")
                        .loginProcessingUrl("/api/auth/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(authenticationSuccessHandler)
                        .failureUrl("/api/auth/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessUrl("/?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendRedirect("/api/auth/login"))
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                )
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .expiredUrl("/api/auth/login?expired=true")
                );

        http.authenticationProvider(authenticationProvider);

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler(
            CustomAuthenticationSuccessHandler handler) {
        return handler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(CustomUserDetailsService customUserDetailsService) {
        return customUserDetailsService;
    }

    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                         PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}