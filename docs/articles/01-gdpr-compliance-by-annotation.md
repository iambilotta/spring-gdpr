# GDPR compliance-by-annotation in Spring Boot: 5 weeks to evidence-as-code

> Draft for iambilotta.com, target publish: July 2026, alongside v0.1 of `spring-gdpr` on Maven Central.

GDPR compliance, as sold by Vanta or Sprinto, is a SaaS dashboard. You pay €15-30k per year for a tool that surveys your stack, asks you to fill in forms, and produces a Markdown DPIA you could have written yourself.

The information that should drive that DPIA already lives in your codebase: which entities hold personal data, on what legal basis, with what retention. The problem is not knowledge. The problem is plumbing. Every team retypes that knowledge into a SaaS form, then watches it drift away from the code three releases later.

`spring-gdpr` removes the form. You annotate the entity. The annotations drive the audit log at runtime, the right-to-erasure flow, the retention enforcement, and a build-time DPIA + ROPA generator. If you remove an annotation, the claim disappears too. Code review catches inconsistencies. The compliance dossier is reproducible from a git SHA.

## What the five annotations declare

```java
@GdprDataSubjects(categories = {"customer"})
@GdprLegalBasis(value = LawfulBasis.CONTRACT, article = "6(1)(b)")
@GdprRetention(period = "P5Y", strategy = Strategy.ANONYMIZE)
@GdprErasable(strategy = GdprErasable.Strategy.DELETE, subjectIdField = "id")
public class Customer {
  private String id;
  @GdprPersonalData(description = "primary email") private String email;
  @GdprPersonalData(specialCategory = true)        private String taxId;
  private Instant createdAt;
}
```

Five annotations cover the Art. 30 row in full: data subjects, lawful basis, retention period, erasure participation, and the per-field personal-data flag (which doubles as the AOP audit point). The `specialCategory` flag promotes a field to Art. 9 / Art. 10 territory, which the build-time generator surfaces as elevated risk in the DPIA scaffold.

## Three forces from one source of truth

### 1. AOP audit advisor (runtime, Art. 12 + Art. 30(1)(g))

Every method whose pointcut matches `@GdprPersonalData` (on the method, the declaring class, or any parameter) fires a structured access record. The default sink is SLF4J. Flip a flag and you get a JdbcTemplate-backed audit table with bootstrap-on-first-use schema.

```yaml
spring:
  gdpr:
    audit:
      jdbc-enabled: true
      table: gdpr_audit_access
```

The record carries `eventId`, `actor` (resolved from the security context), `subjectId` (extracted from method args), `targetType`, `targetMember`, `legalBasis`, `specialCategory`. Five seconds of cardiology, not a six-week SaaS rollout.

### 2. Right-to-erasure flow (runtime, Art. 17)

```http
DELETE /gdpr/erasure/{subjectId}
```

Spring discovers every `ErasureHandler` bean in the context, sorts by `order()`, runs them, and aggregates affected counts per entity. FK-safe ordering is your responsibility (lower order = erased first), but the contract is small enough that you do not need a dependency-graph library to enforce it.

### 3. Retention enforcement (scheduled, Art. 5(1)(e))

`RetentionTarget` is the SPI: tell the starter your `Duration`, your `Strategy`, and how to count + apply. The default scheduler runs at 03:00 daily, applies each target's policy to records past their cutoff, and emits a structured log line. Tests inject a `Clock`, no `Thread.sleep`.

## What the build does for you

`mvn compile` runs `spring-gdpr-processor` and writes two files under `target/generated-sources/annotations/spring/gdpr/`:

- `ropa.csv`: one row per ROPA-record entity (Art. 30(1) line items)
- `dpia.md`: a scaffold that splits records from access points, lists special-category fields, and leaves the necessity, risk, mitigation, and DPO-consultation sections empty for you to fill in

```text
| Entity                              | Data subjects | Legal basis | Retention | Strategy   | Special category |
|-------------------------------------|---------------|-------------|-----------|------------|------------------|
| com.iambilotta.gdpr.demo.Customer   | customer      | 6(1)(b)     | P5Y       | ANONYMIZE  | yes              |
```

The `gdpr:verify` mojo fails the build if either file is missing. The `gdpr:dpia` and `gdpr:ropa` mojos copy them to a stable `target/spring-gdpr/` path so a release pipeline can attach them as evidence assets.

## What it explicitly does not do

It does not certify you. Only a notified body can. The output is evidence-as-code for the dossier, not a green checkmark.

It does not invent GDPR articles. Every reference points to an article that exists in the regulation: 5(1)(e), 6, 9, 10, 12, 15, 17, 30, 35.

It is not a Logback wrapper. An audit logger without a DPIA + ROPA generator is not a compliance tool, it is a logger with extra steps. The point is the round-trip from annotation to artifact.

## When this is the right tool

- You run Spring Boot in regulated context (EU enterprise, public sector, healthcare-adjacent) and your auditor wants a Records of Processing in less than a quarter.
- You are mid-market, your DPO is part-time, and you want to hand them a dossier they can read in an hour.
- You already lived through a GDPR audit and watched the SaaS dashboard drift from the code two releases after onboarding.

## When it is not

- You are pre-revenue B2C with three engineers. Use a Markdown file. Come back when you hire your first DPO.
- Your stack is not Spring Boot. (A Go sister project is on the roadmap; meanwhile, port the annotation set, the engineering surface is small.)

## Where this is going

v0.1 ships the annotations, the AOP advisor, the REST surface, the retention scheduler, and the DPIA + ROPA generator. v0.2 will add consent management and an Art. 20 data-portability export. v0.3 will tackle Art. 44+ cross-border transfer scaffolding (SCC templates).

It is Apache 2.0 from day one. If your org runs Spring Boot in regulated context and your DPO would like to read code instead of filling SaaS forms, [open an issue](https://github.com/iambilotta/spring-gdpr/issues) or send a PR.
