package com.familyhome;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familyhome.event.EventRequest;
import com.familyhome.shopping.ShoppingItemRequest;
import com.familyhome.task.TaskRequest;
import com.familyhome.task.TaskType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsOversizedNamesAndDescriptions() {
        String over100 = "a".repeat(101);
        String over500 = "b".repeat(501);

        assertFalse(
                validator
                        .validate(
                                new TaskRequest(
                                        over100,
                                        "ok",
                                        1L,
                                        TaskType.ONETIME,
                                        LocalDate.of(2026, 11, 25),
                                        null))
                        .isEmpty());
        assertFalse(
                validator
                        .validate(
                                new TaskRequest(
                                        "ok",
                                        over500,
                                        1L,
                                        TaskType.ONETIME,
                                        LocalDate.of(2026, 11, 25),
                                        null))
                        .isEmpty());
        assertFalse(validator.validate(new ShoppingItemRequest(over100, 1)).isEmpty());
        assertFalse(
                validator
                        .validate(new EventRequest(over500, LocalDate.of(2026, 11, 25), List.of()))
                        .isEmpty());
    }

    @Test
    void acceptsBoundaryLengths() {
        String max100 = "a".repeat(100);
        String max500 = "b".repeat(500);

        assertTrue(
                validator
                        .validate(
                                new TaskRequest(
                                        max100,
                                        max500,
                                        1L,
                                        TaskType.ONETIME,
                                        LocalDate.of(2026, 11, 25),
                                        null))
                        .isEmpty());
        assertTrue(validator.validate(new ShoppingItemRequest(max100, 1)).isEmpty());
        assertTrue(
                validator
                        .validate(new EventRequest(max500, LocalDate.of(2026, 11, 25), List.of()))
                        .isEmpty());
    }
}
