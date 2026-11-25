package com.familyhome.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.ServletException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new TestController())
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .build();

    @RestController
    static class TestController {
        @GetMapping("/test/items/{id}")
        String getItem(@PathVariable long id) {
            return "ok:" + id;
        }

        @PostMapping("/test/items")
        String createItem(@RequestBody Map<String, Object> body) {
            return "created";
        }

        @GetMapping("/test/conflict")
        String conflict() {
            throw new DataIntegrityViolationException("unique_constraint");
        }

        @GetMapping("/test/unexpected")
        String unexpected() {
            throw new IllegalStateException("secret internal details");
        }

        @GetMapping("/test/denied")
        String denied() {
            throw new AccessDeniedException("Forbidden by security");
        }
    }

    @Test
    void returnsBadRequestProblemDetailOnTypeMismatch() throws Exception {
        mockMvc.perform(get("/test/items/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Invalid value for parameter 'id'"));
    }

    @Test
    void returnsBadRequestProblemDetailOnMalformedJson() throws Exception {
        mockMvc.perform(
                        post("/test/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{bad-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Malformed JSON request body"));
    }

    @Test
    void returnsConflictProblemDetailOnDataIntegrityViolation() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Operation conflicts with existing data"));
    }

    @Test
    void returnsInternalServerErrorWithoutLeakingDetails() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }

    @Test
    void rethrowsSecurityExceptionsInsteadOfReturning500() {
        ServletException thrown =
                assertThrows(ServletException.class, () -> mockMvc.perform(get("/test/denied")));
        assertInstanceOf(AccessDeniedException.class, thrown.getCause());
        assertEquals("Forbidden by security", thrown.getCause().getMessage());
    }
}
