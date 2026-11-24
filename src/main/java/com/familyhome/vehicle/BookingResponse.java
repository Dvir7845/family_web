package com.familyhome.vehicle;

import java.time.LocalDate;

public record BookingResponse(
            Long id,
            Long vehicleId,
            Long userId,
            String username,
            LocalDate date,
            VehicleBooking.Slot slot) {
                
    public static BookingResponse from(VehicleBooking b) {
        return new BookingResponse(
                b.getId(),
                b.getVehicle().getId(),
                b.getUser().getId(),
                b.getUser().getUsername(),
                b.getDate(),
                b.getSlot());
    }
}