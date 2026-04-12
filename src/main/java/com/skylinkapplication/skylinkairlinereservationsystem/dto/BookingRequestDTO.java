package com.skylinkapplication.skylinkairlinereservationsystem.dto;

import java.util.List;

public class BookingRequestDTO {
    private Long flightId;
    private Long userId;
    private Integer passengerCount;
    private Double bookingExtras;
    private String promoCode;
    private Double totalPrice;
    private List<PassengerDTO> passengers;

    // Default constructor
    public BookingRequestDTO() {
        this.bookingExtras = 0.0;
        this.promoCode = "";
        this.totalPrice = 0.0;
    }

    // Getters and Setters with validation
    public Long getFlightId() {
        return flightId;
    }

    public void setFlightId(Long flightId) {
        this.flightId = flightId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getPassengerCount() {
        return passengerCount;
    }

    public void setPassengerCount(Integer passengerCount) {
        this.passengerCount = passengerCount;
    }

    public Double getBookingExtras() {
        return bookingExtras != null ? bookingExtras : 0.0;
    }

    public void setBookingExtras(Double bookingExtras) {
        this.bookingExtras = bookingExtras != null ? bookingExtras : 0.0;
    }

    public String getPromoCode() {
        return promoCode != null ? promoCode : "";
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public Double getTotalPrice() {
        return totalPrice != null ? totalPrice : 0.0;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public List<PassengerDTO> getPassengers() {
        return passengers;
    }

    public void setPassengers(List<PassengerDTO> passengers) {
        this.passengers = passengers;
    }

    @Override
    public String toString() {
        return "BookingRequestDTO{" +
                "flightId=" + flightId +
                ", userId=" + userId +
                ", passengerCount=" + passengerCount +
                ", bookingExtras=" + bookingExtras +
                ", promoCode='" + promoCode + '\'' +
                ", totalPrice=" + totalPrice +
                ", passengers=" + (passengers != null ? passengers.size() + " passenger(s)" : "null") +
                '}';
    }
}