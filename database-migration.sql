-- Database Migration Script for Flight Booking System
-- This script adds the necessary columns and tables for the enhanced booking workflow

-- Step 1: Add new columns to bookings table
ALTER TABLE bookings 
ADD COLUMN total_price DOUBLE NOT NULL DEFAULT 0.0,
ADD COLUMN booking_extras DOUBLE DEFAULT 0.0;

-- Step 2: Create passengers table
CREATE TABLE IF NOT EXISTS passengers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    date_of_birth DATE NOT NULL,
    country VARCHAR(255) NOT NULL,
    passport_number VARCHAR(100),
    passport_expiry DATE,
    email VARCHAR(255) NOT NULL,
    booking_id BIGINT NOT NULL,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

-- Step 3: Create indexes for better performance
CREATE INDEX idx_passengers_booking_id ON passengers(booking_id);
CREATE INDEX idx_passengers_email ON passengers(email);
CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_flight_id ON bookings(flight_id);
CREATE INDEX idx_bookings_status ON bookings(status);

-- Note: If you're using JPA/Hibernate with auto-ddl, these tables will be created automatically.
-- This script is provided for manual database setup or migration purposes.