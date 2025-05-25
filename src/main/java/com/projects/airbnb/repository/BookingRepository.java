package com.projects.airbnb.repository;

import com.projects.airbnb.entity.Booking;
import com.projects.airbnb.entity.Hotel;
import org.springframework.cglib.core.Local;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
   Optional<Booking> findByPaymentSessionId(String sessionId);

    List<Booking> findByHotel(Hotel existingHotel);

    List<Booking> findByHotelAndCreatedAtBetween(Hotel hotel, LocalDateTime startDateTime, LocalDateTime endDateTime);
}
