package com.familyhome.vehicle;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.familyhome.member.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "driver")
class VehicleApiTest {
    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired VehicleRepository vehicles;
    @Autowired VehicleBookingRepository bookings;
    Long vehicleId;

    @BeforeEach
    void setup() {
        AppUser driver = new AppUser("driver", "unused");
        driver.changeDriver(true);
        users.saveAndFlush(driver);
        users.saveAndFlush(new AppUser("passenger", "unused"));
        AppUser other = new AppUser("other", "unused");
        other.changeDriver(true);
        users.saveAndFlush(other);
        vehicleId = vehicles.saveAndFlush(new Vehicle("Family car")).getId();
    }

    String body() {
        return "{\"vehicleId\":" + vehicleId + ",\"date\":\"2099-12-10\",\"slot\":\"MORNING\"}";
    }

    @Test
    void accessRequiresDriverEvenForAdmin() throws Exception {
        for (String role : new String[] {"MEMBER", "ADMIN"}) {
            mvc.perform(get("/vehicles").with(user("passenger").roles(role)))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/vehicles/bookings?month=2099-12").with(user("passenger").roles(role)))
                    .andExpect(status().isForbidden());
            mvc.perform(
                            post("/vehicles/bookings")
                                    .with(user("passenger").roles(role))
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body()))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/vehicles").with(anonymous()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void onlyAdminDriverCanAddVehicles() throws Exception {
        mvc.perform(
                        post("/vehicles")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"New car\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/vehicles")
                                .with(user("driver").roles("ADMIN"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"New car\"}"))
                .andExpect(status().isCreated());
        mvc.perform(
                        post("/vehicles")
                                .with(user("driver").roles("ADMIN"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bookingIsAssignedToCallerAndCanBeCancelled() throws Exception {
        mvc.perform(
                        post("/vehicles/bookings")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("driver"));
        mvc.perform(get("/vehicles/bookings?month=2099-12"))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/vehicles/bookings?month=2099-11"))
                .andExpect(jsonPath("$.length()").value(0));
        Long id = bookings.findAll().getFirst().getId();
        mvc.perform(delete("/vehicles/bookings/" + id).with(user("other")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/vehicles/bookings/" + id).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminCanCancelAnotherDriversBooking() throws Exception {
        users.findByUsername("other").orElseThrow().changeRole(Role.ADMIN);
        mvc.perform(
                        post("/vehicles/bookings")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body()))
                .andExpect(status().isCreated());
        mvc.perform(
                        delete("/vehicles/bookings/" + bookings.findAll().getFirst().getId())
                                .with(user("other").roles("ADMIN"))
                                .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void invalidInputAndMissingCsrfAreRejected() throws Exception {
        mvc.perform(
                        post("/vehicles/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body()))
                .andExpect(status().isForbidden());
        for (String invalid :
                new String[] {
                    "{}",
                    body().replace("2099-12-10", "2000-01-01"),
                    body().replace("MORNING", "NIGHT"),
                    body().replace("\"vehicleId\":" + vehicleId, "\"vehicleId\":999999")
                }) {
            mvc.perform(
                            post("/vehicles/bookings")
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(invalid))
                    .andExpect(status().is4xxClientError());
        }
        mvc.perform(get("/vehicles/bookings?month=invalid")).andExpect(status().isBadRequest());
    }

    @Test
    void duplicateSlotReturnsConflict() throws Exception {
        mvc.perform(
                        post("/vehicles/bookings")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body()))
                .andExpect(status().isCreated());
        mvc.perform(
                        post("/vehicles/bookings")
                                .with(user("other"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body()))
                .andExpect(status().isConflict());
    }

    @Test
    void eachSlotAndVehicleCanBeBookedIndependently() throws Exception {
        for (String slot : new String[] {"MORNING", "AFTERNOON", "EVENING"})
            mvc.perform(
                            post("/vehicles/bookings")
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body().replace("MORNING", slot)))
                    .andExpect(status().isCreated());
        Long second = vehicles.saveAndFlush(new Vehicle("Second car")).getId();
        mvc.perform(
                        post("/vehicles/bookings")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        body().replace(
                                                        "\"vehicleId\":" + vehicleId,
                                                        "\"vehicleId\":" + second)))
                .andExpect(status().isCreated());
    }

    @Test
    void onlyAdminDriverCanDeleteVehicle() throws Exception {
        mvc.perform(delete("/vehicles/" + vehicleId).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(
                        delete("/vehicles/" + vehicleId)
                                .with(user("passenger").roles("ADMIN"))
                                .with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/vehicles/" + vehicleId).with(user("driver").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(
                        delete("/vehicles/" + vehicleId)
                                .with(anonymous())
                                .with(csrf())
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
        assertTrue(vehicles.existsById(vehicleId));
    }

    @Test
    void deleteRemovesAllVehicleBookingsAndPreservesOtherVehicles() throws Exception {
        mvc.perform(
                        post("/vehicles/bookings")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body()))
                .andExpect(status().isCreated());
        Long second = vehicles.saveAndFlush(new Vehicle("Second car")).getId();
        mvc.perform(
                        post("/vehicles/bookings")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        body().replace(
                                                        "\"vehicleId\":" + vehicleId,
                                                        "\"vehicleId\":" + second)))
                .andExpect(status().isCreated());
        mvc.perform(
                        delete("/vehicles/" + vehicleId)
                                .with(user("driver").roles("ADMIN"))
                                .with(csrf()))
                .andExpect(status().isNoContent());
        vehicles.flush();
        assertFalse(vehicles.existsById(vehicleId));
        assertTrue(vehicles.existsById(second));
        mvc.perform(get("/vehicles/bookings?month=2099-12"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].vehicleId").value(second.intValue()));
        mvc.perform(
                        delete("/vehicles/" + vehicleId)
                                .with(user("driver").roles("ADMIN"))
                                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void driverStatusIsManagedOnlyByAdmin() throws Exception {
        Long id = users.findByUsername("passenger").orElseThrow().getId();
        mvc.perform(
                        patch("/members/" + id + "/driver")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"driver\":true}"))
                .andExpect(status().isForbidden());
        mvc.perform(
                        patch("/members/" + id + "/driver")
                                .with(user("driver").roles("ADMIN"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"driver\":true}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/me").with(user("passenger"))).andExpect(jsonPath("$.driver").value(true));
    }
}
