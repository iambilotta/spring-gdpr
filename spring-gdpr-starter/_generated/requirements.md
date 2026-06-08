# Requirements — spring-gdpr-starter

Auto-generated from test sources by tracegate. Do NOT edit by hand: edit the test javadoc / docstring instead and rerun. Single source of truth is the test code.

**Convention**: category from the test name (`*Test`=FR, `*NfrTest`=NFR, `*InvariantTest`=INV, `*ContractTest`=CON; Python file markers `*invariant*`/`*nfr*`/`*contract*` map the same way; Playwright E2E tests join as **E2E**). Spec from doc-comment tags `@spec.given` / `@spec.when` / `@spec.then` (plus optional `@spec.adr` / `@spec.us`). Tests without a complete spec are listed with `(spec missing)` so they're visible and lintable.

## Coverage

- Total tests scanned: **70**
- With complete spec javadoc: **35** (50%)
- FR: 70

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
