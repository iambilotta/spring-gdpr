package com.iambilotta.gdpr.starter.web;

import java.time.Instant;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.iambilotta.gdpr.starter.access.AccessExportService;
import com.iambilotta.gdpr.starter.access.SubjectAccessExport;
import com.iambilotta.gdpr.starter.audit.AuditAccessRecord;
import com.iambilotta.gdpr.starter.audit.AuditSink;
import com.iambilotta.gdpr.starter.erasure.ErasureReport;
import com.iambilotta.gdpr.starter.erasure.ErasureService;

/**
 * REST surface mandated by Articles 15 (right of access) and 17 (right to erasure).
 *
 * <p>Mounted at the path configured via {@code spring.gdpr.web.base-path}, default {@code /gdpr}.
 *
 * <p>Endpoints exposed:
 * <ul>
 *   <li>{@code DELETE /gdpr/erasure/{subjectId}} runs all configured {@code @GdprErasable} handlers.
 *       Returns {@code 200} with the per-handler aggregate. Orchestration is fail-fast: a throwing
 *       handler propagates (no {@code 207 Multi-Status}); a blank id is rejected {@code 400}.</li>
 *   <li>{@code GET    /gdpr/access/export?subjectId=...} returns the Article 15 dossier assembled from
 *       the registered {@code SubjectDataProvider} beans</li>
 *   <li>{@code GET    /gdpr/audit/access?subjectId=...&from=...&to=...} returns audit records for a subject</li>
 * </ul>
 *
 * <p>The starter does NOT add authentication or authorization. Wire Spring Security rules
 * around {@code spring.gdpr.web.base-path/**} to restrict access to DPO / privileged ops.
 */
@RestController
@RequestMapping("${spring.gdpr.web.base-path:/gdpr}")
public class GdprController {

    private final ErasureService erasureService;
    private final AccessExportService accessExportService;
    private final AuditSink auditSink;

    public GdprController(
            ErasureService erasureService,
            AccessExportService accessExportService,
            AuditSink auditSink) {
        this.erasureService = erasureService;
        this.accessExportService = accessExportService;
        this.auditSink = auditSink;
    }

    @DeleteMapping("/erasure/{subjectId}")
    public ErasureReport erase(@PathVariable String subjectId) {
        return erasureService.eraseSubject(subjectId);
    }

    /**
     * Article 15 right of access: returns the dossier of classified personal-data fields assembled
     * from every registered {@code SubjectDataProvider}. Empty {@code fields} when no provider holds
     * data for the subject (the honesty contract: exactly what the providers return, never more).
     */
    @GetMapping("/access/export")
    public SubjectAccessExport accessExport(@RequestParam String subjectId) {
        return accessExportService.exportSubject(subjectId);
    }

    @GetMapping("/audit/access")
    public List<AuditAccessRecord> auditAccess(
            @RequestParam String subjectId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to
    ) {
        return auditSink.findBySubject(subjectId, from, to);
    }
}
