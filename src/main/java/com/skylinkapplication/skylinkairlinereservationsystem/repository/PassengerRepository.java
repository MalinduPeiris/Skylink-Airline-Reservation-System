package com.skylinkapplication.skylinkairlinereservationsystem.repository;

import com.skylinkapplication.skylinkairlinereservationsystem.model.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Long> {
}