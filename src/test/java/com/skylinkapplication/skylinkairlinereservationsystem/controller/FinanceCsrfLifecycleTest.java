package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import jakarta.servlet.http.Cookie;
import com.skylinkapplication.skylinkairlinereservationsystem.config.SecurityConfig;
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

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@WebMvcTest(FinanceController.class)
@Import(SecurityConfig.class)
class FinanceCsrfLifecycleTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private PaymentService paymentService;
    @MockitoBean private BookingService bookingService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private AuthenticationProvider authenticationProvider;
    @MockitoBean private AuthenticationSuccessHandler authenticationSuccessHandler;

    @Test
    @WithMockUser(username = "finance", roles = "FINANCE_EXECUTIVE")
    void firstFinancePageIssuesCsrfCookieForItsPostForms() throws Exception {
        when(paymentService.getPaymentsByStatus("PENDING")).thenReturn(List.of());

        mvc.perform(get("/dashboard/finance/pending"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"));

        verifyNoInteractions(bookingService);
    }

    @Test
    @WithMockUser(username = "finance", roles = "FINANCE_EXECUTIVE")
    void financePostWithoutCsrfIsRejected() throws Exception {
        mvc.perform(post("/dashboard/finance/payment/accept/9223372036854775807"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(paymentService);
    }

    @Test
    @WithMockUser(username = "finance", roles = "FINANCE_EXECUTIVE")
    void firstAndRepeatFinancePostsAcceptTokenIssuedByFirstPage() throws Exception {
        when(paymentService.getPaymentsByStatus("PENDING")).thenReturn(List.of());
        Cookie csrfCookie = mvc.perform(get("/dashboard/finance/pending"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("XSRF-TOKEN");

        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(post("/dashboard/finance/payment/accept/9223372036854775807")
                            .cookie(csrfCookie)
                            .param("_csrf", csrfCookie.getValue()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/dashboard/finance/pending"));
        }
        verify(paymentService, times(2)).updatePayment(eq(Long.MAX_VALUE), any());
    }
}
