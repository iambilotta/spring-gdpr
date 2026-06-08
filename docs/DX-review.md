# Developer-Experience Review — `spring-gdpr`

> Scope: is `spring-gdpr` designed **DX-first** (the "Laravel-like" bar: batteries-included,
> works out of the box, 5-minute getting-started, clear API, helpful errors)? What is good,
> what is improvable, and what to ship next.
>
> Method: read the front door (`README.md`), the autoconfiguration, the five annotations, the
> three SPIs (`ErasureHandler`, `RetentionTarget`, `SubjectDataProvider`), the runnable
> `examples/quickstart-postgres`, the config metadata, the log-redaction converter and the
> Art. 15 export, plus ADR-0001..0009. Then **actually followed the getting-started**:
> `./mvnw -DskipTests install` (exit 0) → `mvn compile` on the example (exit 0) →
> inspected the generated `ropa.csv` / `dpia.md`. Findings cite files.
>
> Reviewed at commit `278eab6` (main), with the IDENTITY/CONTACT/FINANCIAL taxonomy,
> log-redaction, Art. 15 export, and the (proposed) crypto-shredding ADR-0009 in place.

## Verdict in one line

`spring-gdpr` is **architecturally DX-first but documentation-lagging**: the *runtime* is
genuinely batteries-included (one autoconfig, sane defaults, fail-soft on misconfig, IDE
autocomplete), and the core promise — *annotate once, get ROPA + DPIA at every build* — works
out of the box exactly as advertised. But three of the most recently shipped capabilities
(log redaction, the Art. 15 structured export, the category taxonomy) are **invisible to a new
adopter**: they exist in code and javadoc but never reach the README or the one runnable
example. And one headline behaviour in the README (`207 Multi-Status` on partial erasure) is
**not implemented**. The gap is not the engine; it is the front door.

---

## DX Scorecard

| # | Dimension | Score | Strength | Gap |
|---|---|---|---|---|
| 1 | **Install** | 🟢 Good | One JitPack repo + one starter dep; build verified green (`install` exit 0). Dual-line table (Boot 3.5 → v1.1.0, Boot 4 → v2.0.0) is clear. | Three-step wiring (starter + processor `annotationProcessorPaths` + Flyway migration), not one. GroupId differs between the **README** (`com.github.iambilotta.spring-gdpr`, JitPack) and the **example pom** (`com.iambilotta.gdpr`, local-install) — both correct in context, but a copy-paste trap. |
| 2 | **Zero-config default** | 🟢 Good | Pure autoconfig (`GdprAutoConfiguration`), `matchIfMissing=true` everywhere, SLF4J sink by default so it runs with no DB. Misconfig is **fail-soft with a fix-it WARN**, not a crash (`jdbc-enabled=true` + no DataSource → falls back to `Slf4jAuditSink` and logs *exactly* what to add). `@ConditionalOnMissingBean` on every bean = clean override. | Two "defaults" silently undercut compliance: erasure REST and the `/gdpr/**` surface ship **open** (no auth) and the **actor defaults to the literal string `"system"`** until you override `ActorResolver` — so out-of-the-box audit rows have no real principal. Documented in Reality-check, but a fresh app is non-compliant-by-default, not compliant-by-default. |
| 3 | **API ergonomics** | 🟢 Good | Annotations are intuitive and self-documenting; the worked `Customer` example reads like prose. SPIs are tiny (`ErasureHandler` = 4 methods, `SubjectDataProvider` = one `@FunctionalInterface`). Typed `Class<?>` on the SPI (ADR-0006) makes a renamed entity a compile error, not a runtime surprise. | `@GdprErasable.subjectIdField` is **documentation-only** (ADR-0007) yet looks load-bearing — a real foot-gun; the actual lookup is a parameter literally named `subjectId`. The new `Category` enum stops at three values with no `OTHER`, so anything non-Identity/Contact/Financial is forced to `UNCATEGORISED`. |
| 4 | **Getting-started (5-min)** | 🟡 Mixed | **Verified**: `install` → example `compile` → `ropa.csv` + `dpia.md` generated, in seconds. The "30-second pitch" with the generated CSV/markdown right there is excellent. Runnable Postgres example with curl walkthrough is real and complete for *audit + erasure*. | Not 5 minutes to a *running* app: the quickstart needs Docker + Postgres + Flyway + a hand-written `SecurityConfig` before the first `/gdpr` call. No `:memory:`/H2 "hello compliance in 60s" path. The example README still says it depends on `0.1.0-SNAPSHOT` (stale; pom is `2.0.0`). |
| 5 | **Errors** | 🟡 Mixed | The autoconfig WARNs are model-grade: each says the symptom **and the fix** ("Add `spring-boot-starter-jdbc`…", "Configure `spring.datasource.*`…"). `PiiMasker` fails *closed* (masks on reflection error). Table name is whitelist-validated against injection. | No web error contract: `GdprController.erase` has **no** `@ExceptionHandler`/`@ControllerAdvice`, so a throwing `ErasureHandler` yields a raw 500 and a blank `subjectId` throws `IllegalArgumentException` → 500, not 400. The README's "returns **207 Multi-Status** if a handler partially failed" is **not implemented** (no 207/`MULTI_STATUS` anywhere in `spring-gdpr-starter/src/main`). Docs promise an error UX the code does not deliver. |
| 6 | **Docs** | 🟡 Mixed | README is genuinely strong: pitch, mermaid pipelines, annotation table, config table, JMH numbers, an honest **Reality-check** of what it does *not* do, 9 ADRs with real rationale. Adoptable without reading source — *for the v1 feature set*. | The **newest features have zero adopter docs**: log redaction (`%piimsg`) lives only in `PiiMaskingConverter` javadoc — no README section, no `logback-spring.xml` in the example; the Art. 15 `SubjectDataProvider` export has no README entry and **no provider registered in the example**; the `Category` taxonomy is documented in the annotation but unused in the demo, so `ropa.csv` ships an empty `categories` column. A reader of README+example would not know these three features exist. |
| 7 | **Batteries-included vs explicit-binding balance** | 🟢 Good | The contract is principled and consistent: the library owns *orchestration* (handler ordering, audit shape, async decorator, retention cron, reflection assembly) and the adopter owns *the lookup that only they can know* (which table holds the subject) via small SPIs. ADR-0004's "honesty contract" (export/erase exactly what providers return, never more) is the right call for a compliance tool. The fall-soft-to-SLF4J default means it *does something useful* with zero bindings. | The "explicit-binding" half is under-supported with **ready strategies**. There is no shipped `JdbcErasureHandler`/`JdbcRetentionTarget`/`JpaSubjectDataProvider` base class — every adopter writes the same boilerplate by hand. Laravel-like would ship the common adapter and let you opt out. |

