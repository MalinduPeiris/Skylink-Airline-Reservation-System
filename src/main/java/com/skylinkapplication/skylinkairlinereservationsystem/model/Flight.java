package com.skylinkapplication.skylinkairlinereservationsystem.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Entity
@Data
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String flightNumber;

    @Column(name = "departure_date", nullable = false)
    private Date departureDate;

    @Column(name = "arrival_date", nullable = false)
    private Date arrivalDate;

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    @Column(nullable = false)
    private Double price;

    @Enumerated(EnumType.STRING)
    @Column(name = "cabin_class", nullable = false)
    private CabinClass cabinClass;

    @Column(name = "seats_available", nullable = false)
    private Integer seatsAvailable;

    @Column(name = "aircraft_type", nullable = false)
    private String aircraftType;

    @Column(nullable = false)
    private String status;

    @OneToMany(mappedBy = "flight", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Seat> seats;

    public enum CabinClass {
        ECONOMY,
        BUSINESS,
        FIRST_CLASS
    }

}