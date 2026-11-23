package com.familyhome.vehicle;

import com.familyhome.exception.BusinessRuleException;
import com.familyhome.exception.ConflictException;
import com.familyhome.exception.ForbiddenOperationException;
import com.familyhome.exception.ResourceNotFoundException;
import com.familyhome.member.*;
import java.time.*;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VehicleService {
    private final VehicleRepository vehicles;
    private final VehicleBookingRepository bookings;
    private final AppUserRepository users;
    private final Clock clock;

    public VehicleService(
            VehicleRepository vehicles,
            VehicleBookingRepository bookings,
            AppUserRepository users,
            Clock clock) {
        this.vehicles = vehicles;
        this.bookings = bookings;
        this.users = users;
        this.clock = clock;
    }

    private AppUser driver() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user =
                users.findByUsername(username)
                        .orElseThrow(() -> new ForbiddenOperationException("User not found"));
        if (!user.isDriver()) throw new ForbiddenOperationException("User is not a driver");
        return user;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@auth.isDriver(authentication)")
    public List<Vehicle> list() {
        return vehicles.findAll();
    }

    @PreAuthorize("hasRole('ADMIN') && @auth.isDriver(authentication)")
    public Vehicle create(String name) {
        return vehicles.save(new Vehicle(name.trim()));
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

    public record BookingResponse(
            Long id,
            Long vehicleId,
            Long userId,
            String username,
            LocalDate date,
            VehicleBooking.Slot slot) {}

    private BookingResponse response(VehicleBooking b) {
        return new BookingResponse(
                b.getId(),
                b.getVehicle().getId(),
                b.getUser().getId(),
                b.getUser().getUsername(),
                b.getDate(),
                b.getSlot());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@auth.isDriver(authentication)")
    public List<BookingResponse> month(YearMonth month) {
        return bookings
                .findByDateBetweenOrderByDateAsc(month.atDay(1), month.atEndOfMonth())
                .stream()
                .map(this::response)
                .toList();
    }

    @PreAuthorize("@auth.isDriver(authentication)")
    public BookingResponse book(Long vehicleId, LocalDate date, VehicleBooking.Slot slot) {
        AppUser user = driver();
        if (date.isBefore(LocalDate.now(clock.withZone(ZoneId.of("Asia/Jerusalem")))))
            throw new BusinessRuleException("Cannot book past dates");
        Vehicle vehicle =
                vehicles.findById(vehicleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        try {
            return response(bookings.saveAndFlush(new VehicleBooking(vehicle, user, date, slot)));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Vehicle slot already booked");
        }
    }
    
    @PreAuthorize("@auth.isDriver(authentication)")
    public void cancel(Long id) {
        AppUser user = driver();
        VehicleBooking booking =
                bookings.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (!booking.getUser().getId().equals(user.getId()) && !user.getRole().equals(Role.ADMIN))
            throw new ForbiddenOperationException("Cannot cancel booking");
        bookings.delete(booking);
    }
}
