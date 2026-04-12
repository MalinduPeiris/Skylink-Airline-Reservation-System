package com.skylinkapplication.skylinkairlinereservationsystem.dto;

import com.skylinkapplication.skylinkairlinereservationsystem.model.Flight;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;

import java.util.Date;
import java.util.List;

public class BookingDTO {
    private Long id;
    private Date bookingDate;
    private String status; // e.g., PENDING, CONFIRMED, CANCELLED
    private Integer passengers;
    private Long userId;
    private Long flightId;
    private Long paymentId;
    private Double totalPrice;
    private Double bookingExtras;
    private String promoCode;
    private List<PassengerDTO> passengerList;
    private User user;
    private Flight flight;

    // NEW: Add payment object for enriched data
    private PaymentDTO payment;

    // Constructors
    public BookingDTO() {
    }

    public BookingDTO(Long id, Date bookingDate, String status, Integer passengers, Long userId, Long flightId, Long paymentId) {
        this.id = id;
        this.bookingDate = bookingDate;
        this.status = status;
        this.passengers = passengers;
        this.userId = userId;
        this.flightId = flightId;
        this.paymentId = paymentId;
    }

    private Long daysLeft;

    public Long getDaysLeft() {
        return daysLeft;
    }

    public void setDaysLeft(Long daysLeft) {
        this.daysLeft = daysLeft;
    }


    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(Date bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPassengers() {
        return passengers;
    }

    public void setPassengers(Integer passengers) {
        this.passengers = passengers;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getFlightId() {
        return flightId;
    }

    public void setFlightId(Long flightId) {
        this.flightId = flightId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Double getBookingExtras() {
        return bookingExtras;
    }

    public void setBookingExtras(Double bookingExtras) {
        this.bookingExtras = bookingExtras;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public List<PassengerDTO> getPassengerList() {
        return passengerList;
    }

    public void setPassengerList(List<PassengerDTO> passengerList) {
        this.passengerList = passengerList;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Flight getFlight() {
        return flight;
    }

    public void setFlight(Flight flight) {
        this.flight = flight;
    }

    // NEW: Payment getter and setter for enriched data
    public PaymentDTO getPayment() {
        return payment;
    }

    public void setPayment(PaymentDTO payment) {
        this.payment = payment;
    }
}