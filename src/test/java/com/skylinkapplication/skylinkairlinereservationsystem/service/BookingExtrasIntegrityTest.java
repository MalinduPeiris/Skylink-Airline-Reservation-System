package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.BookingRequestDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.PassengerDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Booking;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Flight;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.BookingRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.FlightRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.PassengerRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingExtrasIntegrityTest {
    @Mock private BookingRepository bookingRepository;
    @Mock private FlightRepository flightRepository;
    @Mock private PassengerRepository passengerRepository;
    @Mock private UserRepository userRepository;
    @Mock private PromotionService promotionService;
    @InjectMocks private BookingService service;

    @Test
    void calculatesApprovedExtrasPerPassengerDespiteTamperedHiddenAmount() {
        BookingRequestDTO request = request(2);
        request.setBaggage("2000");
        request.setSeatPref("2500");
        request.setMeal("1500");
        request.setAssistance("3000");
        request.setBookingExtras(1.0);
        request.setTotalPrice(1.0);
        Flight flight = stubUserAndFlight();
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(30L);
            return booking;
        });

        var result = service.createBookingWithPassengers(request);

        assertEquals(18000.0, result.getBookingExtras());
        assertEquals(18300.0, result.getTotalPrice());
        assertEquals(8, flight.getSeatsAvailable());
    }

    @Test
    void rejectsUnlistedExtraPriceBeforePersistingBooking() {
        BookingRequestDTO request = request(1);
        request.setBaggage("999999");
        Flight flight = stubUserAndFlight();
        lenient().when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(30L);
            return booking;
        });

        assertThrows(IllegalArgumentException.class,
                () -> service.createBookingWithPassengers(request));
        verify(bookingRepository, never()).save(any());
        verify(passengerRepository, never()).saveAll(any());
        assertEquals(10, flight.getSeatsAvailable());
    }

    private Flight stubUserAndFlight() {
        User user = new User();
        user.setId(10L);
        user.setEmail("traveler@example.com");
        Flight flight = new Flight();
        flight.setId(20L);
        flight.setPrice(150.0);
        flight.setSeatsAvailable(10);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(flightRepository.findById(20L)).thenReturn(Optional.of(flight));
        return flight;
    }

    private static BookingRequestDTO request(int count) {
        BookingRequestDTO request = new BookingRequestDTO();
        request.setUserId(10L);
        request.setFlightId(20L);
        request.setPassengerCount(count);
        PassengerDTO passenger = new PassengerDTO();
        passenger.setFirstName("Ava");
        passenger.setLastName("Perera");
        passenger.setEmail("traveler@example.com");
        passenger.setDateOfBirth(new Date(946684800000L));
        passenger.setCountry("Sri Lanka");
        request.setPassengers(count == 1 ? List.of(passenger) : List.of(passenger, passenger));
        return request;
    }
}
