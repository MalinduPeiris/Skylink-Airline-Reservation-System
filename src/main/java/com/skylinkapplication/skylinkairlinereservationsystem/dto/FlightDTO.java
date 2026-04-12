package com.skylinkapplication.skylinkairlinereservationsystem.dto;

import java.util.Date;

public class FlightDTO {
    private Long id;
    private String flightNumber;
    private Date departureDate;
    private Date arrivalDate;
    private String origin;
    private String destination;
    private Double price;
    private String cabinClass; // e.g., ECONOMY, BUSINESS
    private Integer seatsAvailable;

    // Constructors
    public FlightDTO() {}
    public FlightDTO(Long id, String flightNumber, Date departureDate, Date arrivalDate, String origin,
                     String destination, Double price, String cabinClass, Integer seatsAvailable) {
        this.id = id;
        this.flightNumber = flightNumber;
        this.departureDate = departureDate;
        this.arrivalDate = arrivalDate;
        this.origin = origin;
        this.destination = destination;
        this.price = price;
        this.cabinClass = cabinClass;
        this.seatsAvailable = seatsAvailable;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }
    public Date getDepartureDate() { return departureDate; }
    public void setDepartureDate(Date departureDate) { this.departureDate = departureDate; }
    public Date getArrivalDate() { return arrivalDate; }
    public void setArrivalDate(Date arrivalDate) { this.arrivalDate = arrivalDate; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getCabinClass() { return cabinClass; }
    public void setCabinClass(String cabinClass) { this.cabinClass = cabinClass; }
    public Integer getSeatsAvailable() { return seatsAvailable; }
    public void setSeatsAvailable(Integer seatsAvailable) { this.seatsAvailable = seatsAvailable; }
}