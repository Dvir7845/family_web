package com.example.rest_service.vehicle;
import com.example.rest_service.member.*;
import java.time.*;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
@Transactional
public class VehicleService {
    private final VehicleRepository vehicles;
    private final VehicleBookingRepository bookings;
    private final AppUserRepository users;
    public VehicleService(VehicleRepository vehicles, VehicleBookingRepository bookings, AppUserRepository users) {
        this.vehicles = vehicles; this.bookings = bookings; this.users = users;
    }
    private AppUser driver() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = users.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (!user.isDriver()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return user;
    }
    @Transactional(readOnly = true)
    public List<Vehicle> list() { driver(); return vehicles.findAll(); }
    @PreAuthorize("hasRole('ADMIN')")
    public Vehicle create(String name) { driver(); return vehicles.save(new Vehicle(name.trim())); }
    public record BookingResponse(Long id, Long vehicleId, Long userId, String username, LocalDate date, VehicleBooking.Slot slot) {}
    private BookingResponse response(VehicleBooking b) {
        return new BookingResponse(b.getId(), b.getVehicle().getId(), b.getUser().getId(), b.getUser().getUsername(), b.getDate(), b.getSlot());
    }
    @Transactional(readOnly = true)
    public List<BookingResponse> month(YearMonth month) {
        driver();
        return bookings.findByDateBetweenOrderByDateAsc(month.atDay(1), month.atEndOfMonth()).stream().map(this::response).toList();
    }
    public BookingResponse book(Long vehicleId, LocalDate date, VehicleBooking.Slot slot) {
        AppUser user = driver();
        if (date.isBefore(LocalDate.now(ZoneId.of("Asia/Jerusalem"))))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot book past dates");
        Vehicle vehicle = vehicles.findById(vehicleId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try { return response(bookings.saveAndFlush(new VehicleBooking(vehicle, user, date, slot))); }
        catch (DataIntegrityViolationException ex) { throw new ResponseStatusException(HttpStatus.CONFLICT, "Vehicle slot already booked"); }
    }
    public void cancel(Long id) {
        AppUser user = driver();
        VehicleBooking booking = bookings.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!booking.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        bookings.delete(booking);
    }
}
