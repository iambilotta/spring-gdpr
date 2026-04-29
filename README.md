# spring-gdpr

GDPR compliance-by-annotation for Spring Boot. Annotate domain types, get an audit log of every personal-data access, an automatic right-to-erasure flow, retention enforcement, and a build-time DPIA + ROPA generator. Apache 2.0, no SaaS sign-up.

> Status: **planning / v0.1 in progress**, not yet on Maven Central. Naming verified clear on GitHub at 2026-04-29.

## Why

OneTrust, Vanta, Sprinto sell GDPR compliance as a SaaS dashboard. Plenty of teams already have the answer in their codebase, they just lack the wiring to expose it. `spring-gdpr` is the wiring:

- **Refactoring-safe**: removing `@GdprPersonalData` removes the claim. Code review catches inconsistencies.
- **Evidence-as-code**: the same annotations drive the AOP audit advisor at runtime AND the DPIA + ROPA generators at build time.
- **Spring-native ergonomics**: a DPO consultant reads the annotations and maps your data flows in one hour.
- **Zero vendor lock-in**: outputs are Markdown + CSV. Take them anywhere.

## Module map

| Module | Purpose |
|---|---|
| `spring-gdpr-annotations` | The five annotations. Zero runtime deps. Safe to import from any module. |
| `spring-gdpr-starter` | Auto-configured AOP advisor, retention scheduler, REST endpoints, audit sinks. |
| `spring-gdpr-processor` | APT processor: writes `dpia.md` + `ropa.csv` to `target/generated-sources/annotations/spring/gdpr/`. |
| `spring-gdpr-maven-plugin` | CLI goals (`gdpr:dpia`, `gdpr:ropa`, `gdpr:verify`) for CI. |
| `spring-gdpr-starter-test` | Runnable demo + integration suite (round-trip annotation -> audit -> erasure -> artifacts). |

## Annotations

| Annotation | Target | What it does |
|---|---|---|
| `@GdprPersonalData` | type, method, field, parameter | Marks data as in-scope. AOP advisor logs every access. |
| `@GdprDataSubjects` | type | Lists data-subject categories (Art. 30). |
| `@GdprLegalBasis` | type, method | Declares lawful basis (Art. 6 / Art. 9). Build warns if missing on a ROPA record. |
| `@GdprRetention` | type | Retention period + strategy (delete / anonymize / pseudonymize), Art. 5(1)(e). |
| `@GdprErasable` | type | Right-to-erasure participation (Art. 17), with FK-safe ordering. |

## Quickstart

Add the runtime starter:

```xml
<dependency>
  <groupId>com.iambilotta.gdpr</groupId>
  <artifactId>spring-gdpr-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

Wire the build-time processor as an annotation processor path:

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

(Optional) wire the maven plugin to fail the build when artifacts are missing:

```xml
<plugin>
  <groupId>com.iambilotta.gdpr</groupId>
  <artifactId>spring-gdpr-maven-plugin</artifactId>
  <version>0.1.0</version>
  <executions>
    <execution>
      <goals><goal>verify</goal></goals>
    </execution>
  </executions>
</plugin>
```

## Example 1: declare a customer entity (records of processing)

```java
@GdprDataSubjects(categories = {"customer"})
@GdprLegalBasis(value = LawfulBasis.CONTRACT, article = "6(1)(b)", note = "performance of sales contract")
@GdprRetention(period = "P5Y", strategy = Strategy.ANONYMIZE)
@GdprErasable(strategy = GdprErasable.Strategy.DELETE, subjectIdField = "id")
public class Customer {
  private String id;
  @GdprPersonalData(description = "primary email") private String email;
  @GdprPersonalData(specialCategory = true)        private String taxId;
  private Instant createdAt;
}
```

After `mvn compile`, you get a deterministic `dpia.md` + `ropa.csv` under `target/generated-sources/annotations/spring/gdpr/`. The DPIA splits records (entities) from access points (methods touching personal data).

## Example 2: register an `ErasureHandler`

```java
@Component
public class CustomerErasureHandler implements ErasureHandler {
  private final CustomerRepository repository;

