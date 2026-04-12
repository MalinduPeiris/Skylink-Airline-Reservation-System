package com.skylinkapplication.skylinkairlinereservationsystem.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Data
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer rating;

    private String comments;

    @Column(nullable = false)
    private Date submittedDate;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}