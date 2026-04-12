package com.skylinkapplication.skylinkairlinereservationsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/password-reset",
                                "/api/admin/dashboard",
                                "/dashboard/admin",
                                "/api/flight/search",
                                "/api/flight/all",

                                "/api/support/update/**",
                                "/api/feedback/update/**",

                                "/api/feedback/delete/**",
                                "/deals",
                                "/api/marketing/promotions",
                                "/api/marketing/promotion/**",
                                "/api/marketing/update/**",
                                "/api/marketing/delete/**",
                                "/api/support/tickets",
                                "/api/user/profile",
                                "/api/user/update",
                                "/api/user/delete",
                                "/api/user/support/tickets",
                                "/flights",
                                "/api/booking/page",
                                "/flight-admin/delete/",
                                "/api/support/init-sample-data",
                                "/dashboard/reservation-manager",
                                "/dashboard/finance",
                                "/dashboard/flight-admin",
                                "/dashboard/flight-admin/delete/**",
                                "/dashboard/flight-admin/update/**",
                                "/api/user/support/submit-ticket",
                                "/dashboard/marketing-manager",
                                "/dashboard/customer-support",
                                "/api/support/delete/**",
                                "/api/booking/update/**",
                                "/api/marketing/create",
                                "/api/payment/update/**",
                                "/api/payment/delete/**",
                                "/api/auth/logout",
                                "/dashboard",
                                "/api/booking/delete/**",
                                "/api/booking/cancel",
                                "/logout",
                                "/api/user/support/submit-feedback",
                                "/api/booking/traveler-payment",
                                "/dashboard/finance/cancelled-bookings",
                                "/api/feedback/edit/**",
                                "/api/support/respond",
                                "/api/support/edit/**",
                                "/dashboard/finance/completed",
                                "/dashboard/finance/all-payments",
                                "/dashboard/finance/failed",
                                "/dashboard/finance/refunded",
                                "/dashboard/finance/payment/update/**",
                                "/dashboard/finance/payment/delete/**",
                                "/dashboard/finance/cancelled-bookings",
                                "/dashboard/finance/booking/update-payment-status/**",
                                "/booking",
                                "/booking-confirmation",
                                "/traveler-payment",
                                "/css/**",
                                "/js/**",
                                "/static/**",
                                "/images/**",
                                "/favicon.ico",
                                "/api/booking/create-with-passengers"
                        ).permitAll()
                        .requestMatchers(
                                "/",
                                "/index",
                                "/index.html",
                                "/register.html"
                        ).permitAll()
                        // Auth endpoints
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/password-reset",
                                "/api/auth/logout"
                        ).permitAll()
                        // Public flight search and viewing
                        .requestMatchers(
                                "/api/flight/search",
                                "/api/flight/all",
                                "/flights"
                        ).permitAll()
                        // User support endpoints - should be accessible to authenticated users
                        .requestMatchers("/api/user/support/**").authenticated()
                        // Booking endpoints - require authentication
                        .requestMatchers("/api/booking/**").authenticated()
                        // User profile endpoints - require authentication
                        .requestMatchers("/api/user/**").authenticated()
                        // Traveler payment endpoints - require authentication (allow page/process/confirmation)
                        .requestMatchers("/api/payment/page", "/api/payment/process", "/api/payment/confirmation").authenticated()
                        // Marketing endpoints - require authentication
                        .requestMatchers("/api/marketing/**").authenticated()
                        // Dashboard endpoints - role-based (handled by @PreAuthorize in controllers)
                        .requestMatchers("/dashboard/**").authenticated()
                        // Role-based API access
                        .requestMatchers("/dashboard/flight-admin/**").hasRole("IT_SYSTEM_ENGINEER")
                        .requestMatchers("/dashboard/reservation-manager/**").hasAnyRole("RESERVATION_MANAGER", "IT_SYSTEM_ENGINEER")
                        .requestMatchers("/dashboard/marketing-manager/**").hasAnyRole("MARKETING_EXECUTIVE", "IT_SYSTEM_ENGINEER")
                        .requestMatchers("/dashboard/customer-support/**").hasAnyRole("CUSTOMER_SUPPORT_OFFICER", "IT_SYSTEM_ENGINEER")
                        .requestMatchers("/dashboard/admin/**").hasRole("IT_SYSTEM_ENGINEER")
                        .requestMatchers("/dashboard/user-management/**").hasRole("IT_SYSTEM_ENGINEER")
                        .requestMatchers("/dashboard/promotion-management/**").hasAnyRole("MARKETING_EXECUTIVE", "IT_SYSTEM_ENGINEER")
                        .requestMatchers(
                                "/dashboard/finance",
                                "/dashboard/finance/**"
                        ).hasAnyRole("FINANCE_EXECUTIVE", "IT_SYSTEM_ENGINEER")
                        .requestMatchers("/api/finance/**").hasAnyRole("FINANCE_EXECUTIVE", "IT_SYSTEM_ENGINEER")
                        .requestMatchers("/api/user/profile", "/api/user/update", "/api/user/delete").authenticated()
                        .requestMatchers("/dashboard/it-system-engineer/**").hasRole("IT_SYSTEM_ENGINEER")
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
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendRedirect("/api/auth/login");
                        })
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

    // Use the existing component `CustomAuthenticationSuccessHandler`
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler(CustomAuthenticationSuccessHandler handler) {
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
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}