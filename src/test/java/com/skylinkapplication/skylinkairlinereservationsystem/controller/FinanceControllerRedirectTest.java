package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.PaymentDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.service.BookingService;
import com.skylinkapplication.skylinkairlinereservationsystem.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FinanceControllerRedirectTest {
    @Mock private PaymentService paymentService;
    @Mock private BookingService bookingService;
    @InjectMocks private FinanceController controller;

    @Test
    void approvingFromAllPaymentsReturnsToExistingRouteAndRequestsCompletion() {
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
        String redirect = controller.acceptPayment(8L, "all", flash);

        ArgumentCaptor<PaymentDTO> payment = ArgumentCaptor.forClass(PaymentDTO.class);
        verify(paymentService).updatePayment(org.mockito.ArgumentMatchers.eq(8L), payment.capture());
        assertEquals("COMPLETED", payment.getValue().getStatus());
        assertEquals("redirect:/dashboard/finance/all-payments", redirect);
        assertTrue(flash.getFlashAttributes().containsKey("success"));
    }

    @Test
    void rejectingFromPendingPreservesItsRouteAndRequestsFailure() {
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
        String redirect = controller.cancelPayment(9L, "pending", flash);

        ArgumentCaptor<PaymentDTO> payment = ArgumentCaptor.forClass(PaymentDTO.class);
        verify(paymentService).updatePayment(org.mockito.ArgumentMatchers.eq(9L), payment.capture());
        assertEquals("FAILED", payment.getValue().getStatus());
        assertEquals("redirect:/dashboard/finance/pending", redirect);
    }

    @Test
    void failedApprovalFromAllPaymentsReturnsToExistingRoute() {
        doThrow(new IllegalStateException("No pending payment"))
                .when(paymentService).updatePayment(org.mockito.ArgumentMatchers.eq(8L),
                        org.mockito.ArgumentMatchers.any(PaymentDTO.class));
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();

        assertEquals("redirect:/dashboard/finance/all-payments",
                controller.acceptPayment(8L, "all", flash));
        assertTrue(flash.getFlashAttributes().containsKey("error"));
    }

    @Test
    void cancelledBookingsTabUsesItsExistingRoute() {
        assertEquals("redirect:/dashboard/finance/cancelled-bookings",
                controller.cancelPayment(9L, "cancelled", new RedirectAttributesModelMap()));
    }
}
