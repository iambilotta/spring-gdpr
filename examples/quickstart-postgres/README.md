# spring-gdpr quickstart (Postgres)

End-to-end runnable example. PostgreSQL via Docker Compose, Spring Boot 3.5 web app, Spring Security with two demo accounts, the spring-gdpr starter wired against a real JDBC backend, Flyway running both the gdpr audit migration AND the demo's own customer-table migration.

The shape is what a production-grade adopter ships:
- Audit table created via Flyway (production path, not auto-create).
- Async sink decorator on by default.
- `ActorResolver` overridden to read the Spring Security principal.
- DPO-role gate on `/gdpr/**`.
- `SubjectDataProvider` registered so the Article 15 access export returns real data.
- `Customer` fields tagged with `IDENTITY` / `CONTACT` / `FINANCIAL` categories (populates the `categories` column of `ropa.csv`).
- `logback-spring.xml` wiring the `%piimsg` redaction converter so a logged `Customer` masks its personal fields.

## What you need

- JDK 21+
- Maven 3.9+
- Docker (any recent version with the Compose plugin)

## 60-second tour

```bash
# 1) Build the spring-gdpr modules and install them to the local Maven repo.
#    The quickstart depends on 2.0.0 via groupId com.iambilotta.gdpr (the artifacts'
#    own coordinates), resolved from your local ~/.m2 after this install. Consumers
#    who pull from JitPack instead use groupId com.github.iambilotta.spring-gdpr
#    (see the note in the main README "Quick start").
cd ../..
mvn -B -ntp -DskipTests install

# 2) Start Postgres
cd examples/quickstart-postgres
docker compose up -d

# 3) Build and start the app. Flyway applies V1__gdpr_audit_access.sql
#    + V2__customers_table.sql on first run.
mvn -B spring-boot:run
```

The app listens on `:8080`. Two demo accounts:

| User | Password   | Role  |
|------|------------|-------|
| app  | app-secret | USER  |
| dpo  | dpo-secret | DPO   |

## Walkthrough

Create a customer (USER role suffices):

```bash
curl -u app:app-secret -X POST http://localhost:8080/customers \
  -H 'Content-Type: application/json' \
  -d '{
        "id": "alice-1",
        "fullName": "Alice",
        "email": "alice@example.com",
        "taxId": "AT-12345",
        "healthCondition": "asthma"
      }'
```

Read the customer (USER role):

```bash
curl -u app:app-secret http://localhost:8080/customers/alice-1
```

This call passes through `CustomerRepository.findById`, which is annotated `@GdprPersonalData`. The advisor fires, the async decorator dispatches the audit record, the JDBC sink persists it. Tail the Postgres audit table:

```bash
docker compose exec -T postgres psql -U gdpr -d gdpr -c \
  "SELECT event_id, actor, subject_id, target_member, legal_basis, special_category FROM gdpr_audit_access ORDER BY at_ts DESC LIMIT 5;"
```

You should see one row with `actor = 'app'`, `legal_basis = '6(1)(b) + 9(2)(a)'`, `special_category = true`.

Read the audit log for the subject (who accessed what, DPO role required):

```bash
curl -u dpo:dpo-secret \
  "http://localhost:8080/gdpr/audit/access?subjectId=alice-1"
```

Run the Article 15 right-of-access export (DPO role required). The `customerDataProvider`
bean in [`GdprExportConfig`](src/main/java/com/example/gdprdemo/GdprExportConfig.java) feeds
the `Customer`'s `@GdprPersonalData` fields into the dossier, each tagged with its category:

```bash
curl -u dpo:dpo-secret \
  "http://localhost:8080/gdpr/access/export?subjectId=alice-1"
```

```json
{
  "subjectId": "alice-1",
  "fields": [
    { "field": "fullName", "value": "Alice", "description": "full legal name", "category": "IDENTITY" },
    { "field": "email", "value": "alice@example.com", "description": "primary email", "category": "CONTACT" },
    { "field": "taxId", "value": "AT-12345", "description": "national tax id", "category": "FINANCIAL" }
  ]
}
```

Run a right-to-erasure (DPO role only):

```bash
curl -u dpo:dpo-secret -X DELETE http://localhost:8080/gdpr/erasure/alice-1
```

Response:

```json
{
  "subjectId": "alice-1",
  "affectedByType": {
    "com.example.gdprdemo.Customer": 1
  }
}
```

Same call as a USER returns 403.

## Where the artifacts land

After `mvn compile`:

- `target/generated-sources/annotations/spring/gdpr/dpia.md`: DPIA scaffold for the Customer entity, ready for DPO review.
- `target/generated-sources/annotations/spring/gdpr/ropa.csv`: Records of Processing Activities row.

Commit both to your repo. The diff at PR review is the most useful artifact you can show your DPO.

## Migration story

Flyway runs from `classpath:db/migration`. Two scripts ship with the quickstart:

- `V1__gdpr_audit_access.sql`: copy of the migration shipped inside `spring-gdpr-starter`. Apply this exact file, or its Liquibase equivalent, in your real project.
- `V2__customers_table.sql`: the demo's own schema.

In your project, you typically run V1 ONCE when adopting spring-gdpr. After that, the schema is part of your migration history alongside everything else.

## What this example does NOT show

- Production-grade authentication. In-memory users with hardcoded passwords are a demo, not a template. Wire your IdP.
- Multi-tenant data isolation. Add a tenant column on the audit table and a custom `ActorResolver` that includes tenant context.
- Cross-region replication of the audit log. Treat that as a backup / sink-of-sinks layer above this starter.
- Consent management (Art. 7). Coming in v0.2.
- Data portability export (Art. 20). Coming in v0.2.

## Tear down

```bash
docker compose down -v
```
