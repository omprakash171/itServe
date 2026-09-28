package com.itserve;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end API test of the core service desk workflow using the seeded demo data:
 * employee raises a ticket -> admin assigns an agent -> agent works and resolves it ->
 * employee closes and rates it. Also covers auth failures and RBAC.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ItServeApiIntegrationTest {

    private static final String DEMO_PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", DEMO_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    @Order(1)
    void loginRejectsBadCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@itserve.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    void protectedEndpointsRequireToken() throws Exception {
        mockMvc.perform(get("/api/tickets")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/dashboard/admin")).andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    void employeesCannotAccessAdminEndpoints() throws Exception {
        String employeeToken = login("employee1@itserve.com");
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard/admin").header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    void fullTicketLifecycleWorks() throws Exception {
        String employeeToken = login("employee1@itserve.com");
        String adminToken = login("admin@itserve.com");
        String agentToken = login("agent1@itserve.com");

        // Categories list drives the create form
        JsonNode categories = objectMapper.readTree(mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        long categoryId = categories.get(0).get("id").asLong();

        // 1. Employee raises a MEDIUM ticket -> SLA deadline = createdAt + 8h
        MvcResult created = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "subject", "Integration test ticket - VPN slow",
                                "description", "VPN throughput dropped badly during the day, affecting remote work.",
                                "categoryId", categoryId,
                                "priority", "MEDIUM",
                                "location", "Remote",
                                "preferredContact", "EMAIL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketNumber").value(org.hamcrest.Matchers.matchesRegex("INC-\\d{4}-\\d{6}")))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.slaDeadline").isNotEmpty())
                .andReturn();
        long ticketId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // Validation: description too short is rejected
        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"ab\",\"description\":\"short\",\"categoryId\":1,\"priority\":\"LOW\"}"))
                .andExpect(status().isUnprocessableEntity());

        // Employee cannot jump straight to RESOLVED (invalid transition from OPEN)
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\",\"note\":\"fixed myself\"}"))
                .andExpect(status().isUnprocessableEntity());

        // 2. Admin assigns a support agent -> status becomes ASSIGNED, agent gets notified
        JsonNode agents = objectMapper.readTree(mockMvc.perform(get("/api/users?role=SUPPORT_AGENT")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        long agentId = agents.get(0).get("id").asLong();

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":" + agentId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.type == 'TICKET_ASSIGNED')]").exists());

        // Employees cannot assign
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":" + agentId + "}"))
                .andExpect(status().isForbidden());

        // 3. Agent accepts and starts working
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // Agent comments
        mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Checked the concentrator load, throttling an old firmware bug.\"}"))
                .andExpect(status().isOk());

        // Resolving without a note is rejected
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\",\"note\":\"Updated concentrator firmware; throughput restored.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());

        // 4. Employee closes and rates
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(post("/api/tickets/" + ticketId + "/rating")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stars\":5,\"feedback\":\"Fast and transparent.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stars").value(5));

        // Ticket detail shows the full activity timeline
        mockMvc.perform(get("/api/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(1))
                .andExpect(jsonPath("$.history[?(@.action == 'CREATED')]").exists())
                .andExpect(jsonPath("$.history[?(@.action == 'ASSIGNED')]").exists())
                .andExpect(jsonPath("$.history[?(@.action == 'STATUS_CHANGED')]").exists());

        // Closed tickets reject further changes
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(5)
    void dashboardsReturnLiveMetrics() throws Exception {
        String adminToken = login("admin@itserve.com");
        mockMvc.perform(get("/api/dashboard/admin").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.totalTickets").isNumber())
                .andExpect(jsonPath("$.kpis.slaCompliancePct").isNumber())
                .andExpect(jsonPath("$.byStatus").isArray())
                .andExpect(jsonPath("$.agentWorkload").isArray())
                .andExpect(jsonPath("$.createdOverTime.length()").value(14));

        String agentToken = login("agent2@itserve.com");
        mockMvc.perform(get("/api/dashboard/agent").header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.myOpenTickets").isNumber())
                .andExpect(jsonPath("$.todaysTasks").isArray());

        String employeeToken = login("employee2@itserve.com");
        mockMvc.perform(get("/api/dashboard/employee").header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.myOpenTickets").isNumber())
                .andExpect(jsonPath("$.recentTickets").isArray());
    }

    @Test
    @Order(6)
    void ticketSearchFilterAndPaginationWork() throws Exception {
        String adminToken = login("admin@itserve.com");
        mockMvc.perform(get("/api/tickets?search=VPN&page=0&size=5")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(org.hamcrest.Matchers.lessThanOrEqualTo(5)))
                .andExpect(jsonPath("$.totalElements").isNumber());

        mockMvc.perform(get("/api/tickets?status=OPEN&priority=CRITICAL")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tickets?sla=breached")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(7)
    void knowledgeBaseAndSlaRulesAreAvailable() throws Exception {
        String employeeToken = login("employee1@itserve.com");
        mockMvc.perform(get("/api/articles?search=VPN").header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").isNotEmpty());

        mockMvc.perform(get("/api/sla-rules").header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    @Order(8)
    void employeeOnlySeesOwnTickets() throws Exception {
        String employeeToken = login("employee3@itserve.com");
        MvcResult result = mockMvc.perform(get("/api/tickets?size=100")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode page = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode ticket : page.get("content")) {
            assertThat(ticket.get("createdBy").get("email").asText()).isEqualTo("employee3@itserve.com");
        }
    }
}
