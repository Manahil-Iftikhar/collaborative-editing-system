package com.collab;

import com.collab.security.JwtUtil;
import com.collab.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "JWT_SECRET=test-only-signing-material-not-for-deployment-123456")
@AutoConfigureMockMvc
@Transactional
class UserAccessTest {
    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwt;
    @Autowired UserRepository users;
    @Autowired ObjectMapper json;
    String aliceToken;
    Long aliceId;
    Long bobId;

    @BeforeEach
    void setup() throws Exception {
        users.deleteAll();
        register("alice");
        register("bob");
        aliceId = users.findByUsername("alice").orElseThrow().getId();
        bobId = users.findByUsername("bob").orElseThrow().getId();
        var response = mvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username", "alice", "password", "test-password"))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        aliceToken = json.readTree(response).get("token").asText();
    }

    void register(String username) throws Exception {
        mvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username", username, "email", username+"@example.test",
                "password", "test-password", "fullName", username))))
            .andExpect(status().isCreated());
    }

    @Test
    void rejectsAnonymousProfileReadsAndWrites() throws Exception {
        mvc.perform(get("/api/users/profile/alice")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/"+aliceId)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/users/profile/alice").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerCanReadAndUpdate() throws Exception {
        mvc.perform(get("/api/users/profile/alice").header("Authorization", "Bearer "+aliceToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("alice"));
        mvc.perform(get("/api/users/"+aliceId).header("Authorization", "Bearer "+aliceToken))
            .andExpect(status().isOk());
        mvc.perform(put("/api/users/profile/alice").header("Authorization", "Bearer "+aliceToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"Updated Alice\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Updated Alice"));
    }

    @Test
    void rejectsCrossAccountReadsAndWrites() throws Exception {
        mvc.perform(get("/api/users/profile/bob").header("Authorization", "Bearer "+aliceToken))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/users/"+bobId).header("Authorization", "Bearer "+aliceToken))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/users/profile/bob").header("Authorization", "Bearer "+aliceToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"Changed\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/users/profile/bob").header("Authorization", "Bearer "+jwt.generateToken("bob")))
            .andExpect(jsonPath("$.fullName").value("bob"));
    }

    @Test
    void rejectsMalformedAndUntrustedTokens() throws Exception {
        mvc.perform(get("/api/users/profile/alice").header("Authorization", "Bearer broken"))
            .andExpect(status().isUnauthorized());
        JwtUtil other = new JwtUtil("other-test-only-signing-material-never-for-production");
        mvc.perform(get("/api/users/profile/alice").header("Authorization", "Bearer "+other.generateToken("alice")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUnknownOrInactiveUsers() throws Exception {
        mvc.perform(get("/api/users/profile/ghost").header("Authorization", "Bearer "+jwt.generateToken("ghost")))
            .andExpect(status().isUnauthorized());
        var alice = users.findByUsername("alice").orElseThrow();
        alice.setActive(false); users.saveAndFlush(alice);
        mvc.perform(get("/api/users/profile/alice").header("Authorization", "Bearer "+aliceToken))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deniesUnlistedEndpoints() throws Exception {
        mvc.perform(get("/h2-console").header("Authorization", "Bearer "+aliceToken))
            .andExpect(status().isForbidden());
    }
}