Legend: 🟢 meets the DX-first bar · 🟡 partially · 🔴 misses.

**Aggregate:** 4× 🟢, 3× 🟡, 0× 🔴. A strong, honest library with a real engine and a polished
front door for its *v1 surface*; the friction is concentrated in (a) features that shipped
faster than their docs and (b) one over-promised error behaviour.

---

## Ranked improvements (most impactful first)

Each item: **what** · **why (DX impact)** · **where**.

### 1. Make the README tell the truth about partial-erasure errors — or implement the 207
- **What:** Either implement `207 Multi-Status` + a `4xx` for blank/unknown subject in
  `GdprController`, or delete the "returns `207 Multi-Status`" sentence from the README and
  document the *actual* contract (a throwing handler → 500, blank id → 500).
- **Why:** A documented behaviour that does not exist is the single worst DX bug — the adopter
  builds error handling around a promise that never fires, and discovers it in production. For
  a *compliance* tool, "what happens when erasure half-fails" is the first question a DPO asks.
- **Where:** `README.md` §"How right-to-erasure actually works" (the 207 claim, line ~208) vs
  `spring-gdpr-starter/.../web/GdprController.java` (no `@ExceptionHandler`) and
  `.../erasure/ErasureService.java` (no per-handler try/catch, no partial-status model).

### 2. Document + demo the three "invisible" features (log redaction, Art. 15 export, categories)
- **What:** Add a README section and an example wiring for each: a `logback-spring.xml` with the
  `%piimsg` `conversionRule` in `examples/quickstart-postgres`; a registered `SubjectDataProvider`
  bean + a `GET /gdpr/access/export` curl in the walkthrough; and `category = IDENTITY/…/FINANCIAL`
  on the demo `Customer` fields so `ropa.csv` actually shows a populated `categories` column.
- **Why:** Shipped-but-undocumented = shipped-for-nobody. A new adopter reading README + example
  (the 95% path) cannot discover features that only exist in class javadoc. This is the largest
  *adoption* lever: three real capabilities are currently dark.
- **Where:** `README.md` (no `piimsg`/`SubjectDataProvider`/redaction sections — verified by grep);
  `examples/quickstart-postgres/` (no `logback*.xml`, no `SubjectDataProvider` bean, `Customer`
  uses no `category`); `.../logging/PiiMaskingConverter.java` (the only place the wiring is shown).

### 3. Ship ready-made SPI base classes (the missing batteries)
- **What:** Provide `JdbcErasureHandler` (delete/anonymize by a configured table+column),
  a `JdbcRetentionTarget` (sweep rows older than cutoff), and a `JdbcSubjectDataProvider`, each
  driven by a couple of constructor args. Keep the raw SPI for the non-JDBC case.
