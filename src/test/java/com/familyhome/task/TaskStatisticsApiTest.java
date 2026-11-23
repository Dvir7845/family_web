package com.familyhome.task;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.familyhome.member.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class TaskStatisticsApiTest {
    @Autowired MockMvc mvc;
    @Autowired TaskRepository tasks;
    @Autowired AppUserRepository users;

    @Test
    void statisticsRequireLogin() throws Exception {
        mvc.perform(get("/tasks/statistics/weekly").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void countsOnlyCurrentWeekAndDoesNotMoveCreditOnReassignment() throws Exception {
        AppUser alice = users.saveAndFlush(new AppUser("alice", "unused")),
                bob = users.saveAndFlush(new AppUser("bob", "unused"));
        ZoneId zone = ZoneId.of("Asia/Jerusalem");
        LocalDate start =
                LocalDate.now(zone).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        Instant boundary = start.atStartOfDay(zone).toInstant();
        for (Instant instant :
                new Instant[] {
                    boundary,
                    boundary.minusNanos(1000000000),
                    start.plusWeeks(1).atStartOfDay(zone).toInstant()
                }) {
            OneTimeTask task =
                    new OneTimeTask("Count boundaries", "", null, LocalDate.of(2026, 11, 8));
            task.assignOwner(bob);
            task.markAsCompleted("alice");
            ReflectionTestUtils.setField(task, "completedAt", instant);
            tasks.saveAndFlush(task);
        }
        mvc.perform(get("/tasks/statistics/weekly").with(user("bob")))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.members[?(@.username == 'alice')].completedCount")
                                .value(hasItem(1)))
                .andExpect(
                        jsonPath("$.members[?(@.username == 'bob')].completedCount")
                                .value(hasItem(0)));
    }

    @Test
    void completingTwiceCountsOnceAndPersistsPerformer() throws Exception {
        AppUser alice = users.saveAndFlush(new AppUser("alice", "unused"));
        OneTimeTask task = new OneTimeTask("Complete", "", null, LocalDate.of(2026, 11, 8));
        task.assignOwner(alice);
        task = tasks.saveAndFlush(task);
        for (int i = 0; i < 2; i++)
            mvc.perform(
                            patch("/tasks/" + task.getId() + "/complete")
                                    .with(user("alice"))
                                    .with(csrf()))
                    .andExpect(status().isOk());
        tasks.flush();
        assertNotNull(tasks.findById(task.getId()).orElseThrow().getCompletedAt());
        mvc.perform(get("/tasks/statistics/weekly").with(user("alice")))
                .andExpect(
                        jsonPath("$.members[?(@.username == 'alice')].completedCount")
                                .value(hasItem(1)));
    }
}
