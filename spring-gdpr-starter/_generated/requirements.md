# Requirements — spring-gdpr-starter

Auto-generated from test sources by tracegate. Do NOT edit by hand: edit the test javadoc / docstring instead and rerun. Single source of truth is the test code.

**Convention**: category from the test name (`*Test`=FR, `*NfrTest`=NFR, `*InvariantTest`=INV, `*ContractTest`=CON; Python file markers `*invariant*`/`*nfr*`/`*contract*` map the same way; Playwright E2E tests join as **E2E**). Spec from doc-comment tags `@spec.given` / `@spec.when` / `@spec.then` (plus optional `@spec.adr` / `@spec.us`). Tests without a complete spec are listed with `(spec missing)` so they're visible and lintable.

## Coverage

- Total tests scanned: **107**
- With complete spec javadoc: **72** (67%)
- FR: 107

## Module `access`

### Functional Requirements

#### `FR-access.AccessExportService#carriesTheCategoryOfEachExportedField`

- **Given**: any subject's classified field
- **When**: the export is assembled
- **Then**: the declared category travels with the field (so the dossier is grouped by category)
- **User Story**: REQ-GDPR-019
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/access/AccessExportServiceTest.java`

#### `FR-access.AccessExportService#doesNotExportNonPersonalFields`

- **Given**: a non-classified field on the subject's object
- **When**: the export is assembled
- **Then**: the non-personal field is NOT included (Art. 15 covers personal data only)
- **User Story**: REQ-GDPR-019
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/access/AccessExportServiceTest.java`

#### `FR-access.AccessExportService#exportsTheClassifiedFieldsOfTheSubjectsObjects`

- **Given**: a provider returning a subject's Customer with two classified fields
- **When**: the access export is assembled for that subject
- **Then**: the export carries one entry per classified field, with name, value and category
- **User Story**: REQ-GDPR-019
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/access/AccessExportServiceTest.java`

#### `FR-access.AccessExportService#returnsAnEmptyExportForAnUnknownSubject`

- **Given**: a subject for whom no provider returns any object
- **When**: the export is assembled
- **Then**: an empty export is returned for that subject id (not null, not an error)
- **User Story**: REQ-GDPR-019
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/access/AccessExportServiceTest.java`

#### `FR-access.JdbcSubjectDataProvider#feedsTheAccessExportWithClassifiedFields`

- **Given**: the JDBC subject-data provider behind the AccessExportService
- **When**: an export is assembled for the subject
- **Then**: the dossier carries the classified field value from the mapped row
- **User Story**: US-DX-002-art15-export-endpoint
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/access/JdbcSubjectDataProviderTest.java`

#### `FR-access.JdbcSubjectDataProvider#rejectsNonIdentifierNames`

- **Given**: a SQL-injection payload in the table or subject-id column name
- **When**: a JDBC subject-data provider is constructed
- **Then**: construction fails: identifiers are whitelist-validated
- **User Story**: US-DX-003-jdbc-spi-base-classes
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/access/JdbcSubjectDataProviderTest.java`

#### `FR-access.JdbcSubjectDataProvider#returnsTheSubjectsRowsMappedToPersonalDataObjects`

- **Given**: a customers table with the subject's row
- **When**: the JDBC subject-data provider fetches by the subject-id column
- **Then**: it returns the mapped object(s) for that subject only
- **User Story**: US-DX-003-jdbc-spi-base-classes
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/access/JdbcSubjectDataProviderTest.java`


## Module `audit`

### Functional Requirements

#### `FR-audit.AsyncAuditSinkDecorator#findBySubjectDelegatesSynchronously`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/audit/AsyncAuditSinkDecoratorTest.java`

#### `FR-audit.AsyncAuditSinkDecorator#rejectsInvalidThreadCount`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/audit/AsyncAuditSinkDecoratorTest.java`

#### `FR-audit.AsyncAuditSinkDecorator#saturatedQueueDropsNewestAndCounts`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/audit/AsyncAuditSinkDecoratorTest.java`

#### `FR-audit.AsyncAuditSinkDecorator#sinkFailureIsAbsorbedAndCounted`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/audit/AsyncAuditSinkDecoratorTest.java`

#### `FR-audit.AsyncAuditSinkDecorator#writesAreDispatchedToWorkerThread`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/audit/AsyncAuditSinkDecoratorTest.java`

#### `FR-audit.AuditSinkMetrics#registersThreeGauges`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/audit/AuditSinkMetricsTest.java`

