# Requirements — spring-gdpr-starter, grouped by User Story

Auto-generated companion to `requirements.md`. Tests link to a User Story via the javadoc tag `@spec.us=US-NNN-slug` (the slug points to a User Story defined in `PRODUCT.md`). Implementation-detail tests with no `@spec.us` are collected at the bottom; declared User Stories in PRODUCT.md with zero linked tests are listed as **not implemented yet**.

## Coverage

- Total tests scanned: **97**
- Tests linked to a User Story: **62**
- Tests without `@spec.us` (implementation detail): **35**
- User Stories declared in PRODUCT.md: **0**
- User Stories with at least one linked test: **0**
- User Stories declared but **not yet implemented**: **0**

## `REQ-GDPR-016`  _(unknown to PRODUCT.md)_

- `FR-erasure.CryptoShreddingErasure#droppingTheKeyRendersThePiiUnrecoverableWhileTheEventStaysImmutable`
  - **Then**: the PII is no longer recoverable from the raw payload, while the event bytes are unchanged
- `FR-erasure.CryptoShreddingErasure#erasureIsPerSubjectAndDoesNotAffectOtherSubjects`
  - **Then**: the other subject's data is still fully recoverable (per-subject key granularity)
- `FR-erasure.CryptoShreddingErasure#erasurePreservesTheAuditTrailAsARecordedFact`
  - **Then**: the key is dropped AND an audit record of the erasure is preserved (who/when/why)
- `FR-erasure.CryptoShreddingErasure#replayAfterErasureIsIdempotentAndExposesNoPii`
  - **Then**: both replays yield the same read model with the PII blank (idempotent under replay)
- `FR-erasure.crypto.AesGcmCryptoShredder#cannotDecryptUnderAnotherSubjectsKey`
  - **Then**: decryption fails closed (empty): keys are not interchangeable across subjects
- `FR-erasure.crypto.AesGcmCryptoShredder#failsClosedOnMalformedInput`
  - **Then**: it returns empty instead of throwing (fail closed on malformed input)
- `FR-erasure.crypto.AesGcmCryptoShredder#failsClosedOnTamperedCiphertext`
  - **Then**: decryption fails closed (empty), never a partial plaintext (GCM authentication)
- `FR-erasure.crypto.AesGcmCryptoShredder#usesAFreshIvPerRecordSoCiphertextsDiffer`
  - **Then**: they differ (fresh random IV per record) yet both decrypt to the plaintext
- `FR-erasure.crypto.JdbcSubjectKeyStore#dropIsPerSubject`
  - **Then**: the other's key is untouched (per-subject granularity at the store level)
- `FR-erasure.crypto.JdbcSubjectKeyStore#dropOfNeverMintedSubjectStillTombstones`
  - **Then**: a tombstone is written so the subject can never be minted afterwards
- `FR-erasure.crypto.JdbcSubjectKeyStore#dropRemovesTheKey`
  - **Then**: keyFor is empty, exists is false, and the bytes are gone
- `FR-erasure.crypto.JdbcSubjectKeyStore#erasedSubjectCannotBeReMinted`
  - **Then**: it refuses to mint a new key (the tombstone prevents un-erasure)
- `FR-erasure.crypto.JdbcSubjectKeyStore#mintsAndReadsBackAStableKey`
  - **Then**: getOrCreate is idempotent and keyFor returns the same key bytes

## `REQ-GDPR-018`  _(unknown to PRODUCT.md)_

- `FR-logging.PiiMasker#classifiedFieldValuesNeverAppearInClearText`
  - **Then**: neither personal-data value appears in clear text in the output
- `FR-logging.PiiMasker#nonPersonalFieldsArePreserved`
  - **Then**: the non-personal value is preserved (only PII is masked, not the whole object)
- `FR-logging.PiiMasker#reportsWhenAnObjectCarriesNoPersonalData`
  - **Then**: it reports false, so the converter can skip the reflection cost
- `FR-logging.PiiMaskingConverter#rendersAMaskedMessageInTheLogOutput`
  - **Then**: the captured log line masks the PII value but keeps the non-personal field

## `REQ-GDPR-019`  _(unknown to PRODUCT.md)_

