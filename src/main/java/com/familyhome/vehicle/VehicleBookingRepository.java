package com.familyhome.vehicle;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleBookingRepository extends JpaRepository<VehicleBooking, Long> {
    void deleteByVehicleId(Long vehicleId);

    void deleteByUserId(Long userId);

    List<VehicleBooking> findByDateBetweenOrderByDateAsc(LocalDate start, LocalDate end);
}