#### `FR-audit.AuditSinkMetrics#submittedGaugeReflectsLiveCount`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/audit/AuditSinkMetricsTest.java`

#### `FR-audit.JdbcAuditSink#failsFastWhenTableMissingAndAutoCreateOff`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/audit/JdbcAuditSinkTest.java`

#### `FR-audit.JdbcAuditSink#filtersByTimeWindow`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/audit/JdbcAuditSinkTest.java`

#### `FR-audit.JdbcAuditSink#rejectsTableNamesThatLookLikeSqlInjection`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/audit/JdbcAuditSinkTest.java`

#### `FR-audit.JdbcAuditSink#schemaIsIdempotent`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/audit/JdbcAuditSinkTest.java`

#### `FR-audit.JdbcAuditSink#worksWhenTableExistsAndAutoCreateOff`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/audit/JdbcAuditSinkTest.java`

#### `FR-audit.JdbcAuditSink#writeAndQueryRoundTrip`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/audit/JdbcAuditSinkTest.java`

#### `FR-audit.JdbcAuditSinkPostgres#failsFastWhenTableMissingAndAutoCreateOff`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/audit/JdbcAuditSinkPostgresIT.java`

#### `FR-audit.JdbcAuditSinkPostgres#writesAndReadsAgainstMigrationApplied`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/audit/JdbcAuditSinkPostgresIT.java`

#### `FR-audit.LegalBasisMapping#criminalConvictionsProducesArt10Reference`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/audit/LegalBasisMappingTest.java`

#### `FR-audit.LegalBasisMapping#explicitArticleOverrideWins`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/audit/LegalBasisMappingTest.java`

#### `FR-audit.LegalBasisMapping#nullAnnotationReturnsNull`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/audit/LegalBasisMappingTest.java`

#### `FR-audit.LegalBasisMapping#ordinaryDataReturnsArticle6Reference`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/audit/LegalBasisMappingTest.java`

#### `FR-audit.LegalBasisMapping#specialCategoryWithArt9ProducesCompositeReference`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/audit/LegalBasisMappingTest.java`

#### `FR-audit.LegalBasisMapping#specialCategoryWithoutArt9OrArt10FlagsMissing`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/audit/LegalBasisMappingTest.java`

#### `FR-audit.PersonalDataAccessAdvisor#deliveriesReachTheSink`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/audit/PersonalDataAccessAdvisorTest.java`

#### `FR-audit.PersonalDataAccessAdvisor#sinkFailureIsAbsorbedNotPropagated`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/audit/PersonalDataAccessAdvisorTest.java`


## Module `autoconfig`

### Functional Requirements

#### `FR-autoconfig.DeclarativeErasureWiring#autoWiresACryptoShreddingHandlerForACryptoShredAnnotatedType`

