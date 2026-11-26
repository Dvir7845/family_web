package com.familyhome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.familyhome.event.EventRepository;
import com.familyhome.member.AppUser;
import com.familyhome.member.AppUserRepository;
import com.familyhome.member.Role;
import com.familyhome.shopping.ShoppingItemRepository;
import com.familyhome.task.TaskRepository;
import com.familyhome.vehicle.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "demo"})
class DemoModeTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AppUserRepository users;
    @Autowired private TaskRepository tasks;
    @Autowired private ShoppingItemRepository shopping;
    @Autowired private EventRepository events;
    @Autowired private VehicleRepository vehicles;

    @Test
    void seedsNonDestructiveDemoUserAndSampleData() {
        AppUser demo = users.findByUsername("demo").orElseThrow();
        assertEquals(Role.MEMBER, demo.getRole());
        assertTrue(demo.isDriver());
        assertTrue(demo.isTaskManager());

        assertTrue(tasks.count() >= 4);
        assertTrue(shopping.count() >= 3);
        assertTrue(events.count() >= 1);
        assertTrue(vehicles.count() >= 2);
    }

    @Test
    void blocksChangingDemoAccountPassword() throws Exception {
        mockMvc.perform(
                        patch("/me/password")
                                .with(httpBasic("demo", "DemoPass123!"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "currentPassword": "DemoPass123!",
                                          "newPassword": "AnotherPassword123!"
                                        }
                                        """))
                .andExpect(status().isForbidden());
    }
}
