package com.familyhome.vehicle;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import com.familyhome.exception.BusinessRuleException;

@RestController
@RequestMapping("/vehicles")
public class VehicleController {
    private final VehicleService service;

    public VehicleController(VehicleService service) {
        this.service = service;
    }

    public record VehicleRequest(@NotBlank @Size(max = 100) String name) {}

    public record BookingRequest(
            @NotNull @Positive Long vehicleId,
            @NotNull LocalDate date,
            @NotNull VehicleBooking.Slot slot) {}

    @GetMapping
    public List<Vehicle> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Vehicle create(@Valid @RequestBody VehicleRequest request) {
        return service.create(request.name());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/bookings")
    public List<VehicleService.BookingResponse> month(@RequestParam String month) {
        YearMonth parsed;
        try {
            parsed = YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new BusinessRuleException("Invalid month");
        }
        if (parsed.getYear() < 1 || parsed.getYear() > 9999)
            throw new BusinessRuleException("Invalid year");
        return service.month(parsed);
    }

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleService.BookingResponse book(@Valid @RequestBody BookingRequest request) {
        return service.book(request.vehicleId(), request.date(), request.slot());
    }

    @DeleteMapping("/bookings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id) {
        service.cancel(id);
    }
}
