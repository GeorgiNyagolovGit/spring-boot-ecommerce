package com.shop.ecommerce.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registeredCustomerCanLogIn() throws Exception {
        String registration = """
                {
                  "firstName": "Integration",
                  "lastName": "Test",
                  "email": "auth.integration@example.com",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.roles[0]").value("ROLE_CUSTOMER"));

        String login = """
                {
                  "email": "AUTH.INTEGRATION@example.com",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email")
                        .value("auth.integration@example.com"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void demoCustomerCanLogIn() throws Exception {
        String login = """
                {
                  "email": "customer@example.com",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.roles[0]").value("ROLE_CUSTOMER"));
    }

    @Test
    void invalidCredentialsReturnUnauthorized() throws Exception {
        String login = """
                {
                  "email": "customer@example.com",
                  "password": "wrong-password"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Invalid email or password"));
    }
}
