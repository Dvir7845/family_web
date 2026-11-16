package com.familyhome.task;

import com.familyhome.member.AppUser;
import com.familyhome.member.AppUserRepository;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:task-repository;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.auto_quote_keyword=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TaskRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Test
    void queriesReturnOnlyTasksOfRequestedOwner() {
        // creating two users
        AppUser firstUser = userRepository.save(
            new AppUser("first-user", "unused-test-hash")
        );

        AppUser secondUser = userRepository.save(
            new AppUser("second-user", "unused-test-hash")
        );

        // creating a task for each user
        OneTimeTask firstTask = new OneTimeTask("Shopping", "Buy milk", null, java.time.LocalDate.of(2026, 11, 8));
        firstTask.assignOwner(firstUser);
        taskRepository.saveAndFlush(firstTask);

        RecurringTask secondTask = new RecurringTask("Cleaning", "Clean kitchen", secondUser, java.time.DayOfWeek.MONDAY);
        secondTask.assignOwner(secondUser);
        taskRepository.saveAndFlush(secondTask);
        entityManager.clear();

        assertEquals(java.time.LocalDate.of(2026, 11, 8),
            assertInstanceOf(OneTimeTask.class, taskRepository.findById(firstTask.getId()).orElseThrow()).getTaskDate());
        assertEquals(java.time.DayOfWeek.MONDAY,
            assertInstanceOf(RecurringTask.class, taskRepository.findById(secondTask.getId()).orElseThrow()).getDay());

        // each list includes only the task of the appropriate owner
        assertEquals(
            List.of(firstTask.getId()),
            taskRepository.findAllByOwnerId(firstUser.getId())
                .stream()
                .map(BaseTask::getId)
                .toList()
        );

        assertEquals(
            List.of(secondTask.getId()),
            taskRepository.findAllByOwnerId(secondUser.getId())
                .stream()
                .map(BaseTask::getId)
                .toList()
        );

        // the task exists for its owner
        assertTrue(
            taskRepository.findByIdAndOwnerId(
                secondTask.getId(), secondUser.getId()
            ).isPresent()
        );

        // the same task does not exist for another user
        assertTrue(
            taskRepository.findByIdAndOwnerId(
                secondTask.getId(), firstUser.getId()
            ).isEmpty()
        );
    }
}
