package com.skylinkapplication.skylinkairlinereservationsystem.repository;

import com.skylinkapplication.skylinkairlinereservationsystem.model.Booking;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Flight;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Payment;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PaymentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void findAll_shouldReturnAllPayments() {
        // Create a user
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password");
        entityManager.persistAndFlush(user);

        // Create a flight
        Flight flight = new Flight();
        flight.setFlightNumber("SK001");
        flight.setOrigin("CMB");
        flight.setDestination("SIN");
        flight.setDepartureDate(new Date());
        flight.setArrivalDate(new Date());
        flight.setPrice(1000.0);
        flight.setCabinClass(Flight.CabinClass.ECONOMY);
        flight.setSeatsAvailable(100);
        flight.setAircraftType("Boeing 737");
        flight.setStatus("SCHEDULED");
        entityManager.persistAndFlush(flight);

        // Create a booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setFlight(flight);
        booking.setPassengers(2);
        booking.setBookingDate(new Date());
        booking.setTotalPrice(2000.0);
        entityManager.persistAndFlush(booking);

        // Create payments
        Payment payment1 = new Payment();
        payment1.setBooking(booking);
        payment1.setAmount(1000.0);
        payment1.setPaymentDate(new Date());
        payment1.setStatus(Payment.Status.COMPLETED);
        payment1.setPaymentMethod(Payment.PaymentMethod.CARD);
        payment1.setTransactionId("TXN-001");
        entityManager.persistAndFlush(payment1);

        Payment payment2 = new Payment();
        payment2.setBooking(booking);
        payment2.setAmount(500.0);
        payment2.setPaymentDate(new Date());
        payment2.setStatus(Payment.Status.FAILED);
        payment2.setPaymentMethod(Payment.PaymentMethod.EZ_CASH);
        payment2.setTransactionId("TXN-002");
        entityManager.persistAndFlush(payment2);

        // Test findAll
        List<Payment> payments = paymentRepository.findAll();
        
        assertThat(payments).hasSize(2);
        assertThat(payments).extracting(Payment::getAmount).containsExactlyInAnyOrder(1000.0, 500.0);
        assertThat(payments).extracting(Payment::getStatus).containsExactlyInAnyOrder(
            Payment.Status.COMPLETED, Payment.Status.FAILED);
    }
}