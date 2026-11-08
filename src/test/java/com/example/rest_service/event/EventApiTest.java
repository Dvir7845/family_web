package com.example.rest_service.event;

import com.example.rest_service.member.AppUser;
import com.example.rest_service.member.AppUserRepository;
import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request
        .SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request
        .MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result
        .MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:event-api;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.auto_quote_keyword=true",
        "APP_LOGIN_PASSWORD=TestBootstrap123!"
})
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "event_member", roles = "MEMBER")
class EventApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private Long memberId;

    @BeforeEach
    void setUp() {
        AppUser member = userRepository.saveAndFlush(
                new AppUser("event_member", "unused-in-mock-auth")
        );
        memberId = member.getId();
    }

    @Test
    @WithAnonymousUser
    void anonymousUserCannotReadEvents() throws Exception {
        mockMvc.perform(get("/events")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void writeWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void membersCanCreateReadUpdateAndDeleteSharedEvent()
            throws Exception {

        long eventId = createEvent();

        flushAndClear();

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[*].description",
                        hasItem("Family dinner")
                ));

        // Another ordinary member can update the shared event.
        mockMvc.perform(put("/events/" + eventId)
                        .with(user("another_member").roles("MEMBER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Updated dinner",
                                  "eventDate": "2026-12-20",
                                  "invitedUserIds": [%d]
                                }
                                """.formatted(memberId)))
                .andExpect(status().isOk());

        flushAndClear();

        mockMvc.perform(get("/events/" + eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description")
                        .value("Updated dinner"))
                .andExpect(jsonPath("$.eventDate")
                        .value("2026-12-20"))
                .andExpect(jsonPath("$.invitedUserIds[0]")
                        .value(memberId.intValue()));

        mockMvc.perform(delete("/events/" + eventId)
                        .with(user("another_member").roles("MEMBER"))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful());

        flushAndClear();

        mockMvc.perform(get("/events/" + eventId))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"description\":\" \",\"eventDate\":\"2026-12-10\",\"invitedUserIds\":[]}",
        "{\"description\":\"Dinner\",\"eventDate\":null,\"invitedUserIds\":[]}",
        "{\"description\":\"Dinner\",\"eventDate\":\"2026-12-10\",\"invitedUserIds\":null}",
        "{\"description\":\"Dinner\",\"eventDate\":\"2026-12-10\",\"invitedUserIds\":[null]}",
        "{\"description\":\"Dinner\",\"eventDate\":\"2026-12-10\",\"invitedUserIds\":[-1]}",
        "{\"description\":\"Dinner\",\"eventDate\":\"2026-12-10\",\"invitedUserIds\":[9223372036854775807]}"
    })
    void invalidInputIsRejectedOnCreateAndUpdate(String body)
            throws Exception {

        long eventId = createEvent();

        mockMvc.perform(post("/events")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/events/" + eventId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        flushAndClear();

        // A rejected update must not change the original event.
        mockMvc.perform(get("/events/" + eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description")
                        .value("Family dinner"))
                .andExpect(jsonPath("$.eventDate")
                        .value("2026-12-10"))
                .andExpect(jsonPath("$.invitedUserIds").isEmpty());
    }

    @Test
    void missingEventReturns404() throws Exception {
        String path = "/events/" + Long.MAX_VALUE;

        mockMvc.perform(get(path))
                .andExpect(status().isNotFound());

        mockMvc.perform(put(path)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete(path).with(csrf()))
                .andExpect(status().isNotFound());

        mockMvc.perform(post(path + "/invite/" + memberId)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void existingUserCanBeInvited() throws Exception {
        long eventId = createEvent();

        mockMvc.perform(post(
                        "/events/" + eventId + "/invite/" + memberId)
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful());

        flushAndClear();

        mockMvc.perform(get("/events/" + eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitedUserIds[0]")
                        .value(memberId.intValue()));
    }

    @Test
    void nonexistentUserCannotBeInvited() throws Exception {
        long eventId = createEvent();

        mockMvc.perform(post(
                        "/events/" + eventId + "/invite/" + Long.MAX_VALUE)
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        flushAndClear();

        mockMvc.perform(get("/events/" + eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitedUserIds").isEmpty());
    }

    @Test
    void memberCanLoadNamesForInviteSelection() throws Exception {
        mockMvc.perform(get("/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[*].username",
                        hasItem("event_member")
                ))
                .andExpect(jsonPath("$[*].passwordHash").isEmpty())
                .andExpect(jsonPath("$[*].password").isEmpty());
    }

    private long createEvent() throws Exception {
        String response = mockMvc.perform(post("/events")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number id = JsonPath.read(response, "$.id");
        return id.longValue();
    }

    private String validBody() {
        return """
                {
                  "description": "Family dinner",
                  "eventDate": "2026-12-10",
                  "invitedUserIds": []
                }
                """;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}