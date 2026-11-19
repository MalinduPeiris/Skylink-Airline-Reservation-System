package com.skylinkapplication.skylinkairlinereservationsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Date;
import java.util.List;

@Entity
@Data
public class Promotion {
    public enum Scope { GENERAL, TARGETED }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double discount;

    @Column(nullable = false)
    private Date validityStart;

    @Column(nullable = false)
    private Date validityEnd;

    @Column(nullable = false)
    private String targetCriteria;

    @Column(unique = true, nullable = false)
    private String promoCode;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 16)
    private Scope scope;

    @Column
    private Boolean active;

    @ManyToMany
    @JoinTable(
            name = "promotion_users",
            joinColumns = @JoinColumn(name = "promotion_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> users;
}