- `FR-access.AccessExportService#carriesTheCategoryOfEachExportedField`
  - **Then**: the declared category travels with the field (so the dossier is grouped by category)
- `FR-access.AccessExportService#doesNotExportNonPersonalFields`
  - **Then**: the non-personal field is NOT included (Art. 15 covers personal data only)
- `FR-access.AccessExportService#exportsTheClassifiedFieldsOfTheSubjectsObjects`
  - **Then**: the export carries one entry per classified field, with name, value and category
- `FR-access.AccessExportService#returnsAnEmptyExportForAnUnknownSubject`
  - **Then**: an empty export is returned for that subject id (not null, not an error)
- `FR-autoconfig.GdprAutoConfiguration#wiresTheAccessExportService`
  - **Then**: an AccessExportService bean is wired (Art. 15 export, REQ-GDPR-019), even with no SubjectDataProvider registered (it just exports nothing)

## `REQ-GDPR-022`  _(unknown to PRODUCT.md)_

- `FR-erasure.forgettable.CompositeSubjectErasureHandler#erasesBothTheForgettablePayloadAndTheCryptoKey`
  - **Then**: the external value is deleted AND the crypto key is dropped (both paths erased)
- `FR-erasure.forgettable.CompositeSubjectErasureHandler#rejectsAnEmptyDelegateList`
  - **Then**: construction is rejected (a composite that erases nothing is a silent compliance hole)
- `FR-erasure.forgettable.CompositeSubjectErasureHandler#sumsAffectedCountsAcrossDelegates`
  - **Then**: it sums the affected counts and reports the DELETE strategy
- `FR-erasure.forgettable.ForgettablePayloadErasureHandler#erasedSubjectStaysErased`
  - **Then**: the store refuses it (no silent resurrection through the erasure path)
- `FR-erasure.forgettable.ForgettablePayloadErasureHandler#erasureDeletesThePiiWhileTheCarrierKeepsOnlyADanglingReference`
  - **Then**: the value is gone (resolve empty) while the carrier's reference is unchanged
- `FR-erasure.forgettable.ForgettablePayloadErasureHandler#recordsTheErasureAsAnAuditFact`
  - **Then**: an audit record (action ERASURE, basis Art.17, actor) is written: a recorded fact
- `FR-erasure.forgettable.ForgettablePayloadErasureHandler#rejectsBlankSubjectId`
  - **Then**: it is rejected (a blank id is never a real erasure target)
- `FR-erasure.forgettable.ForgettablePayloadErasureHandler#reportsDeletedRowCountAndDeleteStrategy`
  - **Then**: it reports the number of deleted value rows and uses the DELETE strategy
- `FR-erasure.forgettable.ForgettablePayloadReference#rejectsBlankCoordinates`
  - **Then**: construction is rejected (a reference must always point somewhere)
- `FR-erasure.forgettable.ForgettablePayloadReference#rejectsMalformedUrn`
  - **Then**: parsing is rejected rather than producing a half-built reference
- `FR-erasure.forgettable.ForgettablePayloadReference#roundTripsThroughItsUrn`
  - **Then**: the URN is stable and round-trips back to the same reference
- `FR-erasure.forgettable.ForgettablePayloadResolver#requireThrowsOnAMissingValue`
  - **Then**: the resolver throws (a missing value is never silently treated as present)
- `FR-erasure.forgettable.ForgettablePayloadResolver#requireThrowsOnAnErasedValueRatherThanFakingOne`
  - **Then**: the resolver throws a typed not-available error instead of a placeholder
- `FR-erasure.forgettable.ForgettablePayloadResolver#resolvesAReferenceToItsStoredValue`
  - **Then**: the resolver returns the value from the external store
- `FR-erasure.forgettable.ForgettablePayloadResolver#resolvesAnErasedReferenceToEmpty`
  - **Then**: the resolver returns empty (the dangling reference exposes no PII)
- `FR-erasure.forgettable.GdprPersonalDataStorage#carriesTheForgettablePayloadStorageWhenDeclared`
  - **Then**: it reports FORGETTABLE_PAYLOAD (the field routes to the external store + primary erasure)
- `FR-erasure.forgettable.GdprPersonalDataStorage#defaultsToInlineForBackwardCompatibility`
  - **Then**: it defaults to INLINE (every pre-existing annotation stays valid)