- **Why:** Today *every* adopter hand-writes the same `repo.deleteBySubjectId` handler. Laravel-
  like means the common 80% is one line of config; the SPI is the escape hatch, not the only door.
  This is the difference between "batteries-included" and "bring-your-own-battery, here's the spec".
- **Where:** `.../erasure/ErasureHandler.java`, `.../retention/RetentionTarget.java`,
  `.../access/SubjectDataProvider.java` — all currently interface-only; example writes the impls by hand.

### 4. Close the secure-by-default gap (or make the insecurity loud at startup)
- **What:** When `erasure.rest-enabled=true` and **no** Spring Security filter chain matches the
  base-path, log a startup WARN ("`/gdpr/**` is exposed without authentication — erasure deletes
  data; wire a SecurityFilterChain. See README."). Optionally, default `ActorResolver` to throw/WARN
  rather than silently stamping `"system"` on every audit row.
- **Why:** A compliance library whose erasure endpoint is open-by-default and whose audit actor is
  a fake constant until you notice, inverts the expectation. Fail-soft is great for *DX*; here it
  fails-soft straight past *compliance*. A loud startup signal keeps DX (it still boots) while
  removing the silent foot-gun.
- **Where:** `GdprAutoConfiguration.WebConfig` (mounts `GdprController` with no auth awareness);
  `gdprActorResolver()` default `ActorResolver.fixed("system")`; README §"Wiring with Spring Security".

### 5. Add a genuine 60-second path (H2, no Docker, no Flyway)
- **What:** A second, smaller example (or an `application-h2.yml` profile in the existing one) that
  boots in-memory with `auto-create-schema=true`, no Docker, no security, and exercises one audited
  read + one erasure. README "Quick start" links it as the *first* try.
- **Why:** The current quickstart is a *production-shaped* demo (good!) but it is not a 5-minute
  first-contact. The "time to first green" is the strongest retention metric for an OSS lib; right
  now it's gated on Docker + Postgres + a hand-written SecurityConfig.
- **Where:** `examples/quickstart-postgres/` is the only example; `auto-create-schema` already exists
  in `GdprProperties.Audit` for exactly this dev path.

### 6. Disarm the `subjectIdField` foot-gun in the type system
- **What:** Rename it `subjectIdFieldDoc` / `documentedSubjectIdField`, or add a Javadoc `@deprecated`-
  style banner *in the worked README example* ("documentation only — see ADR-0007"), so the name
  stops implying runtime behaviour. Better: actually honour it in the default `SubjectIdResolver`.
- **Why:** A parameter named `subjectIdField` that does **not** drive the subject lookup is a classic
  least-surprise violation; ADR-0007 documents the surprise but the annotation surface still invites it.
- **Where:** `annotations/.../GdprErasable.java` (`subjectIdField`), `.../audit/SubjectIdResolver.java`
  (resolves by parameter literally named `subjectId`), ADR-0007.

### 7. Reconcile the example's stale coordinates and snapshot version
- **What:** Update `examples/quickstart-postgres/README.md` (says `0.1.0-SNAPSHOT`) to `2.0.0`, and
  add one line to the main README clarifying *why* the example pom uses `com.iambilotta.gdpr`
  (local install) while consumers use `com.github.iambilotta.spring-gdpr` (JitPack).
- **Why:** Small, but version drift in the getting-started erodes trust precisely where a new adopter
  is most literal (copy-pasting). The groupId difference, undocumented, reads like a bug.
- **Where:** `examples/quickstart-postgres/README.md` (60-second tour, "depends on 0.1.0-SNAPSHOT");
  example `pom.xml` `groupId` vs `README.md` JitPack coordinates.

### 8. (minor) Quiet the build warnings / pin native-access flags
- **What:** The `install` run emits restricted-method / `sun.misc.Unsafe` warnings (Maven 3.9 on JDK 25).
  Harmless, but noisy on a first build and reads as "is this broken?".
- **Why:** First-build noise is a confidence tax. A `.mvn/jvm.config` with `--enable-native-access`
  removes it.
- **Where:** build output of `./mvnw … install`; `.mvn/`.

---

## Top 3 DX wins to ship next

1. **Truth-in-README on erasure errors (improvement #1).** Highest trust-per-line-of-effort:
   either implement the 207 + 4xx contract or correct the README. A compliance tool cannot
   over-promise its failure behaviour.
2. **Surface the dark features (improvement #2).** One README section + one example wiring each
   for log-redaction, the Art. 15 export, and the category taxonomy. Turns three already-built
   capabilities from "exists in javadoc" into "adopters actually use them".
3. **Ship the JDBC SPI base classes (improvement #3).** This is the move that makes the library
   feel *Laravel-like* rather than *spec-with-hooks*: the common case becomes a constructor call,
   the SPI stays as the escape hatch. Biggest jump on the batteries-included axis.
