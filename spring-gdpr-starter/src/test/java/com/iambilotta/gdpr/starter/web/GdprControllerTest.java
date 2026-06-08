package com.iambilotta.gdpr.starter.web;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.iambilotta.gdpr.annotations.GdprErasable;
import com.iambilotta.gdpr.annotations.GdprPersonalData;
import com.iambilotta.gdpr.starter.access.AccessExportService;
import com.iambilotta.gdpr.starter.access.SubjectDataProvider;
import com.iambilotta.gdpr.starter.audit.AuditSink;
import com.iambilotta.gdpr.starter.erasure.ErasureHandler;
import com.iambilotta.gdpr.starter.erasure.ErasureService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-boundary contract for {@link GdprController}: the HTTP shape and status codes the README
 * documents. Standalone MockMvc so it stays a fast web slice with no Spring context boot.
 */
class GdprControllerTest {

    private MockMvc mvc(ErasureService erasure, AccessExportService access, AuditSink sink) {
        return MockMvcBuilders.standaloneSetup(new GdprController(erasure, access, sink))
                .setControllerAdvice(new GdprExceptionHandler())
                .build();
    }

    /**
     * @spec.given an access-export endpoint and a registered provider that returns a personal-data object
     * @spec.when  a client GETs /gdpr/access/export with the subject id
     * @spec.then  the Article 15 dossier is returned as 200 with the subject's classified fields
     * @spec.us    US-DX-002-art15-export-endpoint
     */
    @Test
    void exposesTheArticle15AccessExportAsAnEndpoint() throws Exception {
        SubjectDataProvider provider = subjectId -> List.of(new Profile("Alice"));
        AccessExportService access = new AccessExportService(List.of(provider));

        MockMvc mvc = mvc(emptyErasure(), access, mock(AuditSink.class));

        mvc.perform(get("/gdpr/access/export").param("subjectId", "alice-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectId").value("alice-1"))
                .andExpect(jsonPath("$.fields[0].field").value("fullName"))
                .andExpect(jsonPath("$.fields[0].value").value("Alice"));
    }

    /**
     * @spec.given an access-export endpoint
     * @spec.when  a client GETs the export with a blank subject id
     * @spec.then  the request is rejected with 400 Bad Request, not a raw 500
     * @spec.us    US-DX-001-honest-erasure-error-contract
     */
    @Test
    void blankSubjectIdOnExportIs400NotRaw500() throws Exception {
        AccessExportService access = new AccessExportService(List.of());

        MockMvc mvc = mvc(emptyErasure(), access, mock(AuditSink.class));

        mvc.perform(get("/gdpr/access/export").param("subjectId", " "))
                .andExpect(status().isBadRequest());
    }

    /**
     * @spec.given an erasure endpoint and a handler that throws while erasing
     * @spec.when  a client DELETEs the subject
     * @spec.then  the handler exception propagates (fail-fast), never swallowed into a 207 success
     * @spec.us    US-DX-001-honest-erasure-error-contract
     */
    @Test
    void throwingHandlerSurfacesAsServerErrorNot207() throws Exception {
        ErasureHandler throwing = new ErasureHandler() {
            @Override
            public Class<?> entityType() {
                return Profile.class;
            }

            @Override
            public GdprErasable.Strategy strategy() {
                return GdprErasable.Strategy.DELETE;
            }

            @Override
            public int erase(String subjectId) {
                throw new IllegalStateException("downstream delete failed");
            }
        };
        ErasureService erasure = new ErasureService(List.of(throwing));

        MockMvc mvc = mvc(erasure, new AccessExportService(List.of()), mock(AuditSink.class));

        // The handler exception is not mapped to a 207/200 success: it propagates (fail-fast). The
        // library advice only catches IllegalArgumentException, so a domain failure stays a server
        // error (raw 500 in a deployed DispatcherServlet, as documented in ADR-0004).
        assertThatThrownBy(() -> mvc.perform(delete("/gdpr/erasure/alice-1")))
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .rootCause()
                .hasMessage("downstream delete failed");
    }

    private static ErasureService emptyErasure() {
        return new ErasureService(List.of());
    }

    static class Profile {
        @GdprPersonalData(description = "full legal name")
        private final String fullName;

        Profile(String fullName) {
            this.fullName = fullName;
        }
    }
}
