package com.skylinkapplication.skylinkairlinereservationsystem.dto;

import java.util.Date;

public class BookingDTO {
    private Long id;
    private Date bookingDate;
    private String status; // e.g., PENDING, CONFIRMED, CANCELLED
    private Integer passengers;
    private Long flightId;
    private Long paymentId;

    // Constructors
    public BookingDTO() {}
    public BookingDTO(Long id, Date bookingDate, String status, Integer passengers, Long flightId, Long paymentId) {
        this.id = id;
        this.bookingDate = bookingDate;
        this.status = status;
        this.passengers = passengers;
        this.flightId = flightId;
        this.paymentId = paymentId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Date getBookingDate() { return bookingDate; }
    public void setBookingDate(Date bookingDate) { this.bookingDate = bookingDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPassengers() { return passengers; }
    public void setPassengers(Integer passengers) { this.passengers = passengers; }
    public Long getFlightId() { return flightId; }
    public void setFlightId(Long flightId) { this.flightId = flightId; }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
}

