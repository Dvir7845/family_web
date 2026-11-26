package com.familyhome;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.familyhome.member.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
        properties = {
            "spring.flyway.locations=classpath:db/migration,classpath:db/postgresql",
            "spring.jpa.hibernate.ddl-auto=validate",
            "app.scheduling.enabled=false",
            "APP_LOGIN_PASSWORD=TestBootstrap123!"
        })
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PostgresIntegrationTest {

    @Container @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private AppUserRepository users;

    @Test
    void runsAllFlywayMigrationsAndHappyPathApiFlowOnPostgres() throws Exception {
        long adminId = users.findByUsername("admin").orElseThrow().getId();

        mockMvc.perform(
                        post("/tasks")
                                .with(httpBasic("admin", "TestBootstrap123!"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Trash",
                                          "description": "Take out bins",
                                          "assigneeId": %d,
                                          "type": "WEEKLY",
                                          "days": ["SUNDAY", "WEDNESDAY"]
                                        }
                                        """
                                                .formatted(adminId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(
                        post("/events")
                                .with(httpBasic("admin", "TestBootstrap123!"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "description": "Family dinner",
                                          "eventDate": "2027-01-15",
                                          "invitedUserIds": [%d]
                                        }
                                        """
                                                .formatted(adminId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/events").with(httpBasic("admin", "TestBootstrap123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].description").value("Family dinner"));

        mockMvc.perform(
                        post("/shopping-items")
                                .with(httpBasic("admin", "TestBootstrap123!"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "productName": "Milk",
                                          "quantity": 2
                                        }
                                        """))
                .andExpect(status().isCreated());
    }
}
