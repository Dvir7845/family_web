package com.familyhome.member;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = {
                "spring.datasource.url=jdbc:h2:mem:current-member-api;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.properties.hibernate.auto_quote_keyword=true",
                "APP_LOGIN_PASSWORD=TestBootstrap123!"
})
@AutoConfigureMockMvc
class CurrentMemberApiTest {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void unauthenticatedRequestIsRejected() throws Exception {
                mockMvc.perform(
                                get("/me")
                                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void authenticatedUserGetsOwnProfile() throws Exception {
                mockMvc.perform(
                                get("/me")
                                                .with(httpBasic("dvir", "TestBootstrap123!"))
                                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").isNumber())
                                .andExpect(jsonPath("$.username").value("dvir"))
                                .andExpect(jsonPath("$.role").value("ADMIN"))
                                .andExpect(jsonPath("$.password").doesNotExist())
                                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        }

        @Test
        @Transactional
        void adminCreatesMemberWhoCanLoginButCannotCreateAccounts()
                        throws Exception {

                // the admin creates a new member account
                mockMvc.perform(
                                post("/members")
                                                .with(httpBasic("dvir", "TestBootstrap123!"))
                                                .with(csrf())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                  "username": "test_member",
                                                                  "password": "MemberPass123!"
                                                                }
                                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").isNumber())
                                .andExpect(jsonPath("$.username").value("test_member"))
                                .andExpect(jsonPath("$.password").doesNotExist())
                                .andExpect(jsonPath("$.passwordHash").doesNotExist());

                // the new member can log in and get their own profile
                mockMvc.perform(
                                get("/me")
                                                .with(httpBasic("test_member", "MemberPass123!"))
                                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("test_member"))
                                .andExpect(jsonPath("$.role").value("MEMBER"));

                // the new member is not allowed to create additional accounts
                mockMvc.perform(
                                post("/members")
                                                .with(httpBasic("test_member", "MemberPass123!"))
                                                .with(csrf())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                  "username": "another_member",
                                                                  "password": "AnotherPass123!"
                                                                }
                                                                """))
                                .andExpect(status().isForbidden());
        }

        private long createTestMember(String username) throws Exception {
                String response = mockMvc.perform(
                                post("/members")
                                                .with(httpBasic("dvir", "TestBootstrap123!"))
                                                .with(csrf())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                  "username": "%s",
                                                                  "password": "MemberPass123!"
                                                                }
                                                                """.formatted(username)))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                Number id = JsonPath.read(response, "$.id");
                return id.longValue();
        }

        @Test
        @Transactional
        void everyoneCanSeeTaskButOnlyAssigneeCanCompleteIt()
                        throws Exception {

                long assigneeId = createTestMember("assigned_member");
                createTestMember("viewer_member");

                // the admin creates a task and assigns it to the first member
                String response = mockMvc.perform(
                                post("/tasks")
                                                .with(httpBasic("dvir", "TestBootstrap123!"))
                                                .with(csrf())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                  "name": "Shopping",
                                                                  "day": "Sunday",
                                                                  "description": "Buy milk",
                                                                  "assigneeId": %d
                                                                }
                                                                """.formatted(assigneeId)))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                Number taskId = JsonPath.read(response, "$.id");

                // another family member sees the task and the assignee
                mockMvc.perform(
                                get("/tasks")
                                                .with(httpBasic("viewer_member", "MemberPass123!")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].assigneeUsername")
                                                .value("assigned_member"));

                // the same family member cannot mark as complete
                mockMvc.perform(
                                patch("/tasks/" + taskId + "/complete")
                                                .with(httpBasic("viewer_member", "MemberPass123!"))
                                                .with(csrf()))
                                .andExpect(status().isNotFound());

                // the blocked attempt does not change the task's status
                mockMvc.perform(
                                get("/tasks/" + taskId)
                                                .with(httpBasic("viewer_member", "MemberPass123!")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.completed").value(false));

                // the assignee themselves can mark as complete
                mockMvc.perform(
                                patch("/tasks/" + taskId + "/complete")
                                                .with(httpBasic("assigned_member", "MemberPass123!"))
                                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.completed").value(true));
        }

        @Test
        @Transactional
        void familyMembersCanManageSharedShoppingItems() throws Exception {
                createTestMember("shopping_member");
                createTestMember("other_shopper");

                // member creates a shopping item
                String response = mockMvc.perform(
                                post("/shopping-items")
                                                .with(httpBasic("shopping_member", "MemberPass123!"))
                                                .with(csrf())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                  "productName": "Milk",
                                                                  "quantity": 2
                                                                }
                                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                Number itemId = JsonPath.read(response, "$.id");

                // other family member sees the item in the shared list
                mockMvc.perform(
                                get("/shopping-items")
                                                .with(httpBasic("other_shopper", "MemberPass123!")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].productName").value("Milk"))
                                .andExpect(jsonPath("$[0].quantity").value(2));

                // he is also allowed to update the quantity
                mockMvc.perform(
                                put("/shopping-items/" + itemId)
                                                .with(httpBasic("other_shopper", "MemberPass123!"))
                                                .with(csrf())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                  "productName": "Milk",
                                                                  "quantity": 3
                                                                }
                                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.quantity").value(3));

                // the first member deletes the item
                mockMvc.perform(
                                delete("/shopping-items/" + itemId)
                                                .with(httpBasic("shopping_member", "MemberPass123!"))
                                                .with(csrf()))
                                .andExpect(status().isNoContent());

                // the item is no longer available to the other member
                mockMvc.perform(
                                get("/shopping-items/" + itemId)
                                                .with(httpBasic("other_shopper", "MemberPass123!")))
                                .andExpect(status().isNotFound());
        }
}