package com.familyhome.member;

import com.familyhome.task.*;
import com.familyhome.event.*;
import com.familyhome.vehicle.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:accounts;DB_CLOSE_DELAY=-1", "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.auto_quote_keyword=true", "APP_LOGIN_PASSWORD=TestBootstrap123!"})
@AutoConfigureMockMvc
class AccountApiTest {
    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired TaskRepository tasks;
    @Autowired EventRepository events;
    @Autowired VehicleRepository vehicles;
    @Autowired VehicleBookingRepository bookings;
    AppUser member;
    @BeforeEach void setup() {
        member = users.saveAndFlush(new AppUser("account_member", encoder.encode("OldPassword123!")));
    }
    @AfterEach void cleanup() {
        bookings.deleteAll(); tasks.deleteAll(); events.deleteAll(); vehicles.deleteAll();
        users.findByUsername("account_member").ifPresent(users::delete);
    }
    MockHttpSession login() throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").with(csrf()).param("username", "account_member").param("password", "OldPassword123!"))
            .andExpect(status().is3xxRedirection()).andReturn().getRequest().getSession(false);
    }
    String passwordBody(String current, String next) {
        return "{\"currentPassword\":\""+current+"\",\"newPassword\":\""+next+"\"}";
    }
    @Test void passwordChangeRequiresCurrentPasswordAndValidatesInput() throws Exception {
        for (String body : List.of(passwordBody("wrong", "NewPassword123!"), passwordBody("OldPassword123!", "short"),
                passwordBody("OldPassword123!", "א".repeat(40)), "{}")) {
            mvc.perform(patch("/me/password").with(user("account_member")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        assertTrue(encoder.matches("OldPassword123!", users.findById(member.getId()).orElseThrow().getPasswordHash()));
        mvc.perform(patch("/me/password").with(user("account_member")).contentType(MediaType.APPLICATION_JSON).content(passwordBody("OldPassword123!", "NewPassword123!"))).andExpect(status().isForbidden());
        mvc.perform(patch("/me/password").with(csrf()).accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON).content(passwordBody("OldPassword123!", "NewPassword123!"))).andExpect(status().isUnauthorized());
    }
    @Test void passwordChangeHashesPasswordAndExpiresAllSessions() throws Exception {
        MockHttpSession first = login(), second = login();
        mvc.perform(patch("/me/password").session(first).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(passwordBody("OldPassword123!", "NewPassword123!"))).andExpect(status().isNoContent());
        assertTrue(first.isInvalid());
        mvc.perform(get("/me").session(second).accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
        mvc.perform(get("/me").with(httpBasic("account_member", "OldPassword123!"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/me").with(httpBasic("account_member", "NewPassword123!"))).andExpect(status().isOk());
        assertTrue(encoder.matches("NewPassword123!", users.findById(member.getId()).orElseThrow().getPasswordHash()));
    }
    @Test void onlyAdminCanDeleteAndCannotDeleteSelf() throws Exception {
        mvc.perform(delete("/members/"+member.getId()).with(user("account_member")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(delete("/members/"+member.getId()).with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(delete("/members/"+member.getId()).with(user("account_member").roles("ADMIN")).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(delete("/members/999999").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isNotFound());
        assertTrue(users.existsById(member.getId()));
    }
    @Test void deletePreservesSharedDataAndExpiresDeletedUsersSession() throws Exception {
        MockHttpSession session = login();
        Task task = new Task("Keep task", "Sunday", ""); task.assignOwner(member); task = tasks.saveAndFlush(task);
        Event event = events.saveAndFlush(new Event("Keep event", LocalDate.of(2099,1,1), new ArrayList<>(List.of(member.getId()))));
        Vehicle vehicle = vehicles.saveAndFlush(new Vehicle("Car"));
        bookings.saveAndFlush(new VehicleBooking(vehicle, member, LocalDate.of(2099,1,1), VehicleBooking.Slot.MORNING));
        mvc.perform(delete("/members/"+member.getId()).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isNoContent());
        assertFalse(users.existsById(member.getId()));
        assertNull(tasks.findById(task.getId()).orElseThrow().getAssigneeUsername());
        assertTrue(events.findById(event.getId()).orElseThrow().getInvitedUserIds().isEmpty());
        assertEquals(0, bookings.count()); assertEquals(1, vehicles.count());
        mvc.perform(get("/tasks").session(session).accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
        mvc.perform(get("/me").with(httpBasic("account_member", "OldPassword123!"))).andExpect(status().isUnauthorized());
    }
    @Test void promotionRequiresAdminAndCsrf() throws Exception {
        String path = "/members/"+member.getId()+"/promote";
        mvc.perform(post(path).with(user("account_member")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post(path).with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(post(path).with(anonymous()).with(csrf()).accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
        assertEquals(Role.MEMBER, users.findById(member.getId()).orElseThrow().getRole());
        mvc.perform(post("/members/999999/promote").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isNotFound());
    }
    @Test void promotionExpiresOldSessionAndNewLoginGetsAdminRights() throws Exception {
        MockHttpSession session = login();
        String path = "/members/"+member.getId()+"/promote";
        mvc.perform(post(path).with(httpBasic("dvir", "TestBootstrap123!")).with(csrf())).andExpect(status().isNoContent());
        assertEquals(Role.ADMIN, users.findById(member.getId()).orElseThrow().getRole());
        assertFalse(users.findById(member.getId()).orElseThrow().isDriver());
        mvc.perform(get("/me").session(session).accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
        MockHttpSession promoted = login();
        mvc.perform(get("/me").session(promoted)).andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(patch("/members/"+member.getId()+"/driver").session(promoted).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"driver\":true}")).andExpect(status().isNoContent());
        mvc.perform(get("/members").session(promoted)).andExpect(jsonPath("$[?(@.username == 'account_member')].role").value(org.hamcrest.Matchers.hasItem("ADMIN")));
        mvc.perform(post(path).with(httpBasic("dvir", "TestBootstrap123!")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/me").session(promoted)).andExpect(status().isOk());
    }
    @Test void adminCanSetAndRevokeDriverForExistingMember() throws Exception {
        for (boolean driver : new boolean[]{true, false}) {
            mvc.perform(patch("/members/"+member.getId()+"/driver").with(user("admin").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"driver\":"+driver+"}")).andExpect(status().isNoContent());
            mvc.perform(get("/me").with(user("account_member"))).andExpect(jsonPath("$.driver").value(driver));
        }
        mvc.perform(patch("/members/"+member.getId()+"/driver").with(user("account_member")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"driver\":true}")).andExpect(status().isForbidden());
    }
}
