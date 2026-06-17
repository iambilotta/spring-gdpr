# Structure — `spring-gdpr`

Convention-driven skeleton of the repository: the git-tracked files (respecting `.gitignore`, so no `node_modules` / `target` / build output), rendered as a tree. A single readable snapshot of where everything lives, for a human or an agent orienting in a fresh session. Regenerated on every commit like every `_generated` doc; the source of truth is the filesystem, never this markdown.

_194 tracked paths._

```
spring-gdpr/
├── .github/
│   ├── ISSUE_TEMPLATE/
│   │   ├── bug.md
│   │   └── feature.md
│   ├── workflows/
│   │   ├── ci.yml
│   │   └── codeql.yml
│   ├── dependabot.yml
│   ├── PULL_REQUEST_TEMPLATE.md
│   └── pull_request_template.md
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
├── docs/
│   ├── adr/
│   │   ├── 0000-template.md
│   │   ├── 0001-annotations-as-source-of-truth.md
│   │   ├── 0002-async-audit-sink-default.md
│   │   ├── 0003-build-time-and-runtime-as-two-products.md
│   │   ├── 0004-erasure-handler-orchestration.md
│   │   ├── 0005-retention-via-spring-scheduled.md
│   │   ├── 0006-typed-class-on-spi.md
│   │   ├── 0007-subjectidfield-is-documentation-only.md
│   │   ├── 0008-consent-and-portability-deferred.md
│   │   ├── 0009-append-only-erasure-crypto-shredding.md
│   │   ├── 0010-forgettable-payload-primary-pattern.md
│   │   └── README.md
│   ├── articles/
│   │   └── 01-gdpr-compliance-by-annotation.md
│   ├── requirements/
│   │   └── gdpr.requirements.md
│   └── DX-review.md
├── examples/
│   └── quickstart-postgres/
│       ├── _generated/
│       │   ├── config.md
│       │   ├── http-endpoints.md
│       │   ├── modules.md
│       │   ├── ports.md
│       │   ├── requirements-by-us.md
│       │   ├── requirements.json
│       │   ├── requirements.md
│       │   └── templates.md
│       ├── src/
│       │   ├── main/
│       │   │   ├── java/
│       │   │   │   └── com/
│       │   │   │       └── example/
│       │   │   │           └── gdprdemo/
│       │   │   │               ├── Customer.java
│       │   │   │               ├── CustomerController.java
│       │   │   │               ├── CustomerErasureHandler.java
│       │   │   │               ├── CustomerRepository.java
│       │   │   │               ├── GdprExportConfig.java
│       │   │   │               ├── QuickstartApplication.java
│       │   │   │               └── SecurityConfig.java
│       │   │   └── resources/
│       │   │       ├── db/
│       │   │       │   └── migration/
│       │   │       │       ├── V1__gdpr_audit_access.sql
│       │   │       │       └── V2__customers_table.sql
│       │   │       ├── application.yml
│       │   │       └── logback-spring.xml
│       │   └── test/
│       │       └── java/
│       │           └── com/
│       │               └── example/
│       │                   └── gdprdemo/
│       │                       └── QuickstartE2ETest.java
│       ├── docker-compose.yml
│       ├── pom.xml
│       └── README.md
├── spring-gdpr-annotations/
│   ├── src/
│   │   └── main/
│   │       └── java/
│   │           └── com/
│   │               └── iambilotta/
│   │                   └── gdpr/
│   │                       └── annotations/
│   │                           ├── GdprDataSubjects.java
│   │                           ├── GdprErasable.java
│   │                           ├── GdprLegalBasis.java
│   │                           ├── GdprPersonalData.java
│   │                           ├── GdprRetention.java
│   │                           └── package-info.java
│   └── pom.xml
├── spring-gdpr-benchmark/
│   ├── results/
│   │   └── 2026-05-02-jdk25-corretto.json
│   ├── src/
│   │   └── main/
│   │       └── java/
│   │           └── com/
│   │               └── iambilotta/
│   │                   └── gdpr/
│   │                       └── benchmark/
│   │                           └── AuditSinkBenchmark.java
│   ├── pom.xml
│   └── README.md
├── spring-gdpr-maven-plugin/
│   ├── src/
│   │   └── main/
│   │       └── java/
│   │           └── com/
│   │               └── iambilotta/
│   │                   └── gdpr/
│   │                       └── plugin/
│   │                           ├── AbstractGdprMojo.java
│   │                           ├── DpiaMojo.java
│   │                           ├── RopaMojo.java
│   │                           └── VerifyMojo.java
│   └── pom.xml
├── spring-gdpr-processor/
│   ├── _generated/
│   │   ├── requirements-by-us.md
│   │   ├── requirements.json
│   │   └── requirements.md
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── iambilotta/
│   │   │   │           └── gdpr/
│   │   │   │               └── processor/
│   │   │   │                   └── GdprAnnotationProcessor.java
│   │   │   └── resources/
│   │   │       └── META-INF/
│   │   │           └── services/
│   │   │               └── javax.annotation.processing.Processor
│   │   └── test/
│   │       └── java/
│   │           └── com/
│   │               └── iambilotta/
│   │                   └── gdpr/
│   │                       └── processor/
│   │                           └── ProcessingRecordCategoryTest.java
│   └── pom.xml
├── spring-gdpr-starter/
│   ├── _generated/
│   │   ├── config.md
│   │   ├── http-endpoints.md
│   │   ├── modules.md
│   │   ├── ports.md
│   │   ├── requirements-by-us.md
│   │   ├── requirements.json
│   │   ├── requirements.md
│   │   └── templates.md
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── iambilotta/
│   │   │   │           └── gdpr/
│   │   │   │               └── starter/
│   │   │   │                   ├── access/
│   │   │   │                   │   ├── AccessExportService.java
│   │   │   │                   │   ├── ExportedField.java
│   │   │   │                   │   ├── JdbcSubjectDataProvider.java
│   │   │   │                   │   ├── SubjectAccessExport.java
│   │   │   │                   │   └── SubjectDataProvider.java
│   │   │   │                   ├── audit/
│   │   │   │                   │   ├── ActorResolver.java
│   │   │   │                   │   ├── AsyncAuditSinkDecorator.java
│   │   │   │                   │   ├── AuditAccessRecord.java
│   │   │   │                   │   ├── AuditSink.java
│   │   │   │                   │   ├── AuditSinkMetrics.java
│   │   │   │                   │   ├── JdbcAuditSink.java
│   │   │   │                   │   ├── PersonalDataAccessAdvisor.java
│   │   │   │                   │   ├── Slf4jAuditSink.java
│   │   │   │                   │   └── SubjectIdResolver.java
│   │   │   │                   ├── autoconfig/
│   │   │   │                   │   ├── GdprAutoConfiguration.java
│   │   │   │                   │   └── GdprErasableScannerRegistrar.java
│   │   │   │                   ├── erasure/
│   │   │   │                   │   ├── crypto/
│   │   │   │                   │   │   ├── AesGcmCryptoShredder.java
│   │   │   │                   │   │   ├── CryptoShredder.java
│   │   │   │                   │   │   ├── CryptoShreddingErasureHandler.java
│   │   │   │                   │   │   ├── InMemorySubjectKeyStore.java
│   │   │   │                   │   │   ├── JdbcSubjectKeyStore.java
│   │   │   │                   │   │   ├── SubjectKeyFactory.java
│   │   │   │                   │   │   └── SubjectKeyStore.java
│   │   │   │                   │   ├── forgettable/
│   │   │   │                   │   │   ├── ForgettablePayloadErasureHandler.java
│   │   │   │                   │   │   ├── ForgettablePayloadReference.java
│   │   │   │                   │   │   ├── ForgettablePayloadResolver.java
│   │   │   │                   │   │   ├── ForgettablePayloadStore.java
│   │   │   │                   │   │   ├── InMemoryForgettablePayloadStore.java
│   │   │   │                   │   │   └── JdbcForgettablePayloadStore.java
│   │   │   │                   │   ├── CompositeSubjectErasureHandler.java
│   │   │   │                   │   ├── ErasureHandler.java
│   │   │   │                   │   ├── ErasureListener.java
│   │   │   │                   │   ├── ErasureListenerException.java
│   │   │   │                   │   ├── ErasureReport.java
│   │   │   │                   │   ├── ErasureService.java
│   │   │   │                   │   ├── JdbcErasureHandler.java
│   │   │   │                   │   └── SubjectErasedEvent.java
│   │   │   │                   ├── jdbc/
│   │   │   │                   │   └── SqlIdentifiers.java
│   │   │   │                   ├── logging/
│   │   │   │                   │   ├── PiiMasker.java
│   │   │   │                   │   └── PiiMaskingConverter.java
│   │   │   │                   ├── retention/
│   │   │   │                   │   ├── JdbcRetentionTarget.java
│   │   │   │                   │   ├── RetentionScheduler.java
│   │   │   │                   │   └── RetentionTarget.java
│   │   │   │                   ├── web/
│   │   │   │                   │   ├── GdprController.java
│   │   │   │                   │   ├── GdprExceptionHandler.java
│   │   │   │                   │   └── GdprSecurityWarning.java
│   │   │   │                   └── GdprProperties.java
│   │   │   └── resources/
│   │   │       ├── db/
│   │   │       │   ├── changelog/
│   │   │       │   │   └── spring-gdpr-changelog.xml
│   │   │       │   └── migration/
│   │   │       │       ├── V1__gdpr_audit_access.sql
│   │   │       │       ├── V2__gdpr_subject_key.sql
│   │   │       │       └── V3__gdpr_forgettable_payload.sql
│   │   │       └── META-INF/
│   │   │           ├── spring/
│   │   │           │   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│   │   │           └── additional-spring-configuration-metadata.json
│   │   └── test/
│   │       └── java/
│   │           └── com/
│   │               └── iambilotta/
│   │                   └── gdpr/
│   │                       └── starter/
│   │                           ├── access/
│   │                           │   ├── AccessExportServiceTest.java
│   │                           │   └── JdbcSubjectDataProviderTest.java
│   │                           ├── audit/
│   │                           │   ├── AsyncAuditSinkDecoratorTest.java
│   │                           │   ├── AuditSinkMetricsTest.java
│   │                           │   ├── JdbcAuditSinkPostgresIT.java
│   │                           │   ├── JdbcAuditSinkTest.java
│   │                           │   ├── LegalBasisMappingTest.java
│   │                           │   └── PersonalDataAccessAdvisorTest.java
│   │                           ├── autoconfig/
│   │                           │   ├── declfixture/
│   │                           │   │   ├── crypto/
│   │                           │   │   │   └── CryptoShreddedEntity.java
│   │                           │   │   ├── forgettable/
│   │                           │   │   │   └── ForgettableEntity.java
│   │                           │   │   └── legacy/
│   │                           │   │       └── DeleteEntity.java
│   │                           │   ├── DeclarativeErasureWiringTest.java
│   │                           │   └── GdprAutoConfigurationTest.java
│   │                           ├── erasure/
│   │                           │   ├── crypto/
│   │                           │   │   ├── AesGcmCryptoShredderTest.java
│   │                           │   │   └── JdbcSubjectKeyStoreTest.java
│   │                           │   ├── forgettable/
│   │                           │   │   ├── CompositeSubjectErasureHandlerTest.java
│   │                           │   │   ├── ForgettablePayloadErasureHandlerTest.java
│   │                           │   │   ├── ForgettablePayloadReferenceTest.java
│   │                           │   │   ├── ForgettablePayloadResolverTest.java
│   │                           │   │   ├── GdprPersonalDataStorageTest.java
│   │                           │   │   ├── InMemoryForgettablePayloadStoreTest.java
│   │                           │   │   └── JdbcForgettablePayloadStoreTest.java
│   │                           │   ├── CryptoShreddingErasureTest.java
│   │                           │   ├── ErasureListenerTest.java
│   │                           │   ├── ErasureServiceTest.java
│   │                           │   └── JdbcErasureHandlerTest.java
│   │                           ├── logging/
│   │                           │   ├── PiiMaskerTest.java
│   │                           │   └── PiiMaskingConverterTest.java
│   │                           ├── retention/
│   │                           │   ├── JdbcRetentionTargetTest.java
│   │                           │   └── RetentionSchedulerTest.java
│   │                           └── web/
│   │                               ├── GdprControllerTest.java
│   │                               └── GdprSecurityWarningTest.java
│   └── pom.xml
├── spring-gdpr-starter-test/
│   ├── _generated/
│   │   ├── config.md
│   │   ├── http-endpoints.md
│   │   ├── modules.md
│   │   ├── ports.md
│   │   ├── requirements-by-us.md
│   │   ├── requirements.json
│   │   ├── requirements.md
│   │   └── templates.md
│   ├── src/
│   │   ├── main/
│   │   │   └── java/
│   │   │       └── com/
│   │   │           └── iambilotta/
│   │   │               └── gdpr/
│   │   │                   └── demo/
│   │   │                       ├── Customer.java
│   │   │                       ├── CustomerErasureHandler.java
│   │   │                       ├── CustomerRepository.java
│   │   │                       └── DemoApplication.java
│   │   └── test/
│   │       └── java/
│   │           └── com/
│   │               └── iambilotta/
│   │                   └── gdpr/
│   │                       └── demo/
│   │                           └── GdprIntegrationTest.java
│   └── pom.xml
├── .editorconfig
├── .gitignore
├── CHANGELOG.md
├── CITATION.cff
├── CLAUDE.md
├── CODE_OF_CONDUCT.md
├── CONTRIBUTING.md
├── jitpack.yml
├── LICENSE
├── Makefile
├── mvnw
├── mvnw.cmd
├── pom.xml
├── README.md
├── SECURITY.md
├── SUPPORT.md
└── tracegate.toml
```
