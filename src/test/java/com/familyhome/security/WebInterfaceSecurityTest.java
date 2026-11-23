package com.familyhome.security;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class WebInterfaceSecurityTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void loginPageAndItsAssetsArePublic() throws Exception {
        mockMvc.perform(get("/login.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));

        mockMvc.perform(get("/assets/styles.css")).andExpect(status().isOk());

        mockMvc.perform(get("/assets/login.js")).andExpect(status().isOk());
    }

    @Test
    void homePageRequiresBrowserLogin() throws Exception {
        mockMvc.perform(get("/").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login.html"));
    }

    @Test
    void apiStillReturns401InsteadOfAnHtmlLoginPage() throws Exception {
        mockMvc.perform(get("/tasks").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousBrowserCanObtainACsrfToken() throws Exception {
        mockMvc.perform(get("/csrf").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.parameterName").value("_csrf"))
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }

    @Test
    void formLoginWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(
                        post("/login")
                                .param("username", "admin")
                                .param("password", "TestBootstrap123!"))
                .andExpect(status().isForbidden());
    }

    @Test
    void wrongPasswordReturnsToTheLoginPage() throws Exception {
        mockMvc.perform(
                        post("/login")
                                .with(csrf())
                                .param("username", "admin")
                                .param("password", "wrong-test-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login.html?error"));
    }

    @Test
    void formLoginEstablishesASessionAndLogoutInvalidatesIt() throws Exception {
        MvcResult login =
                mockMvc.perform(
                                post("/login")
                                        .with(csrf())
                                        .param("username", "admin")
                                        .param("password", "TestBootstrap123!"))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(redirectedUrl("/"))
                        .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(session);

        mockMvc.perform(get("/me").session(session).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));

        mockMvc.perform(get("/index.html").session(session))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));

        mockMvc.perform(post("/logout").session(session).with(csrf()).accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login.html?logout"));

        assertTrue(session.isInvalid());

        mockMvc.perform(get("/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
