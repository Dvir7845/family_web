package com.familyhome.vehicle;

import com.familyhome.exception.BusinessRuleException;
import com.familyhome.exception.ConflictException;
import com.familyhome.exception.ForbiddenOperationException;
import com.familyhome.exception.ResourceNotFoundException;
import com.familyhome.member.AppUser;
import com.familyhome.member.Role;
import com.familyhome.security.AuthService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VehicleService {
    private final VehicleRepository vehicles;
    private final VehicleBookingRepository bookings;
    private final Clock clock;
    private final AuthService authService;

    public VehicleService(
            VehicleRepository vehicles,
            VehicleBookingRepository bookings,
            Clock clock,
            AuthService authService) {
        this.vehicles = vehicles;
        this.bookings = bookings;
        this.clock = clock;
        this.authService = authService;
    }



    @Transactional(readOnly = true)
    @PreAuthorize("@auth.isDriver(authentication)")
    public List<VehicleResponse> list() {
        return vehicles.findAll().stream().map(VehicleResponse::from).toList();
    }

    @PreAuthorize("hasRole('ADMIN') && @auth.isDriver(authentication)")
    public VehicleResponse create(String name) {
        return VehicleResponse.from(vehicles.save(new Vehicle(name.trim())));
    }

    @PreAuthorize("hasRole('ADMIN') && @auth.isDriver(authentication)")
    public void delete(Long id) {
        Vehicle vehicle =
                vehicles.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        bookings.deleteByVehicleId(id);
        bookings.flush();
        vehicles.delete(vehicle);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@auth.isDriver(authentication)")
    public List<BookingResponse> month(YearMonth month) {
        return bookings
                .findByDateBetweenOrderByDateAsc(month.atDay(1), month.atEndOfMonth())
                .stream()
                .map(BookingResponse::from)
                .toList();
    }

    @PreAuthorize("@auth.isDriver(authentication)")
    public BookingResponse book(Long vehicleId, LocalDate date, VehicleBooking.Slot slot) {
        AppUser user = authService.requireCurrentUser();
        if (date.isBefore(LocalDate.now(clock.withZone(ZoneId.of("Asia/Jerusalem")))))
            throw new BusinessRuleException("Cannot book past dates");
        Vehicle vehicle =
                vehicles.findById(vehicleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        try {
            return BookingResponse.from(
                    bookings.saveAndFlush(new VehicleBooking(vehicle, user, date, slot)));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Vehicle slot already booked");
        }
    }

    @PreAuthorize("@auth.isDriver(authentication)")
    public void cancel(Long id) {
        AppUser user = authService.requireCurrentUser();
        VehicleBooking booking =
                bookings.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (!booking.getUser().getId().equals(user.getId()) && !user.getRole().equals(Role.ADMIN))
            throw new ForbiddenOperationException("Cannot cancel booking");
        bookings.delete(booking);
    }
}