- **Given**: a type annotated @GdprErasable(strategy = CRYPTO_SHRED) and no hand-wired handler
- **When**: the context starts with that type's package as the scanned base
- **Then**: a CryptoShreddingErasureHandler for that type is auto-wired and visible to the ErasureService (issue #36, the declarative bridge to the ADR-0009 machinery)
- **ADR**: ADR-0009
- **User Story**: REQ-GDPR-024
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/autoconfig/DeclarativeErasureWiringTest.java`

#### `FR-autoconfig.DeclarativeErasureWiring#autoWiresAForgettablePayloadHandlerForAForgettableAnnotatedTypeWithItsOrder`

- **Given**: a type annotated @GdprErasable(strategy = FORGETTABLE, order = 30)
- **When**: the context starts with that type's package as the scanned base
- **Then**: a ForgettablePayloadErasureHandler is auto-wired with the declared order and is visible to the ErasureService (issue #36, the ADR-0010 primary path, declarative)
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-024
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/autoconfig/DeclarativeErasureWiringTest.java`

#### `FR-autoconfig.DeclarativeErasureWiring#contributesAnInMemoryForgettableStoreFallbackWhenNoDataSourceIsPresent`

- **Given**: a FORGETTABLE type with no DataSource
- **When**: the context starts
- **Then**: an in-memory ForgettablePayloadStore is contributed as the dev/test fallback
- **User Story**: REQ-GDPR-024
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/autoconfig/DeclarativeErasureWiringTest.java`

#### `FR-autoconfig.DeclarativeErasureWiring#contributesAnInMemoryKeyStoreFallbackWhenNoDataSourceIsPresent`

- **Given**: declaring CRYPTO_SHRED with no DataSource on the context
- **When**: the context starts
- **Then**: an in-memory SubjectKeyStore is contributed (dev/test fallback) and the wired handler runs end to end against it
- **User Story**: REQ-GDPR-024
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/autoconfig/DeclarativeErasureWiringTest.java`

#### `FR-autoconfig.DeclarativeErasureWiring#doesNotAutoWireAHandlerForTheLegacyDeleteStrategy`

- **Given**: a type annotated with the legacy strategy DELETE (the adopter owns the handler)
- **When**: the context starts with that type's package as the scanned base
- **Then**: no library handler is auto-wired for it: DELETE/ANONYMIZE/PSEUDONYMIZE stay the adopter's ErasureHandler (ADR-0004), only the append-only-safe strategies are wired
- **ADR**: ADR-0004
- **User Story**: REQ-GDPR-024
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/autoconfig/DeclarativeErasureWiringTest.java`

#### `FR-autoconfig.DeclarativeErasureWiring#theDefaultStoreBeansAreOverridable`

- **Given**: an adopter that declares their own SubjectKeyStore bean
- **When**: the context starts with a CRYPTO_SHRED type
- **Then**: the adopter's store wins (the default is @ConditionalOnMissingBean, overridable)
- **User Story**: REQ-GDPR-024
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/autoconfig/DeclarativeErasureWiringTest.java`

#### `FR-autoconfig.GdprAutoConfiguration#defaultsToSlf4jSinkWhenJdbcDisabled`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/autoconfig/GdprAutoConfigurationTest.java`

#### `FR-autoconfig.GdprAutoConfiguration#disablesEverythingWhenSpringGdprEnabledFalse`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/autoconfig/GdprAutoConfigurationTest.java`

#### `FR-autoconfig.GdprAutoConfiguration#fallsBackToSlf4jWhenJdbcEnabledButNoDataSource`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/autoconfig/GdprAutoConfigurationTest.java`

#### `FR-autoconfig.GdprAutoConfiguration#respectsUserProvidedAuditSinkBean`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/autoconfig/GdprAutoConfigurationTest.java`

#### `FR-autoconfig.GdprAutoConfiguration#usesJdbcSinkWhenEnabledAndDataSourcePresent`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/autoconfig/GdprAutoConfigurationTest.java`

#### `FR-autoconfig.GdprAutoConfiguration#wiresPostErasureListenerAndPublishesSubjectErasedEvent`

- **Given**: the GDPR autoconfiguration plus a registered ErasureListener and an
- **When**: the wired ErasureService erases a subject
- **Then**: both the SPI listener and the @EventListener fire once with the subject (issue #37, the post-erasure hook for event-sourced/CQRS rebuilds)
- **User Story**: REQ-GDPR-023
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/autoconfig/GdprAutoConfigurationTest.java`

#### `FR-autoconfig.GdprAutoConfiguration#wiresTheAccessExportService`

- **Given**: the GDPR autoconfiguration on the context
- **When**: the context starts
- **Then**: an AccessExportService bean is wired (Art. 15 export, REQ-GDPR-019), even with no SubjectDataProvider registered (it just exports nothing)
- **User Story**: REQ-GDPR-019
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/autoconfig/GdprAutoConfigurationTest.java`

#### `FR-autoconfig.GdprAutoConfiguration#wrapsSinkInAsyncDecoratorByDefault`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/autoconfig/GdprAutoConfigurationTest.java`


## Module `erasure`

### Functional Requirements

#### `FR-erasure.CryptoShreddingErasure#droppingTheKeyRendersThePiiUnrecoverableWhileTheEventStaysImmutable`

- **Given**: an event whose PII payload was encrypted under a subject's key
- **When**: the subject's key is dropped (the erasure act) and a replay re-reads the event
- **Then**: the PII is no longer recoverable from the raw payload, while the event bytes are unchanged
- **ADR**: ADR-0009
- **User Story**: REQ-GDPR-016
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/CryptoShreddingErasureTest.java`

#### `FR-erasure.CryptoShreddingErasure#erasureIsPerSubjectAndDoesNotAffectOtherSubjects`

- **Given**: two subjects, each with their own per-subject key
- **When**: one subject is erased (key dropped)
- **Then**: the other subject's data is still fully recoverable (per-subject key granularity)
- **ADR**: ADR-0009
- **User Story**: REQ-GDPR-016
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/CryptoShreddingErasureTest.java`

#### `FR-erasure.CryptoShreddingErasure#erasurePreservesTheAuditTrailAsARecordedFact`

- **Given**: a subject whose key still exists
- **When**: the erasure runs
- **Then**: the key is dropped AND an audit record of the erasure is preserved (who/when/why)
- **ADR**: ADR-0009
- **User Story**: REQ-GDPR-016
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/CryptoShreddingErasureTest.java`

#### `FR-erasure.CryptoShreddingErasure#replayAfterErasureIsIdempotentAndExposesNoPii`

- **Given**: a stream of events for a subject whose key was dropped
- **When**: the projection is rebuilt by replaying the stream twice
- **Then**: both replays yield the same read model with the PII blank (idempotent under replay)
- **ADR**: ADR-0009
- **User Story**: REQ-GDPR-016
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/CryptoShreddingErasureTest.java`

#### `FR-erasure.ErasureListener#invokesTheListenerOnceWithTheReportAfterErasure`

- **Given**: an ErasureService with one registered ErasureListener
- **When**: a subject is erased
- **Then**: the listener is invoked exactly once with the assembled report
- **User Story**: REQ-GDPR-023
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/ErasureListenerTest.java`

#### `FR-erasure.ErasureListener#isANoOpAndBackwardCompatibleWhenNoListenerIsRegistered`

- **Given**: an ErasureService with no listener registered (the legacy constructor)
- **When**: a subject is erased
- **Then**: the erasure still succeeds and returns its report (backward compatible no-op)
- **User Story**: REQ-GDPR-023
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/ErasureListenerTest.java`

#### `FR-erasure.ErasureListener#surfacesAListenerFailureWithoutSwallowingOrUnErasing`

- **Given**: an ErasureService with several listeners, one of which throws
- **When**: a subject is erased
- **Then**: the failure is surfaced (not swallowed) and the other listeners still ran: the erasure itself already happened, a listener fault never silently un-erases it
- **User Story**: REQ-GDPR-023
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/ErasureListenerTest.java`

#### `FR-erasure.ErasureService#aggregatesAffectedCountsByType`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/ErasureServiceTest.java`

#### `FR-erasure.ErasureService#invokesHandlersInOrderAscending`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/ErasureServiceTest.java`

#### `FR-erasure.ErasureService#rejectsBlankSubjectId`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/ErasureServiceTest.java`

#### `FR-erasure.JdbcErasureHandler#deletesRowsMatchingTheSubjectIdColumnAndReturnsTheCount`

- **Given**: a customers table with three rows, two owned by the subject
- **When**: the JDBC erasure handler deletes by the subject-id column
- **Then**: it returns the affected-row count and only the subject's rows are gone
- **User Story**: US-DX-003-jdbc-spi-base-classes
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/JdbcErasureHandlerTest.java`

#### `FR-erasure.JdbcErasureHandler#exposesTheTypedEntityClassAndStrategy`

- **Given**: a JDBC erasure handler built for a domain type
- **When**: the orchestrator reads its metadata
- **Then**: it reports the typed entity class and configured strategy (used in the audit row)
- **User Story**: US-DX-003-jdbc-spi-base-classes
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/JdbcErasureHandlerTest.java`

#### `FR-erasure.JdbcErasureHandler#rejectsNonIdentifierTableOrColumnNames`

- **Given**: a table or column name carrying a SQL-injection payload
- **When**: a JDBC erasure handler is constructed
- **Then**: construction fails: identifiers are whitelist-validated, not interpolated blindly
- **User Story**: US-DX-003-jdbc-spi-base-classes
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/JdbcErasureHandlerTest.java`


## Module `erasure.crypto`

### Functional Requirements

#### `FR-erasure.crypto.AesGcmCryptoShredder#cannotDecryptUnderAnotherSubjectsKey`

- **Given**: a ciphertext sealed under one subject's key
- **When**: it is decrypted under a different subject's key
- **Then**: decryption fails closed (empty): keys are not interchangeable across subjects
- **ADR**: ADR-0009
- **User Story**: REQ-GDPR-016
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/crypto/AesGcmCryptoShredderTest.java`

#### `FR-erasure.crypto.AesGcmCryptoShredder#failsClosedOnMalformedInput`

- **Given**: garbage bytes that are not a valid ciphertext
- **When**: decryption is attempted
- **Then**: it returns empty instead of throwing (fail closed on malformed input)
- **ADR**: ADR-0009
- **User Story**: REQ-GDPR-016
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/crypto/AesGcmCryptoShredderTest.java`

#### `FR-erasure.crypto.AesGcmCryptoShredder#failsClosedOnTamperedCiphertext`

- **Given**: a valid ciphertext
- **When**: a single byte of it is flipped (tampering) and decryption is attempted
- **Then**: decryption fails closed (empty), never a partial plaintext (GCM authentication)
- **ADR**: ADR-0009
- **User Story**: REQ-GDPR-016
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/crypto/AesGcmCryptoShredderTest.java`

#### `FR-erasure.crypto.AesGcmCryptoShredder#usesAFreshIvPerRecordSoCiphertextsDiffer`

- **Given**: a plaintext encrypted twice under the same subject key
- **When**: the two ciphertexts are compared
- **Then**: they differ (fresh random IV per record) yet both decrypt to the plaintext
- **ADR**: ADR-0009
- **User Story**: REQ-GDPR-016
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/crypto/AesGcmCryptoShredderTest.java`

#### `FR-erasure.crypto.JdbcSubjectKeyStore#dropIsPerSubject`

- **Given**: two subjects with keys
- **When**: one is erased
- **Then**: the other's key is untouched (per-subject granularity at the store level)
- **ADR**: ADR-0009
- **User Story**: REQ-GDPR-016
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/crypto/JdbcSubjectKeyStoreTest.java`

#### `FR-erasure.crypto.JdbcSubjectKeyStore#dropOfNeverMintedSubjectStillTombstones`

- **Given**: a subject that never had a key
- **When**: drop is called for it (idempotent erasure)
- **Then**: a tombstone is written so the subject can never be minted afterwards
- **ADR**: ADR-0009
- **User Story**: REQ-GDPR-016
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/crypto/JdbcSubjectKeyStoreTest.java`

#### `FR-erasure.crypto.JdbcSubjectKeyStore#dropRemovesTheKey`

- **Given**: a subject with a minted key
- **When**: the key is dropped (erasure)
- **Then**: keyFor is empty, exists is false, and the bytes are gone
- **ADR**: ADR-0009
- **User Story**: REQ-GDPR-016
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/crypto/JdbcSubjectKeyStoreTest.java`

#### `FR-erasure.crypto.JdbcSubjectKeyStore#erasedSubjectCannotBeReMinted`

- **Given**: a subject whose key was dropped (erased)
- **When**: getOrCreate is called again for the same subject
- **Then**: it refuses to mint a new key (the tombstone prevents un-erasure)
- **ADR**: ADR-0009
- **User Story**: REQ-GDPR-016
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/crypto/JdbcSubjectKeyStoreTest.java`

#### `FR-erasure.crypto.JdbcSubjectKeyStore#mintsAndReadsBackAStableKey`

- **Given**: a JDBC key store on a fresh schema
- **When**: a subject's key is minted then read back
- **Then**: getOrCreate is idempotent and keyFor returns the same key bytes
- **ADR**: ADR-0009
- **User Story**: REQ-GDPR-016
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/crypto/JdbcSubjectKeyStoreTest.java`


## Module `erasure.forgettable`

### Functional Requirements

#### `FR-erasure.forgettable.CompositeSubjectErasureHandler#erasesBothTheForgettablePayloadAndTheCryptoKey`

- **Given**: a subject whose PII lives in BOTH the forgettable-payload store and (for an immutable event) a crypto-shredded field
- **When**: the composite erasure handler runs once for that subject
- **Then**: the external value is deleted AND the crypto key is dropped (both paths erased)
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/CompositeSubjectErasureHandlerTest.java`

#### `FR-erasure.forgettable.CompositeSubjectErasureHandler#rejectsAnEmptyDelegateList`

- **Given**: a composite with no delegates
- **When**: it is constructed
- **Then**: construction is rejected (a composite that erases nothing is a silent compliance hole)
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/CompositeSubjectErasureHandlerTest.java`

#### `FR-erasure.forgettable.CompositeSubjectErasureHandler#sumsAffectedCountsAcrossDelegates`

- **Given**: two delegate handlers each affecting one row/key
- **When**: the composite erases a subject
- **Then**: it sums the affected counts and reports the DELETE strategy
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/CompositeSubjectErasureHandlerTest.java`

#### `FR-erasure.forgettable.ForgettablePayloadErasureHandler#erasedSubjectStaysErased`

- **Given**: a subject erased via the handler
- **When**: a put is attempted again for that subject
- **Then**: the store refuses it (no silent resurrection through the erasure path)
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/ForgettablePayloadErasureHandlerTest.java`

#### `FR-erasure.forgettable.ForgettablePayloadErasureHandler#erasureDeletesThePiiWhileTheCarrierKeepsOnlyADanglingReference`

- **Given**: a carrier that holds only a reference to an externalised PII field
- **When**: the value is resolved before erasure, then the subject is erased
- **Then**: the value is gone (resolve empty) while the carrier's reference is unchanged
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/ForgettablePayloadErasureHandlerTest.java`

#### `FR-erasure.forgettable.ForgettablePayloadErasureHandler#recordsTheErasureAsAnAuditFact`

- **Given**: an erasure of a subject
- **When**: the handler runs
- **Then**: an audit record (action ERASURE, basis Art.17, actor) is written: a recorded fact
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/ForgettablePayloadErasureHandlerTest.java`

#### `FR-erasure.forgettable.ForgettablePayloadErasureHandler#rejectsBlankSubjectId`

- **Given**: a blank subject id
- **When**: the handler is asked to erase
- **Then**: it is rejected (a blank id is never a real erasure target)
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/ForgettablePayloadErasureHandlerTest.java`

#### `FR-erasure.forgettable.ForgettablePayloadErasureHandler#reportsDeletedRowCountAndDeleteStrategy`

- **Given**: a subject with two externalised fields
- **When**: the erasure handler runs
- **Then**: it reports the number of deleted value rows and uses the DELETE strategy
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/ForgettablePayloadErasureHandlerTest.java`

#### `FR-erasure.forgettable.ForgettablePayloadReference#rejectsBlankCoordinates`

- **Given**: a blank subject id or a blank field key
- **When**: a reference is constructed
- **Then**: construction is rejected (a reference must always point somewhere)
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/ForgettablePayloadReferenceTest.java`

#### `FR-erasure.forgettable.ForgettablePayloadReference#rejectsMalformedUrn`

- **Given**: a malformed URN (wrong scheme or missing segments)
- **When**: it is parsed back into a reference
- **Then**: parsing is rejected rather than producing a half-built reference
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/ForgettablePayloadReferenceTest.java`

#### `FR-erasure.forgettable.ForgettablePayloadReference#roundTripsThroughItsUrn`

- **Given**: a subject id and a field key
- **When**: a reference is built and rendered as a URN
- **Then**: the URN is stable and round-trips back to the same reference
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/ForgettablePayloadReferenceTest.java`

#### `FR-erasure.forgettable.ForgettablePayloadResolver#requireThrowsOnAMissingValue`

- **Given**: a reference to a value that was never written
- **When**: the value is required
- **Then**: the resolver throws (a missing value is never silently treated as present)
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/ForgettablePayloadResolverTest.java`

#### `FR-erasure.forgettable.ForgettablePayloadResolver#requireThrowsOnAnErasedValueRatherThanFakingOne`

- **Given**: a reference to a value that was erased
- **When**: the value is required (not optional)
- **Then**: the resolver throws a typed not-available error instead of a placeholder
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/ForgettablePayloadResolverTest.java`

#### `FR-erasure.forgettable.ForgettablePayloadResolver#resolvesAReferenceToItsStoredValue`

- **Given**: a stored value referenced by a ForgettablePayloadReference
- **When**: the reference is resolved
- **Then**: the resolver returns the value from the external store
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/ForgettablePayloadResolverTest.java`

#### `FR-erasure.forgettable.ForgettablePayloadResolver#resolvesAnErasedReferenceToEmpty`

- **Given**: a reference whose subject was erased
- **When**: the reference is resolved
- **Then**: the resolver returns empty (the dangling reference exposes no PII)
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/ForgettablePayloadResolverTest.java`

#### `FR-erasure.forgettable.GdprPersonalDataStorage#carriesTheForgettablePayloadStorageWhenDeclared`

- **Given**: a field marked storage = FORGETTABLE_PAYLOAD
- **When**: the annotation's storage axis is read
- **Then**: it reports FORGETTABLE_PAYLOAD (the field routes to the external store + primary erasure)
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/GdprPersonalDataStorageTest.java`

#### `FR-erasure.forgettable.GdprPersonalDataStorage#defaultsToInlineForBackwardCompatibility`

- **Given**: a personal-data field with no storage declared
- **When**: the annotation's storage axis is read
- **Then**: it defaults to INLINE (every pre-existing annotation stays valid)
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/GdprPersonalDataStorageTest.java`

#### `FR-erasure.forgettable.InMemoryForgettablePayloadStore#eraseDeletesAndTombstones`

- **Given**: a subject with externalised values
- **When**: the subject is erased then a put is attempted again
- **Then**: values resolve empty and the put is refused (tombstone, no resurrection)
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/InMemoryForgettablePayloadStoreTest.java`

#### `FR-erasure.forgettable.InMemoryForgettablePayloadStore#putsResolvesAndMissesEmpty`

- **Given**: an in-memory payload store
- **When**: a value is put and resolved
- **Then**: resolve returns it; a missing field resolves empty (fail-closed)
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/InMemoryForgettablePayloadStoreTest.java`

#### `FR-erasure.forgettable.JdbcForgettablePayloadStore#eraseDeletesEveryFieldForTheSubject`

- **Given**: a subject with two externalised fields
- **When**: the subject is erased
- **Then**: every field for that subject resolves to empty (actual deletion of the PII)
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/JdbcForgettablePayloadStoreTest.java`

#### `FR-erasure.forgettable.JdbcForgettablePayloadStore#eraseIsPerSubject`

- **Given**: two subjects with externalised values
- **When**: one is erased
- **Then**: the other's values are untouched (per-subject erasure granularity)
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/JdbcForgettablePayloadStoreTest.java`

#### `FR-erasure.forgettable.JdbcForgettablePayloadStore#eraseOfNeverSeenSubjectStillTombstones`

- **Given**: a subject that never had any payload
- **When**: erase is called for it (idempotent erasure)
- **Then**: a tombstone is recorded so the subject can never be populated afterwards
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/JdbcForgettablePayloadStoreTest.java`

#### `FR-erasure.forgettable.JdbcForgettablePayloadStore#erasedSubjectCannotHaveAPayloadReWritten`

- **Given**: a subject that was erased (its rows deleted, a tombstone recorded)
- **When**: a new value is put for that same subject
- **Then**: the put is refused (the tombstone forbids silently re-creating an erased subject)
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/JdbcForgettablePayloadStoreTest.java`

#### `FR-erasure.forgettable.JdbcForgettablePayloadStore#putIsAnUpsert`

- **Given**: an existing value for (subject, field)
- **When**: a new value is put for the same coordinates (the store is mutable)
- **Then**: resolve returns the latest value (last-writer-wins upsert)
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/JdbcForgettablePayloadStoreTest.java`

#### `FR-erasure.forgettable.JdbcForgettablePayloadStore#putsAndResolvesAValue`

- **Given**: a JDBC payload store on a fresh schema
- **When**: a value is put for (subject, field) and resolved back
- **Then**: resolve returns the stored value (the externalised PII lives only here)
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/JdbcForgettablePayloadStoreTest.java`

#### `FR-erasure.forgettable.JdbcForgettablePayloadStore#rejectsTableNamesThatLookLikeSqlInjection`

- **Given**: a hostile table name that is not a bare SQL identifier
- **When**: the store is constructed with it
- **Then**: construction fails fast before any SQL is built (injection-safe)
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/JdbcForgettablePayloadStoreTest.java`

#### `FR-erasure.forgettable.JdbcForgettablePayloadStore#resolveOfMissingFieldIsEmpty`

- **Given**: a subject without a value for a given field
- **When**: that field is resolved
- **Then**: resolve returns empty (fail-closed: a missing value is never a partial/placeholder)
- **ADR**: ADR-0010
- **User Story**: REQ-GDPR-022
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/erasure/forgettable/JdbcForgettablePayloadStoreTest.java`


## Module `logging`

### Functional Requirements

#### `FR-logging.PiiMasker#classifiedFieldValuesNeverAppearInClearText`

- **Given**: an object whose name + email fields carry `@GdprPersonalData`
- **When**: the masker renders it
- **Then**: neither personal-data value appears in clear text in the output
- **User Story**: REQ-GDPR-018
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/logging/PiiMaskerTest.java`

#### `FR-logging.PiiMasker#nonPersonalFieldsArePreserved`

- **Given**: an object with a non-personal `id` field
- **When**: the masker renders it
- **Then**: the non-personal value is preserved (only PII is masked, not the whole object)
- **User Story**: REQ-GDPR-018
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/logging/PiiMaskerTest.java`

#### `FR-logging.PiiMasker#reportsWhenAnObjectCarriesNoPersonalData`

- **Given**: a value that carries no `@GdprPersonalData` fields at all
- **When**: the masker is asked whether it must mask it
- **Then**: it reports false, so the converter can skip the reflection cost
- **User Story**: REQ-GDPR-018
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/logging/PiiMaskerTest.java`

#### `FR-logging.PiiMaskingConverter#rendersAMaskedMessageInTheLogOutput`

- **Given**: a Logback logger whose pattern uses the registered %piimsg converter
- **When**: an object carrying a @GdprPersonalData field is logged as a message argument
- **Then**: the captured log line masks the PII value but keeps the non-personal field
- **User Story**: REQ-GDPR-018
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/logging/PiiMaskingConverterTest.java`


## Module `retention`

### Functional Requirements

#### `FR-retention.JdbcRetentionTarget#anonymizesPersonalColumnsOnDueRowsInsteadOfDeleting`

- **Given**: two rows older than the cutoff, with an ANONYMIZE strategy nulling full_name
- **When**: the JDBC retention target applies the due sweep
- **Then**: the old rows stay but their personal-data column is nulled; the fresh row is untouched
- **User Story**: US-DX-003-jdbc-spi-base-classes
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/retention/JdbcRetentionTargetTest.java`

#### `FR-retention.JdbcRetentionTarget#deletesRowsOlderThanCutoffAndCountsThem`

- **Given**: two rows older than the cutoff and one fresh, with a DELETE strategy
- **When**: the JDBC retention target counts then applies the due sweep
- **Then**: countDue and applyDue both report two and only the fresh row survives
- **User Story**: US-DX-003-jdbc-spi-base-classes
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/retention/JdbcRetentionTargetTest.java`

#### `FR-retention.JdbcRetentionTarget#rejectsNonIdentifierNames`

- **Given**: a SQL-injection payload in the table, timestamp, or personal-data column name
- **When**: a JDBC retention target is constructed
- **Then**: construction fails: every identifier is whitelist-validated
- **User Story**: US-DX-003-jdbc-spi-base-classes
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/retention/JdbcRetentionTargetTest.java`

#### `FR-retention.RetentionScheduler#runWithOffsetUsesCallerProvidedDuration`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/retention/RetentionSchedulerTest.java`

#### `FR-retention.RetentionScheduler#sweepAppliesEachTargetWithCutoffDerivedFromInjectedClock`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/retention/RetentionSchedulerTest.java`

#### `FR-retention.RetentionScheduler#targetCountReflectsRegisteredTargets`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/retention/RetentionSchedulerTest.java`


## Module `web`

### Functional Requirements

#### `FR-web.GdprController#blankSubjectIdOnExportIs400NotRaw500`

- **Given**: an access-export endpoint
- **When**: a client GETs the export with a blank subject id
- **Then**: the request is rejected with 400 Bad Request, not a raw 500
- **User Story**: US-DX-001-honest-erasure-error-contract
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/web/GdprControllerTest.java`

#### `FR-web.GdprController#exposesTheArticle15AccessExportAsAnEndpoint`

- **Given**: an access-export endpoint and a registered provider that returns a personal-data object
- **When**: a client GETs /gdpr/access/export with the subject id
- **Then**: the Article 15 dossier is returned as 200 with the subject's classified fields
- **User Story**: US-DX-002-art15-export-endpoint
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/web/GdprControllerTest.java`

#### `FR-web.GdprController#throwingHandlerSurfacesAsServerErrorNot207`

- **Given**: an erasure endpoint and a handler that throws while erasing
- **When**: a client DELETEs the subject
- **Then**: the handler exception propagates (fail-fast), never swallowed into a 207 success
- **User Story**: US-DX-001-honest-erasure-error-contract
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/web/GdprControllerTest.java`

#### `FR-web.GdprSecurityWarning#warnsThatTheGdprSurfaceIsOpenByDefault`

- **Given**: the GDPR web surface is about to be mounted at a base path
- **When**: the startup security warning runs
- **Then**: a single WARN names the open base path and tells the adopter to wire Spring Security
- **User Story**: US-DX-004-secure-by-default-signal
- **File**: `spring-gdpr-starter/src/test/java/com/iambilotta/gdpr/starter/web/GdprSecurityWarningTest.java`
