package com.familyhome.vehicle;

import com.familyhome.member.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

@Entity
@Table(
        name = "vehicle_bookings",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "unique_vehicle_slot",
                        columnNames = {"vehicle_id", "booking_date", "slot"}))
public class VehicleBooking {
    public enum Slot {
        MORNING,
        AFTERNOON,
        EVENING
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "booking_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Slot slot;

    protected VehicleBooking() {}

    public VehicleBooking(Vehicle vehicle, AppUser user, LocalDate date, Slot slot) {
        this.vehicle = vehicle;
        this.user = user;
        this.date = date;
        this.slot = slot;
    }

    public Long getId() {
        return id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public AppUser getUser() {
        return user;
    }

    public LocalDate getDate() {
        return date;
    }

    public Slot getSlot() {
        return slot;
    }
}