  public CustomerErasureHandler(CustomerRepository repository) { this.repository = repository; }

  @Override public String entityType()         { return Customer.class.getName(); }
  @Override public GdprErasable.Strategy strategy() { return GdprErasable.Strategy.DELETE; }
  @Override public int erase(String subjectId) { return repository.eraseBySubjectId(subjectId); }
  @Override public int order()                 { return 100; }
}
```

`DELETE /gdpr/erasure/{subjectId}` runs every registered handler in `order()` ascending. Lower order = erased first (use to enforce FK-safe deletion: child tables before parent tables).

## Example 3: register a `RetentionTarget`

```java
@Component
public class CustomerRetentionTarget implements RetentionTarget {
  private final JdbcTemplate jdbc;

  public CustomerRetentionTarget(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  @Override public String entityType()           { return Customer.class.getName(); }
  @Override public Duration retentionPeriod()    { return Duration.ofDays(365 * 5L); }
  @Override public GdprRetention.Strategy strategy() { return GdprRetention.Strategy.ANONYMIZE; }

  @Override
  public long countDue(Instant cutoff) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM customers WHERE created_at < ? AND email IS NOT NULL",
        Long.class, Timestamp.from(cutoff));
  }

  @Override
  public long applyDue(Instant cutoff) {
    return jdbc.update(
        "UPDATE customers SET email = NULL, tax_id = NULL WHERE created_at < ?",
        Timestamp.from(cutoff));
  }
}
```

The starter's `RetentionScheduler` calls `applyDue()` on every cron tick (default daily at 03:00, override via `spring.gdpr.retention.cron`).

## REST endpoints

Mounted at the path configured via `spring.gdpr.web.base-path`, default `/gdpr`:

- `DELETE /gdpr/erasure/{subjectId}` runs every registered `ErasureHandler` in declared order
- `GET    /gdpr/audit/access?subjectId=&from=&to=` returns audit records (Art. 15 right of access)

## Configuration

```yaml
spring:
  gdpr:
    enabled: true
    web:
      base-path: /gdpr
    audit:
      jdbc-enabled: true
      table: gdpr_audit_access
    retention:
      enabled: true
      cron: "0 0 3 * * *"
    erasure:
      rest-enabled: true
```

## Wiring with Spring Security

The starter does **not** add authentication. The `/gdpr/**` endpoints are sensitive (erasure deletes data, access export reveals subjects), so wire your security rules around them. Example for Spring Security 6:

```java
@Configuration
@EnableWebSecurity
public class GdprSecurityConfig {

  @Bean
  SecurityFilterChain gdprFilterChain(HttpSecurity http) throws Exception {
    http
        .securityMatcher("/gdpr/**")
        .authorizeHttpRequests(auth -> auth
            .anyRequest().hasRole("DPO"))
        .csrf(csrf -> csrf.ignoringRequestMatchers("/gdpr/erasure/**"))
        .httpBasic(Customizer.withDefaults());
    return http;
  }
}
```

To resolve the actor name from the security context, replace the default `ActorResolver`:

```java
@Bean
ActorResolver gdprActorResolver() {
  return () -> {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null ? auth.getName() : "system";
  };
}
```

## Anti-patterns we explicitly reject

1. We do **not** pretend to be a DPO substitute. Output is evidence-as-code, not legal advice.
2. We do **not** invent GDPR articles. Every reference points to an article that exists.
3. We are **not** a Logback wrapper. Without DPIA + ROPA generators it would be an audit logger, which is not what GDPR asks for.

## Roadmap

| Version | Scope | Effort |
|---|---|---|
| v0.1 | annotations, AOP advisor, REST, retention, DPIA + ROPA generator | 4-5 weekend |
| v0.2 | consent management, Art. 20 data portability export | 2-3 weekend |
| v0.3 | cross-border transfer Art. 44+, SCC scaffold | 2 weekend |

## License

Apache License 2.0. See [LICENSE](LICENSE).

## About

Built by [Francesco Bilotta](https://iambilotta.com). Sister repo [spring-aiact](https://github.com/iambilotta/spring-aiact) covers the EU AI Act.
