package com.example.rest_service.task;

import com.example.rest_service.member.AppUser;
import com.example.rest_service.member.AppUserRepository;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.auto_quote_keyword=true"
})
class TaskRepositoryTest {

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
        Task firstTask = new Task("Shopping", "Sunday", "Buy milk");
        firstTask.assignOwner(firstUser);
        taskRepository.saveAndFlush(firstTask);

        Task secondTask = new Task("Cleaning", "Monday", "Clean kitchen");
        secondTask.assignOwner(secondUser);
        taskRepository.saveAndFlush(secondTask);

        // each list includes only the task of the appropriate owner
        assertEquals(
            List.of(firstTask.getId()),
            taskRepository.findAllByOwnerId(firstUser.getId())
                .stream()
                .map(Task::getId)
                .toList()
        );

        assertEquals(
            List.of(secondTask.getId()),
            taskRepository.findAllByOwnerId(secondUser.getId())
                .stream()
                .map(Task::getId)
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