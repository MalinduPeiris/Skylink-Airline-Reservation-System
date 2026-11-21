package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.config.SecurityConfig;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.PaymentDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.UserRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.service.BookingService;
import com.skylinkapplication.skylinkairlinereservationsystem.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.mockito.Mockito.when;

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class TravelerPaymentWebSecurityTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private BookingService bookingService;
    @MockitoBean private PaymentService paymentService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private AuthenticationProvider authenticationProvider;
    @MockitoBean private AuthenticationSuccessHandler authenticationSuccessHandler;

    @Test
    void anonymousTravelerIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/api/payment/page").param("bookingId", "30"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "traveler", roles = "TRAVELER")
    void travelerPostRequiresCsrfToken() throws Exception {
        mvc.perform(post("/api/payment/process").param("bookingId", "30"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "traveler", roles = "TRAVELER")
    void travelerCannotOpenFinanceManagement() throws Exception {
        mvc.perform(get("/api/payment/manage"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "finance", roles = "FINANCE_EXECUTIVE")
    void financeRoleCanOpenManagement() throws Exception {
        mvc.perform(get("/api/payment/manage"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "finance", roles = "FINANCE_EXECUTIVE")
    void financeManagementRendersExistingPaymentDate() throws Exception {
        PaymentDTO payment = new PaymentDTO(7L, 12L, 60045.0, "PENDING", "EZ_CASH",
                "TXN-TEST", new Date(1763779200000L));
        when(paymentService.getAllPayments()).thenReturn(List.of(payment));

        mvc.perform(get("/api/payment/manage"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("TXN-TEST")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Payment #")));
    }
}
