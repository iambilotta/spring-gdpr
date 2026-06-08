package com.example.gdprdemo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
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
 * <p>Auth pattern: explicit {@code httpBasic(username, password)} request post-processor
 * on every secured request. Spring Security 7 (Spring Boot 4) tightened the
 * {@code @WithMockUser} integration with {@code @AutoConfigureMockMvc} so that the
 * mock authentication no longer auto-applies on every request the way it did in
 * Spring Security 6 and earlier; using {@code httpBasic} keeps the test independent
 * of that integration point and matches what a real curl-against-the-running-app
 * would do.
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
                        .with(httpBasic("app", "app-secret"))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("alice-1"));

        mvc.perform(get("/customers/alice-1")
                        .with(httpBasic("app", "app-secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void unauthenticatedAccessIsRejected() throws Exception {
        mvc.perform(get("/customers/alice-1")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/gdpr/erasure/alice-1")).andExpect(status().isUnauthorized());
    }

    @Test
    void erasureEndpointForbiddenForNonDpoRole() throws Exception {
        mvc.perform(delete("/gdpr/erasure/alice-1")
                        .with(httpBasic("app", "app-secret")))
                .andExpect(status().isForbidden());
    }

    @Test
    void accessExportReturnsTheSubjectsClassifiedFieldsForDpo() throws Exception {
        String body = """
                {
                  "id": "carol-3",
                  "fullName": "Carol",
                  "email": "carol@example.com",
                  "taxId": "CT-11111",
                  "healthCondition": "none"
                }
                """;
        mvc.perform(post("/customers")
                        .with(httpBasic("app", "app-secret"))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());

        mvc.perform(get("/gdpr/access/export")
                        .param("subjectId", "carol-3")
                        .with(httpBasic("dpo", "dpo-secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectId").value("carol-3"))
                .andExpect(jsonPath("$.fields[?(@.field == 'email')].value").value("carol@example.com"))
                .andExpect(jsonPath("$.fields[?(@.field == 'email')].category").value("CONTACT"));
    }

    @Test
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
                        .with(httpBasic("app", "app-secret"))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());

        mvc.perform(delete("/gdpr/erasure/bob-2")
                        .with(httpBasic("dpo", "dpo-secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectId").value("bob-2"));
    }
}
