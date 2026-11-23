package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.BookingDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.BookingRequestDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.PassengerDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.PromotionDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Flight;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.FlightRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.UserRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.service.BookingService;
import com.skylinkapplication.skylinkairlinereservationsystem.service.PromotionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TravelerBookingControllerTest {

    @Mock private BookingService bookingService;
    @Mock private FlightRepository flightRepository;
    @Mock private UserRepository userRepository;
    @Mock private PromotionService promotionService;
    @InjectMocks private BookingController controller;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bookingPageKeepsSelectedFlightAndPassengerCountInRequestModel() {
        User user = user();
        Flight flight = new Flight();
        flight.setId(20L);
        flight.setFlightNumber("SK123");
        flight.setDestination("Colombo");
        flight.setSeatsAvailable(5);
        when(userRepository.findByUsername("traveler")).thenReturn(Optional.of(user));
        when(flightRepository.findById(20L)).thenReturn(Optional.of(flight));
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.showBookingPage(20L, 3, null, model,
                new UsernamePasswordAuthenticationToken("traveler", "unused"));

        assertEquals("booking-page", view);
        BookingRequestDTO request = (BookingRequestDTO) model.get("bookingRequest");
        assertNotNull(request);
        assertEquals(20L, request.getFlightId());
        assertEquals(10L, request.getUserId());
        assertEquals(3, request.getPassengerCount());
        assertEquals(3, request.getPassengers().size());
        assertEquals("traveler@example.com", request.getPassengers().get(0).getEmail());
        assertSame(flight, model.get("flight"));
    }

    @Test
    void promotionPreviewReturnsOnlyTheRequestedStoredDiscount() {
        PromotionDTO promotion = new PromotionDTO();
        promotion.setPromoCode("SKY20");
        promotion.setDiscount(20.0);
        when(userRepository.findByUsername("traveler")).thenReturn(Optional.of(user()));
        when(promotionService.findRedeemablePromotion(eq("sky20"), any(User.class))).thenReturn(Optional.of(promotion));

        var authentication = new UsernamePasswordAuthenticationToken("traveler", "unused");

        ResponseEntity<java.util.Map<String, Double>> matching = controller.previewPromotion("sky20", authentication);
        ResponseEntity<java.util.Map<String, Double>> missing = controller.previewPromotion("OTHER", authentication);

        assertEquals(200, matching.getStatusCode().value());
        assertEquals(20.0, matching.getBody().get("discount"));
        assertEquals(0.0, missing.getBody().get("discount"));
        assertEquals(1, matching.getBody().size());
    }

    @Test
    void bookingSubmissionOverridesClientUserAndRedirectsToPaymentPage() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("traveler", "unused"));
        when(userRepository.findByUsername("traveler")).thenReturn(Optional.of(user()));
        BookingRequestDTO request = new BookingRequestDTO();
        request.setUserId(999L);
        request.setFlightId(20L);
        request.setPassengerCount(1);
        PassengerDTO passenger = new PassengerDTO();
        passenger.setFirstName("Ava");
        passenger.setLastName("Perera");
        passenger.setEmail("traveler@example.com");
        passenger.setDateOfBirth(new Date(946684800000L));
        passenger.setCountry("Sri Lanka");
        request.setPassengers(List.of(passenger));
        BookingDTO created = new BookingDTO();
        created.setId(30L);
        when(bookingService.createBookingWithPassengers(any())).thenReturn(created);

        String redirect = controller.createBookingWithPassengers(request, new RedirectAttributesModelMap());

        assertEquals("redirect:/api/payment/page?bookingId=30", redirect);
        ArgumentCaptor<BookingRequestDTO> submitted = ArgumentCaptor.forClass(BookingRequestDTO.class);
        verify(bookingService).createBookingWithPassengers(submitted.capture());
        assertEquals(10L, submitted.getValue().getUserId());
        assertEquals(20L, submitted.getValue().getFlightId());
        assertEquals(1, submitted.getValue().getPassengers().size());
    }

    @Test
    void failedSubmissionKeepsPassengerAndExtraSelectionsForRedirect() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("traveler", "unused"));
        when(userRepository.findByUsername("traveler")).thenReturn(Optional.of(user()));
        BookingRequestDTO request = new BookingRequestDTO();
        request.setFlightId(20L);
        request.setPassengerCount(2);
        request.setBaggage("5000");
        PassengerDTO passenger = new PassengerDTO();
        passenger.setEmail("different@example.invalid");
        request.setPassengers(List.of(passenger, new PassengerDTO()));
        when(bookingService.createBookingWithPassengers(request))
                .thenThrow(new IllegalArgumentException("Primary passenger email must match logged-in user email"));
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();

        assertEquals("redirect:/api/booking/page?flightId=20&passengers=2",
                controller.createBookingWithPassengers(request, flash));
        assertSame(request, flash.getFlashAttributes().get("bookingRequest"));
        assertTrue(flash.getFlashAttributes().get("error").toString().contains("Primary passenger email"));
    }

    @Test
    void bookingPageRestoresFlashedPassengerValuesAndExtras() {
        User user = user();
        Flight flight = new Flight();
        flight.setId(20L);
        flight.setSeatsAvailable(5);
        when(userRepository.findByUsername("traveler")).thenReturn(Optional.of(user));
        when(flightRepository.findById(20L)).thenReturn(Optional.of(flight));
        BookingRequestDTO previous = new BookingRequestDTO();
        previous.setFlightId(20L);
        previous.setPassengerCount(1);
        previous.setBaggage("5000");
        PassengerDTO passenger = new PassengerDTO();
        passenger.setFirstName("Ava");
        passenger.setEmail("different@example.invalid");
        passenger.setDateOfBirth(new Date(946684800000L));
        previous.setPassengers(List.of(passenger));
        ExtendedModelMap model = new ExtendedModelMap();
        model.addAttribute("bookingRequest", previous);

        assertEquals("booking-page", controller.showBookingPage(20L, 1, null, model,
                new UsernamePasswordAuthenticationToken("traveler", "unused")));
        assertSame(previous, model.get("bookingRequest"));
        assertEquals("5000", ((BookingRequestDTO) model.get("bookingRequest")).getBaggage());
        @SuppressWarnings("unchecked")
        java.util.Map<String, String> values = (java.util.Map<String, String>) model.get("recoveryValues");
        assertEquals("Ava", values.get("passengers[0].firstName"));
        assertEquals("different@example.invalid", values.get("passengers[0].email"));
        assertEquals("2000-01-01", values.get("passengers[0].dateOfBirth"));
    }

    private static User user() {
        User user = new User();
        user.setId(10L);
        user.setEmail("traveler@example.com");
        return user;
    }
}
