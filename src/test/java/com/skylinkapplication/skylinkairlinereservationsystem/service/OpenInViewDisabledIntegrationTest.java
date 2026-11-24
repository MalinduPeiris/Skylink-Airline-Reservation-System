package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.BookingDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Booking;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Flight;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Passenger;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Role;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.BookingRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.FlightRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.PassengerRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:open_in_view_test;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
class OpenInViewDisabledIntegrationTest {

    @Autowired private BookingService bookingService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PassengerRepository passengerRepository;
    @Autowired private FlightRepository flightRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MockMvc mockMvc;
    @Autowired private PasswordEncoder passwordEncoder;

    private Long bookingId;
    private Long flightId;
    private Long userId;

    @AfterEach
    void removeIsolatedTestData() {
        passengerRepository.deleteAll();
        bookingRepository.deleteAll();
        flightRepository.deleteAll();
        userRepository.deleteAll();
    }

    @BeforeEach
    void seedBookingWithPassenger() {
        User traveler = new User();
        traveler.setUsername("osiv_traveler");
        traveler.setEmail("osiv@example.test");
        traveler.setPassword(passwordEncoder.encode("isolated-test-password"));
        traveler.setRole(Role.FREQUENT_TRAVELER);
        traveler = userRepository.save(traveler);
        userId = traveler.getId();

        Flight flight = new Flight();
        flight.setFlightNumber("OSIV-101");
        flight.setDepartureDate(new Date(System.currentTimeMillis() + 86_400_000L));
        flight.setArrivalDate(new Date(System.currentTimeMillis() + 90_000_000L));
        flight.setOrigin("Colombo");
        flight.setDestination("Singapore");
        flight.setPrice(12000.0);
        flight.setCabinClass(Flight.CabinClass.ECONOMY);
        flight.setSeatsAvailable(20);
        flight.setAircraftType("A320");
        flight.setStatus("AVAILABLE");
        flight = flightRepository.save(flight);
        flightId = flight.getId();

        Booking booking = new Booking();
        booking.setBookingDate(new Date());
        booking.setStatus(Booking.Status.PENDING);
        booking.setPassengers(1);
        booking.setUser(traveler);
        booking.setFlight(flight);
        booking.setTotalPrice(12000.0);
        booking = bookingRepository.save(booking);
        bookingId = booking.getId();

        Passenger passenger = new Passenger();
        passenger.setBooking(booking);
        passenger.setFirstName("Test");
        passenger.setLastName("Traveler");
        passenger.setEmail("osiv@example.test");
        passenger.setPhone("0700000000");
        passenger.setDateOfBirth(new Date(0));
        passenger.setCountry("Sri Lanka");
        passengerRepository.save(passenger);
    }

    @Test
    void bookingReadMethodsMaterializePassengersBeforeReturningDetachedDtos() {
        BookingDTO byId = bookingService.getBookingById(bookingId);
        assertPassenger(byId);
        assertPassenger(onlyBooking(bookingService.getAllBookings()));
        assertPassenger(onlyBooking(bookingService.getBookingsByStatus("PENDING")));
        assertPassenger(onlyBooking(bookingService.getBookingsByUserId(userId)));
    }

    @Test
    void travelerPagesRenderWithoutOpenInViewAndProtectedPagesStillRequireLogin() throws Exception {
        mockMvc.perform(get("/api/booking/page").param("flightId", flightId.toString())
                        .with(user("osiv_traveler").roles("FREQUENT_TRAVELER")))
                .andExpect(status().isOk()).andExpect(view().name("booking-page"));
        mockMvc.perform(get("/api/payment/page").param("bookingId", bookingId.toString())
                        .with(user("osiv_traveler").roles("FREQUENT_TRAVELER")))
                .andExpect(status().isOk()).andExpect(view().name("traveler-payment"));
        mockMvc.perform(get("/api/user/profile")
                        .with(user("osiv_traveler").roles("FREQUENT_TRAVELER")))
                .andExpect(status().isOk()).andExpect(view().name("user-dashboard"));
        mockMvc.perform(get("/api/payment/confirmation")
                        .with(user("osiv_traveler").roles("FREQUENT_TRAVELER")))
                .andExpect(status().isOk()).andExpect(view().name("booking-confirmation"));

        mockMvc.perform(get("/api/booking/page").param("flightId", flightId.toString()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/api/auth/login"));
        mockMvc.perform(get("/api/payment/page").param("bookingId", bookingId.toString()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/api/auth/login"));

        mockMvc.perform(get("/dashboard/finance")
                        .with(user("finance_staff").roles("FINANCE_EXECUTIVE")))
                .andExpect(status().isOk()).andExpect(view().name("finance-dashboard"));
        mockMvc.perform(get("/dashboard/admin")
                        .with(user("system_staff").roles("IT_SYSTEM_ENGINEER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/marketing/promotions")
                        .with(user("marketing_staff").roles("MARKETING_EXECUTIVE")))
                .andExpect(status().isOk()).andExpect(view().name("promotion-manager-dashboard"));
        mockMvc.perform(get("/dashboard/finance")
                        .with(user("osiv_traveler").roles("FREQUENT_TRAVELER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void customDaoProviderKeepsBcryptLoginAndCsrfProtection() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .param("username", "osiv_traveler")
                        .param("password", "isolated-test-password"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", "osiv_traveler")
                        .param("password", "wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/api/auth/login?error=true"))
                .andExpect(unauthenticated());
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", "osiv_traveler")
                        .param("password", "isolated-test-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("osiv_traveler")
                        .withRoles("FREQUENT_TRAVELER"));
    }

    private BookingDTO onlyBooking(List<BookingDTO> bookings) {
        assertThat(bookings).hasSize(1);
        return bookings.get(0);
    }

    private void assertPassenger(BookingDTO booking) {
        assertThat(booking.getPassengerList()).hasSize(1);
        assertThat(booking.getPassengerList().get(0).getFirstName()).isEqualTo("Test");
        assertThat(booking.getFlight().getFlightNumber()).isEqualTo("OSIV-101");
    }
}
