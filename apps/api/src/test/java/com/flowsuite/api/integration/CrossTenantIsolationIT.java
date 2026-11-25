package com.flowsuite.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MANDATORY cross-tenant isolation test (NFR): a user in Org A must not be able to read,
 * edit, or delete Org B's data. Exercised end-to-end through the HTTP layer against real
 * PostgreSQL + Redis, so JWT auth, tenant resolution, and query scoping are all in play.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class CrossTenantIsolationIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    String tokenA;
    String tokenB;
    String orgAProjectId;
    String orgATaskId;
    String orgACustomerId;

    @BeforeEach
    void setUp() throws Exception {
        tokenA = register("owner-a+" + unique() + "@acme.test", "Alice", "AcmeInc");
        tokenB = register("owner-b+" + unique() + "@globex.test", "Bob", "Globex");

        orgAProjectId = createProject(tokenA, "Org A Secret Project");
        orgATaskId = createTask(tokenA, orgAProjectId, "Org A Task");
        orgACustomerId = createCustomer(tokenA, "Org A Customer");
    }

    @Test
    void userInOrgB_cannotReadOrgAProject() throws Exception {
        mockMvc.perform(get("/api/v1/projects/{id}", orgAProjectId).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
    }

    @Test
    void userInOrgB_cannotEditOrgAProject() throws Exception {
        String body = """
                {"name":"Hacked","description":"pwned"}""";
        mockMvc.perform(put("/api/v1/projects/{id}", orgAProjectId)
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void userInOrgB_cannotDeleteOrgAProject() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/projects/{id}", orgAProjectId).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
    }

    @Test
    void userInOrgB_cannotReadOrChangeOrgATask() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/{id}", orgATaskId).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());

        String statusBody = """
                {"status":"DONE"}""";
        mockMvc.perform(patch("/api/v1/tasks/{id}/status", orgATaskId)
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON).content(statusBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void userInOrgB_cannotReadOrgACustomer() throws Exception {
        mockMvc.perform(get("/api/v1/customers/{id}", orgACustomerId).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
    }

    @Test
    void orgBProjectList_doesNotLeakOrgAProjects() throws Exception {
        mockMvc.perform(get("/api/v1/projects").header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void orgAStillSeesItsOwnProject() throws Exception {
        mockMvc.perform(get("/api/v1/projects/{id}", orgAProjectId).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Org A Secret Project"));
    }

    // --- helpers ---

    private String register(String email, String name, String orgName) throws Exception {
        String body = objectMapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
            put("name", name);
            put("email", email);
            put("password", "password123");
            put("organizationName", orgName);
        }});
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).get("accessToken").asText();
    }

    private String createProject(String token, String name) throws Exception {
        String body = objectMapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
            put("name", name);
            put("description", "desc");
        }});
        MvcResult result = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).get("id").asText();
    }

    private String createTask(String token, String projectId, String title) throws Exception {
        String body = objectMapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
            put("projectId", projectId);
            put("title", title);
        }});
        MvcResult result = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).get("id").asText();
    }

    private String createCustomer(String token, String name) throws Exception {
        String body = objectMapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
            put("name", name);
        }});
        MvcResult result = mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).get("id").asText();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String unique() {
        return Long.toString(System.nanoTime(), 36);
    }
}