- `FR-erasure.forgettable.InMemoryForgettablePayloadStore#eraseDeletesAndTombstones`
  - **Then**: values resolve empty and the put is refused (tombstone, no resurrection)
- `FR-erasure.forgettable.InMemoryForgettablePayloadStore#putsResolvesAndMissesEmpty`
  - **Then**: resolve returns it; a missing field resolves empty (fail-closed)
- `FR-erasure.forgettable.JdbcForgettablePayloadStore#eraseDeletesEveryFieldForTheSubject`
  - **Then**: every field for that subject resolves to empty (actual deletion of the PII)
- `FR-erasure.forgettable.JdbcForgettablePayloadStore#eraseIsPerSubject`
  - **Then**: the other's values are untouched (per-subject erasure granularity)
- `FR-erasure.forgettable.JdbcForgettablePayloadStore#eraseOfNeverSeenSubjectStillTombstones`
  - **Then**: a tombstone is recorded so the subject can never be populated afterwards
- `FR-erasure.forgettable.JdbcForgettablePayloadStore#erasedSubjectCannotHaveAPayloadReWritten`
  - **Then**: the put is refused (the tombstone forbids silently re-creating an erased subject)
- `FR-erasure.forgettable.JdbcForgettablePayloadStore#putIsAnUpsert`
  - **Then**: resolve returns the latest value (last-writer-wins upsert)
- `FR-erasure.forgettable.JdbcForgettablePayloadStore#putsAndResolvesAValue`
  - **Then**: resolve returns the stored value (the externalised PII lives only here)
- `FR-erasure.forgettable.JdbcForgettablePayloadStore#rejectsTableNamesThatLookLikeSqlInjection`
  - **Then**: construction fails fast before any SQL is built (injection-safe)
- `FR-erasure.forgettable.JdbcForgettablePayloadStore#resolveOfMissingFieldIsEmpty`
  - **Then**: resolve returns empty (fail-closed: a missing value is never a partial/placeholder)

## `US-DX-001-honest-erasure-error-contract`  _(unknown to PRODUCT.md)_

- `FR-web.GdprController#blankSubjectIdOnExportIs400NotRaw500`
  - **Then**: the request is rejected with 400 Bad Request, not a raw 500
- `FR-web.GdprController#throwingHandlerSurfacesAsServerErrorNot207`
  - **Then**: the handler exception propagates (fail-fast), never swallowed into a 207 success

## `US-DX-002-art15-export-endpoint`  _(unknown to PRODUCT.md)_

- `FR-access.JdbcSubjectDataProvider#feedsTheAccessExportWithClassifiedFields`
  - **Then**: the dossier carries the classified field value from the mapped row
- `FR-web.GdprController#exposesTheArticle15AccessExportAsAnEndpoint`
  - **Then**: the Article 15 dossier is returned as 200 with the subject's classified fields

## `US-DX-003-jdbc-spi-base-classes`  _(unknown to PRODUCT.md)_

- `FR-access.JdbcSubjectDataProvider#rejectsNonIdentifierNames`
  - **Then**: construction fails: identifiers are whitelist-validated
- `FR-access.JdbcSubjectDataProvider#returnsTheSubjectsRowsMappedToPersonalDataObjects`
  - **Then**: it returns the mapped object(s) for that subject only
- `FR-erasure.JdbcErasureHandler#deletesRowsMatchingTheSubjectIdColumnAndReturnsTheCount`
  - **Then**: it returns the affected-row count and only the subject's rows are gone
- `FR-erasure.JdbcErasureHandler#exposesTheTypedEntityClassAndStrategy`
  - **Then**: it reports the typed entity class and configured strategy (used in the audit row)
- `FR-erasure.JdbcErasureHandler#rejectsNonIdentifierTableOrColumnNames`
  - **Then**: construction fails: identifiers are whitelist-validated, not interpolated blindly
- `FR-retention.JdbcRetentionTarget#anonymizesPersonalColumnsOnDueRowsInsteadOfDeleting`
  - **Then**: the old rows stay but their personal-data column is nulled; the fresh row is untouched
- `FR-retention.JdbcRetentionTarget#deletesRowsOlderThanCutoffAndCountsThem`
  - **Then**: countDue and applyDue both report two and only the fresh row survives
