# spring-gdpr

[![ci](https://github.com/iambilotta/spring-gdpr/actions/workflows/ci.yml/badge.svg)](https://github.com/iambilotta/spring-gdpr/actions/workflows/ci.yml)
[![codeql](https://github.com/iambilotta/spring-gdpr/actions/workflows/codeql.yml/badge.svg)](https://github.com/iambilotta/spring-gdpr/actions/workflows/codeql.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.iambilotta.gdpr/spring-gdpr-starter.svg?label=maven%20central)](https://central.sonatype.com/artifact/com.iambilotta.gdpr/spring-gdpr-starter)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-21%2B-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.5%2B-6db33f.svg)](https://spring.io/projects/spring-boot)

> **GDPR compliance, generated from your annotations.** Annotate your domain types once, get a queryable audit log, a right-to-erasure flow, retention enforcement, and a deterministic Article 35 DPIA + Article 30 ROPA at every build. Apache 2.0, no SaaS sign-up, your evidence stays on your infra.

```java
@GdprDataSubjects(categories = {"customer"})
@GdprLegalBasis(value = LawfulBasis.CONTRACT, article = "6(1)(b)")
@GdprRetention(period = "P5Y", strategy = Strategy.ANONYMIZE)
@GdprErasable(strategy = GdprErasable.Strategy.DELETE, subjectIdField = "id")
public class Customer {
  @GdprPersonalData(description = "primary email")        private String email;
  @GdprPersonalData(specialCategory = true)               private String healthCondition;
}
```

`mvn compile` → `dpia.md` + `ropa.csv` under `target/generated-sources/`. The same annotations drive the runtime audit log + erasure REST + retention scheduler.

---

## Quick links

[**60-second TL;DR**](#60-second-tldr) ·
[**Should I use this?**](#should-i-use-this) ·
[**5-minute path**](#5-minute-path) ·
[**Architecture**](#architecture) ·
[**Annotations**](#annotations) ·
[**Configuration**](#configuration) ·
[**Limitations**](#limitations-and-gotchas) ·
[**Quickstart example**](examples/quickstart-postgres/README.md)

---

## 60-second TL;DR

OneTrust, Vanta, Sprinto sell GDPR compliance as a SaaS dashboard. Plenty of teams already have the answer in their codebase, they just lack the wiring to expose it. `spring-gdpr` is the wiring:

- **Five annotations** on your entities. That is the whole API surface.
- **AOP advisor** captures every personal-data access at runtime, dispatched async to a queryable audit sink (SLF4J or JDBC).
- **REST endpoints** for Article 17 right-to-erasure and Article 15 right of access. Wire Spring Security around them, no auth shipped by default.
- **Build-time generators** emit `dpia.md` (Article 35) + `ropa.csv` (Article 30) at every `mvn compile`. Commit them. The diff at PR review is the most useful artifact you can show your DPO.
- **Refactoring-safe**: remove an annotation, the claim disappears too. Code review catches inconsistencies.

Output is Markdown + CSV. Your evidence is portable. Your compliance dossier is reproducible from a git SHA.

## Should I use this?

| Use it if | Skip it if |
|---|---|
| You run Spring Boot 3.5+ in regulated context (EU enterprise, healthcare-adjacent, public sector) | Your stack is not Spring Boot |
| Your DPO is part-time and wants a dossier they can read in an hour | You need a notarized PDF with a wet signature today |
| You already lived through a GDPR audit and watched the SaaS dashboard drift from the codebase | You are pre-revenue B2C with three engineers; a Markdown file is enough |
| You want evidence-as-code, reproducible from a git SHA | You want a SaaS dashboard with a no-code admin |
| You can wire Spring Security around the GDPR REST endpoints | You expect the library to ship authentication |

## 5-minute path

```bash
# 1. Add the starter (once on Maven Central, reservation pending)
# pom.xml:
#   <dependency>
#     <groupId>com.iambilotta.gdpr</groupId>
#     <artifactId>spring-gdpr-starter</artifactId>
#     <version>0.1.0</version>
#   </dependency>

# 2. Add the build-time generator on the compiler plugin's annotationProcessorPaths.
#    See the snippet in "Wire the build-time generator" below.

# 3. Apply the bundled audit-table migration (Flyway):
#    classpath:db/migration/V1__gdpr_audit_access.sql

# 4. Annotate one domain entity. mvn compile. Open
#    target/generated-sources/annotations/spring/gdpr/dpia.md
#    target/generated-sources/annotations/spring/gdpr/ropa.csv

# Minimum viable adopter path: 5 minutes plus the migration window your
# RDBMS team picks for V1__.
```

For an end-to-end runnable example with PostgreSQL via Docker Compose, Spring Security, and a complete walkthrough: see [`examples/quickstart-postgres/`](examples/quickstart-postgres/README.md).

## Architecture

```
        ┌─ build time ────────────────────────────┐    ┌─ runtime ─────────────────────────────────┐
        │                                          │    │                                            │
        │   @GdprDataSubjects                      │    │   @GdprPersonalData                        │
        │   @GdprLegalBasis      ──┐               │    │   on a method or field                     │
        │   @GdprRetention         │               │    │            │                               │
        │   @GdprErasable          ├─►  APT        │    │            ▼                               │
        │                          │   processor   │    │   PersonalDataAccessAdvisor (Spring AOP)   │
        │                          │               │    │            │                               │
        │                          ▼               │    │            ▼                               │
        │   target/generated-sources/              │    │   AsyncAuditSinkDecorator                  │
        │     ├── ropa.csv   (Art. 30)             │    │     bounded queue, drop-newest             │
        │     └── dpia.md    (Art. 35 scaffold)    │    │            │                               │
        │                                          │    │            ▼                               │
        │   commit them, ship them to your DPO     │    │   AuditSink (SLF4J | JDBC | custom)        │
        │                                          │    │            ▲                               │
        │                                          │    │            │  query for Art. 15 export     │
        │                                          │    │   GET  /gdpr/audit/access?subjectId=...    │
        │                                          │    │   DELETE /gdpr/erasure/{subjectId}         │
        │                                          │    │   @Scheduled retention sweep (Art. 5(1)(e))│
        └──────────────────────────────────────────┘    └────────────────────────────────────────────┘
```

Same annotations drive both halves. Build-time and runtime never disagree, because there is one source of truth: the code.

## Annotations

| Annotation | Target | What it does |
|---|---|---|
| `@GdprPersonalData` | type, method, field, parameter | Marks data as in-scope. AOP advisor logs every access. Set `specialCategory = true` for Article 9 / 10 data. |
| `@GdprDataSubjects` | type | Lists data-subject categories (Article 30). |
| `@GdprLegalBasis` | type, method | Declares lawful basis (Article 6 / 9 / 10). Build warns if missing on a ROPA record. |
| `@GdprRetention` | type | Retention period + strategy (delete / anonymize / pseudonymize), Article 5(1)(e). |
| `@GdprErasable` | type | Right-to-erasure participation (Article 17), with FK-safe ordering. |

For Article 9 special categories (health, biometric, religion, ...) and Article 10 (criminal convictions), set `specialBasis` / `criminalBasis` on `@GdprLegalBasis`. The audit log records a composite reference like `6(1)(b) + 9(2)(h)`.

## Wire the build-time generator

Annotation processors do not auto-discover. Pick one of two patterns.

**Recommended (explicit):**

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <annotationProcessorPaths>
      <path>
        <groupId>com.iambilotta.gdpr</groupId>
        <artifactId>spring-gdpr-processor</artifactId>
        <version>0.1.0</version>
      </path>
    </annotationProcessorPaths>
  </configuration>
</plugin>
```

**Implicit (via classpath dependency):**

```xml
<dependency>
  <groupId>com.iambilotta.gdpr</groupId>
  <artifactId>spring-gdpr-processor</artifactId>
  <version>0.1.0</version>
  <scope>provided</scope>
  <optional>true</optional>
</dependency>
```

Optional Maven plugin to fail CI if generators stop running:

```xml
<plugin>
  <groupId>com.iambilotta.gdpr</groupId>
  <artifactId>spring-gdpr-maven-plugin</artifactId>
  <version>0.1.0</version>
  <executions>
    <execution><goals><goal>verify</goal></goals></execution>
  </executions>
</plugin>
```

## Configuration

```yaml
spring:
  gdpr:
    enabled: true
    web:
      base-path: /gdpr                 # wire Spring Security around <base-path>/**
    audit:
      jdbc-enabled: true               # required for /gdpr/audit/access
      table: gdpr_audit_access
      auto-create-schema: false        # production: apply the bundled migration via Flyway
      async:
        enabled: true                  # async ON by default
        thread-count: 1
        queue-capacity: 1024
    retention:
      enabled: true
      cron: "0 0 3 * * *"
    erasure:
      rest-enabled: true
```

IDE autocomplete is wired: typing `spring.gdpr.` in IntelliJ / VSCode / Eclipse pulls property descriptions and defaults from the bundled `additional-spring-configuration-metadata.json`.

## Observability

When Micrometer is on the classpath (Spring Boot Actuator pulls it in), three gauges register automatically:

| Meter | Meaning | Alert when |
|---|---|---|
| `spring.gdpr.audit.submitted` | events delivered to the async worker | informational |
| `spring.gdpr.audit.dropped` | events dropped due to queue saturation | `rate(...[5m]) > 0` |
| `spring.gdpr.audit.failed` | events that reached the sink and the sink threw | `rate(...[5m]) > 0` |

A positive `dropped` rate means audit gaps under load; bump `queue-capacity` or shard your sink. A positive `failed` rate means downstream sink errors; check the WARN/ERROR lines under the `gdpr.audit` logger.

## Wiring with Spring Security

The starter does **not** add authentication. The `/gdpr/**` endpoints are sensitive (erasure deletes data, access export reveals subjects), so wire your security rules around them:

```java
@Configuration
@EnableWebSecurity
public class GdprSecurityConfig {

  @Bean
  SecurityFilterChain gdprFilterChain(HttpSecurity http) throws Exception {
    http
        .securityMatcher("/gdpr/**")
        .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("DPO"))
        .csrf(csrf -> csrf.ignoringRequestMatchers("/gdpr/erasure/**"))
        .httpBasic(Customizer.withDefaults());
    return http.build();
  }

  @Bean
  ActorResolver gdprActorResolver() {
    return () -> {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      return auth != null ? auth.getName() : "system";
    };
  }
}
```

Audit rows now record the actual authenticated principal instead of the default `"system"`.

## Limitations and gotchas

Honest list. Read before adopting.

- **Throughput ceiling**: default async queue is 1024 entries with 1 worker thread. Sustained traffic above ~10k personal-data accesses per second per pod will saturate and start dropping. Bump `queue-capacity` and `thread-count`, OR ship audit through SLF4J + log aggregator instead of JDBC.
- **Oracle / DB2**: bundled migration uses `BOOLEAN` and `CREATE INDEX IF NOT EXISTS`. Adapt the migration script for those engines. Auto-create dev shortcut catches the index failure and warns; the table creation itself will fail without an adapted DDL.
- **REST endpoints unauthenticated by default**: you MUST wire Spring Security around `${spring.gdpr.web.base-path}/**` before exposing the app. See [the quickstart example](examples/quickstart-postgres/README.md) for a working pattern.
- **Async-by-default trade-off**: audit gaps under saturation are observable (`dropped` counter, WARN logs) but real. If you require zero-loss audit, set `spring.gdpr.audit.async.enabled=false` and accept that sink failures will surface as 500s on the request thread.
- **No JDBC batching**: each event is a single `INSERT`. Adopters at >1k events/sec on JDBC should write a custom `AuditSink` bean that batches.
- **`subjectIdField` is dichiarativo**: the field annotated on `@GdprErasable` is documentation; the actual lookup is whatever your `ErasureHandler.erase(subjectId)` does. The default `SubjectIdResolver` looks for a parameter named `subjectId` (case-insensitive); methods named `findById(String id)` won't resolve. Override the bean to read from MDC, security context, or a tenant header.

## Anti-patterns we explicitly reject

1. We do **not** pretend to be a DPO substitute. Output is evidence-as-code, not legal advice.
2. We do **not** invent GDPR articles. Every reference points to an article that exists.
3. We are **not** a Logback wrapper. Without DPIA + ROPA generators it would be an audit logger, which is not what GDPR asks for.

## Module map

| Module | Purpose |
|---|---|
| `spring-gdpr-annotations` | The five annotations. Zero runtime deps. Safe to import from any module. |
| `spring-gdpr-starter` | Auto-configured AOP advisor, async sink, retention scheduler, REST endpoints, audit sinks. |
| `spring-gdpr-processor` | APT processor: writes `dpia.md` + `ropa.csv` to `target/generated-sources/annotations/spring/gdpr/`. |
| `spring-gdpr-maven-plugin` | CLI goals (`gdpr:dpia`, `gdpr:ropa`, `gdpr:verify`) for CI. |
| `spring-gdpr-starter-test` | Internal demo + integration suite (round-trip annotation -> audit -> erasure -> artifacts). |

## Common questions

**Do I have to use JDBC for the audit log?**
No. Default sink is SLF4J. Set `spring.gdpr.audit.jdbc-enabled=true` only if you need the Article 15 right-of-access endpoint to query historical events. Many teams ship audit through SLF4J to ELK / Loki / Datadog and query there.

**Can I plug my own audit sink?**
Yes. Declare a `@Bean AuditSink`. The starter's auto-config sees the user-supplied bean and skips its own. The async decorator can wrap your sink too if you reuse the autoconfig path.

**What happens to the audit log when the DB is down?**
With async ON (default): events queue up to `queue-capacity`, beyond that the `dropped` counter increments and a WARN logs. The request thread is never blocked. With async OFF: the sink failure is logged at ERROR by the advisor and the business method continues.

**Can I run `spring-gdpr` without Spring Boot?**
Not at v0.1. The autoconfig is Spring Boot 3.5+. Plain Spring Framework support is feasible but unscoped.

**Is the DPIA scaffold really submission-ready?**
No. Sections 1 and 2 (records of processing + access points) are populated mechanically. Sections 3-6 (necessity, risks, mitigation, DPO consultation) are intentionally empty. Those are human judgment calls. The processor fills the parts that are mechanical and leaves the parts that need a human empty, instead of producing a confidently wrong template.

## Roadmap

| Version | Scope | Effort |
|---|---|---|
| v0.1 | annotations, AOP advisor, async sink, REST, retention, DPIA + ROPA generator | shipping |
| v0.2 | consent management, Article 20 data portability export | 2-3 weekend |
| v0.3 | cross-border transfer Article 44+, SCC scaffold | 2 weekend |

## Project files

- [LICENSE](LICENSE) (Apache 2.0)
- [CHANGELOG.md](CHANGELOG.md)
- [CONTRIBUTING.md](CONTRIBUTING.md)
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) (Contributor Covenant 2.1)
- [SECURITY.md](SECURITY.md) (90-day disclosure window)
- [examples/quickstart-postgres/](examples/quickstart-postgres/README.md) (runnable end-to-end demo)

## About

Built by [Francesco Bilotta](https://iambilotta.com). Sister repo [spring-aiact](https://github.com/iambilotta/spring-aiact) covers the EU AI Act (Article 11 Technical File, Article 12 audit log, Article 47 Declaration of Conformity) on the same evidence-as-code foundation.

## License

Apache License 2.0. See [LICENSE](LICENSE).
