package com.familyhome.task;
import com.familyhome.member.*;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:weekly;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.properties.hibernate.auto_quote_keyword=true","APP_LOGIN_PASSWORD=TestBootstrap123!"})
@AutoConfigureMockMvc @Transactional
class TaskStatisticsApiTest {
 @Autowired MockMvc mvc; @Autowired TaskRepository tasks; @Autowired AppUserRepository users;
 @Test void statisticsRequireLogin() throws Exception {
  mvc.perform(get("/tasks/statistics/weekly").accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
 }
 @Test void countsOnlyCurrentWeekAndDoesNotMoveCreditOnReassignment() throws Exception {
  AppUser alice=users.saveAndFlush(new AppUser("alice","unused")),bob=users.saveAndFlush(new AppUser("bob","unused"));
  ZoneId zone=ZoneId.of("Asia/Jerusalem");LocalDate start=LocalDate.now(zone).with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
  Instant boundary=start.atStartOfDay(zone).toInstant();
  for(Instant instant: new Instant[]{boundary,boundary.minusNanos(1000000000),start.plusWeeks(1).atStartOfDay(zone).toInstant()}) {
   OneTimeTask task=new OneTimeTask("Count boundaries", "", null, java.time.LocalDate.of(2026, 11, 8));task.assignOwner(bob);task.markAsCompleted("alice");org.springframework.test.util.ReflectionTestUtils.setField(task,"completedAt",instant);tasks.saveAndFlush(task);
  }
  mvc.perform(get("/tasks/statistics/weekly").with(user("bob"))).andExpect(status().isOk())
   .andExpect(jsonPath("$.members[?(@.username == 'alice')].completedCount").value(org.hamcrest.Matchers.hasItem(1)))
   .andExpect(jsonPath("$.members[?(@.username == 'bob')].completedCount").value(org.hamcrest.Matchers.hasItem(0)));
 }
 @Test void completingTwiceCountsOnceAndPersistsPerformer() throws Exception {
  AppUser alice=users.saveAndFlush(new AppUser("alice","unused"));OneTimeTask task=new OneTimeTask("Complete", "", null, java.time.LocalDate.of(2026, 11, 8));task.assignOwner(alice);task=tasks.saveAndFlush(task);
  for(int i=0;i<2;i++) mvc.perform(patch("/tasks/"+task.getId()+"/complete").with(user("alice")).with(csrf())).andExpect(status().isOk());
  tasks.flush();assertNotNull(tasks.findById(task.getId()).orElseThrow().getCompletedAt());
  mvc.perform(get("/tasks/statistics/weekly").with(user("alice"))).andExpect(jsonPath("$.members[?(@.username == 'alice')].completedCount").value(org.hamcrest.Matchers.hasItem(1)));
 }
}