- `FR-retention.JdbcRetentionTarget#rejectsNonIdentifierNames`
  - **Then**: construction fails: every identifier is whitelist-validated

## `US-DX-004-secure-by-default-signal`  _(unknown to PRODUCT.md)_

- `FR-web.GdprSecurityWarning#warnsThatTheGdprSurfaceIsOpenByDefault`
  - **Then**: a single WARN names the open base path and tells the adopter to wire Spring Security

## Implementation detail (no `@spec.us` link)

These tests are valid requirements but exist below the user-story horizon (unit-level mechanism, internal invariant, white-box assertion). Add `@spec.us` if a user story should claim them.

### Module `audit`

- `FR-audit.AsyncAuditSinkDecorator#findBySubjectDelegatesSynchronously`
- `FR-audit.AsyncAuditSinkDecorator#rejectsInvalidThreadCount`
- `FR-audit.AsyncAuditSinkDecorator#saturatedQueueDropsNewestAndCounts`
- `FR-audit.AsyncAuditSinkDecorator#sinkFailureIsAbsorbedAndCounted`
- `FR-audit.AsyncAuditSinkDecorator#writesAreDispatchedToWorkerThread`
- `FR-audit.AuditSinkMetrics#registersThreeGauges`
- `FR-audit.AuditSinkMetrics#submittedGaugeReflectsLiveCount`
- `FR-audit.JdbcAuditSink#failsFastWhenTableMissingAndAutoCreateOff`
- `FR-audit.JdbcAuditSink#filtersByTimeWindow`
- `FR-audit.JdbcAuditSink#rejectsTableNamesThatLookLikeSqlInjection`
- `FR-audit.JdbcAuditSink#schemaIsIdempotent`
- `FR-audit.JdbcAuditSink#worksWhenTableExistsAndAutoCreateOff`
- `FR-audit.JdbcAuditSink#writeAndQueryRoundTrip`
- `FR-audit.JdbcAuditSinkPostgres#failsFastWhenTableMissingAndAutoCreateOff`
- `FR-audit.JdbcAuditSinkPostgres#writesAndReadsAgainstMigrationApplied`
- `FR-audit.LegalBasisMapping#criminalConvictionsProducesArt10Reference`
- `FR-audit.LegalBasisMapping#explicitArticleOverrideWins`
- `FR-audit.LegalBasisMapping#nullAnnotationReturnsNull`
- `FR-audit.LegalBasisMapping#ordinaryDataReturnsArticle6Reference`
- `FR-audit.LegalBasisMapping#specialCategoryWithArt9ProducesCompositeReference`
- `FR-audit.LegalBasisMapping#specialCategoryWithoutArt9OrArt10FlagsMissing`
- `FR-audit.PersonalDataAccessAdvisor#deliveriesReachTheSink`
- `FR-audit.PersonalDataAccessAdvisor#sinkFailureIsAbsorbedNotPropagated`

### Module `autoconfig`

- `FR-autoconfig.GdprAutoConfiguration#defaultsToSlf4jSinkWhenJdbcDisabled`
- `FR-autoconfig.GdprAutoConfiguration#disablesEverythingWhenSpringGdprEnabledFalse`
- `FR-autoconfig.GdprAutoConfiguration#fallsBackToSlf4jWhenJdbcEnabledButNoDataSource`
- `FR-autoconfig.GdprAutoConfiguration#respectsUserProvidedAuditSinkBean`
- `FR-autoconfig.GdprAutoConfiguration#usesJdbcSinkWhenEnabledAndDataSourcePresent`
- `FR-autoconfig.GdprAutoConfiguration#wrapsSinkInAsyncDecoratorByDefault`

### Module `erasure`

- `FR-erasure.ErasureService#aggregatesAffectedCountsByType`
- `FR-erasure.ErasureService#invokesHandlersInOrderAscending`
- `FR-erasure.ErasureService#rejectsBlankSubjectId`

### Module `retention`

- `FR-retention.RetentionScheduler#runWithOffsetUsesCallerProvidedDuration`
- `FR-retention.RetentionScheduler#sweepAppliesEachTargetWithCutoffDerivedFromInjectedClock`
- `FR-retention.RetentionScheduler#targetCountReflectsRegisteredTargets`
