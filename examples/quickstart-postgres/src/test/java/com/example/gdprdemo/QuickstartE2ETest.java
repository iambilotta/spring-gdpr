package com.example.gdprdemo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test that exercises the full path: REST -> repository -> AOP audit ->
 * sink (slf4j fallback for tests, Postgres in production), erasure REST -> handler ->
 * delete -> aggregate report.
 *
 * <p>Uses an in-process H2 database with auto-create enabled to keep the test self-contained.
 * The shape (controller / repository / advisor / erasure handler / autoconfig wiring) is
 * identical to the production deployment.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:quickstart-e2e;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.gdpr.audit.jdbc-enabled=false",
        "spring.gdpr.audit.async.enabled=false"
})
class QuickstartE2ETest {

    @Autowired
    MockMvc mvc;

    @Test
    @WithMockUser(username = "app", roles = "USER")
    void createCustomerAndFetchHits200() throws Exception {
        String body = """
                {
                  "id": "alice-1",
                  "fullName": "Alice",
                  "email": "alice@example.com",
                  "taxId": "AT-12345",
                  "healthCondition": "asthma"
                }
                """;
        mvc.perform(post("/customers")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("alice-1"));

        mvc.perform(get("/customers/alice-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void unauthenticatedAccessIsRejected() throws Exception {
        mvc.perform(get("/customers/alice-1")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/gdpr/erasure/alice-1")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "app", roles = "USER")
    void erasureEndpointForbiddenForNonDpoRole() throws Exception {
        mvc.perform(delete("/gdpr/erasure/alice-1")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dpo", roles = "DPO")
    void erasureEndpointAccessibleToDpoRole() throws Exception {
        String body = """
                {
                  "id": "bob-2",
                  "fullName": "Bob",
                  "email": "bob@example.com",
                  "taxId": "BT-67890",
                  "healthCondition": null
                }
                """;
        mvc.perform(post("/customers")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("app").roles("USER"))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());

        mvc.perform(delete("/gdpr/erasure/bob-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectId").value("bob-2"));
    }
}
