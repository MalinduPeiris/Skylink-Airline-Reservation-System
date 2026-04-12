package com.skylinkapplication.skylinkairlinereservationsystem.model;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Data
public class Passenger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String firstName;
    @Column(nullable = false)
    private String lastName;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private String phone;
    @Column(nullable = false)
    private Date dateOfBirth;
    @Column(nullable = false)
    private String country;
    private String passportNumber;
    private Date passportExpiry;
    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;
}