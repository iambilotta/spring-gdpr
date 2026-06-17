# Coverage — spring-gdpr-starter (as-is)

⚠ **No JaCoCo CSV found** at `apps/gest/target/site/jacoco/jacoco.csv`. Run `make coverage` (alias for `./mvnw test` + regen) and try again. Without the CSV every file below shows 0% — that's the absence of data, not a result.

**Files**: 47 · **Overall line coverage**: 0.0% (0/0)

## By package

| Package | Files | Lines covered | Line % | Branch % |
|---|---|---|---|---|
| `com.iambilotta.gdpr.starter` | 1 | 0/0 | 0.0% | 0.0% |
| `com.iambilotta.gdpr.starter.access` | 5 | 0/0 | 0.0% | 0.0% |
| `com.iambilotta.gdpr.starter.audit` | 9 | 0/0 | 0.0% | 0.0% |
| `com.iambilotta.gdpr.starter.autoconfig` | 2 | 0/0 | 0.0% | 0.0% |
| `com.iambilotta.gdpr.starter.erasure` | 8 | 0/0 | 0.0% | 0.0% |
| `com.iambilotta.gdpr.starter.erasure.crypto` | 7 | 0/0 | 0.0% | 0.0% |
| `com.iambilotta.gdpr.starter.erasure.forgettable` | 6 | 0/0 | 0.0% | 0.0% |
| `com.iambilotta.gdpr.starter.jdbc` | 1 | 0/0 | 0.0% | 0.0% |
| `com.iambilotta.gdpr.starter.logging` | 2 | 0/0 | 0.0% | 0.0% |
| `com.iambilotta.gdpr.starter.retention` | 3 | 0/0 | 0.0% | 0.0% |
| `com.iambilotta.gdpr.starter.web` | 3 | 0/0 | 0.0% | 0.0% |

## Per file (all production sources)

| File | Lines | Line % | Branch % | Methods |
|---|---|---|---|---|
| `com/iambilotta/gdpr/starter/GdprProperties.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/access/AccessExportService.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/access/ExportedField.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/access/JdbcSubjectDataProvider.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/access/SubjectAccessExport.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/access/SubjectDataProvider.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/audit/ActorResolver.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/audit/AsyncAuditSinkDecorator.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/audit/AuditAccessRecord.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/audit/AuditSink.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/audit/AuditSinkMetrics.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/audit/JdbcAuditSink.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/audit/PersonalDataAccessAdvisor.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/audit/Slf4jAuditSink.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/audit/SubjectIdResolver.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/autoconfig/GdprAutoConfiguration.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/autoconfig/GdprErasableScannerRegistrar.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/erasure/CompositeSubjectErasureHandler.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/erasure/ErasureHandler.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/erasure/ErasureListener.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/erasure/ErasureListenerException.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/erasure/ErasureReport.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/erasure/ErasureService.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/erasure/JdbcErasureHandler.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/erasure/SubjectErasedEvent.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/erasure/crypto/AesGcmCryptoShredder.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/erasure/crypto/CryptoShredder.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/erasure/crypto/CryptoShreddingErasureHandler.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/erasure/crypto/InMemorySubjectKeyStore.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/erasure/crypto/JdbcSubjectKeyStore.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/erasure/crypto/SubjectKeyFactory.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/erasure/crypto/SubjectKeyStore.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/erasure/forgettable/ForgettablePayloadErasureHandler.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/erasure/forgettable/ForgettablePayloadReference.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/erasure/forgettable/ForgettablePayloadResolver.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/erasure/forgettable/ForgettablePayloadStore.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/erasure/forgettable/InMemoryForgettablePayloadStore.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/erasure/forgettable/JdbcForgettablePayloadStore.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/jdbc/SqlIdentifiers.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/logging/PiiMasker.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/logging/PiiMaskingConverter.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/retention/JdbcRetentionTarget.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/retention/RetentionScheduler.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/retention/RetentionTarget.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/web/GdprController.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/web/GdprExceptionHandler.java` | — | — | — | 0/0 |
| `com/iambilotta/gdpr/starter/web/GdprSecurityWarning.java` | — | — | — | 0/0 |
