# Dependencies — spring-gdpr-example-quickstart-postgres (as-is)

Auto-generated from `pom.xml` (Maven), `frontend/package.json` + `e2e/package.json` (npm), and `scripts/requirements.txt` (Python). Empty version = inherited from Spring Boot parent BOM. Pre-commit refreshes this; if you bump a version anywhere, the diff lands here too.

**Total**: 14 (maven=13, maven (parent)=1)

## maven (13)

| Group | Name | Version | Scope | Source |
|---|---|---|---|---|
| `com.h2database` | `h2` | `(from parent / bom)` | `test` | `examples/quickstart-postgres/pom.xml` |
| `com.iambilotta.gdpr` | `spring-gdpr-processor` | `${spring-gdpr.version}` | `provided` | `examples/quickstart-postgres/pom.xml` |
| `com.iambilotta.gdpr` | `spring-gdpr-starter` | `${spring-gdpr.version}` | `compile` | `examples/quickstart-postgres/pom.xml` |
| `org.flywaydb` | `flyway-core` | `(from parent / bom)` | `compile` | `examples/quickstart-postgres/pom.xml` |
| `org.flywaydb` | `flyway-database-postgresql` | `(from parent / bom)` | `compile` | `examples/quickstart-postgres/pom.xml` |
| `org.postgresql` | `postgresql` | `(from parent / bom)` | `runtime` | `examples/quickstart-postgres/pom.xml` |
| `org.springframework.boot` | `spring-boot-starter-data-jpa` | `(from parent / bom)` | `compile` | `examples/quickstart-postgres/pom.xml` |
| `org.springframework.boot` | `spring-boot-starter-jdbc` | `(from parent / bom)` | `compile` | `examples/quickstart-postgres/pom.xml` |
| `org.springframework.boot` | `spring-boot-starter-security` | `(from parent / bom)` | `compile` | `examples/quickstart-postgres/pom.xml` |
| `org.springframework.boot` | `spring-boot-starter-test` | `(from parent / bom)` | `test` | `examples/quickstart-postgres/pom.xml` |
| `org.springframework.boot` | `spring-boot-starter-web` | `(from parent / bom)` | `compile` | `examples/quickstart-postgres/pom.xml` |
| `org.springframework.boot` | `spring-boot-webmvc-test` | `(from parent / bom)` | `test` | `examples/quickstart-postgres/pom.xml` |
| `org.springframework.security` | `spring-security-test` | `(from parent / bom)` | `test` | `examples/quickstart-postgres/pom.xml` |

## maven (parent) (1)

| Group | Name | Version | Scope | Source |
|---|---|---|---|---|
| `org.springframework.boot` | `spring-boot-starter-parent` | `4.0.5` | `parent` | `examples/quickstart-postgres/pom.xml` |
