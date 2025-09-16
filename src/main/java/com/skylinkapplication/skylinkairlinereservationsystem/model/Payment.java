package com.skylinkapplication.skylinkairlinereservationsystem.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Data
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private Date transactionDate;

    @OneToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    public enum Status {
        COMPLETED, REFUNDED, FAILED
    }
}
