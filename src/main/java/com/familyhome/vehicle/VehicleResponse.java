package com.familyhome.vehicle;

public record VehicleResponse(Long id, String name) {

    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(vehicle.getId(), vehicle.getName());
    }
}
