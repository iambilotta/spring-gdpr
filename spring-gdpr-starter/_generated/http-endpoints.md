# HTTP endpoints — spring-gdpr-starter (as-is)

Auto-generated from `@GetMapping` / `@PostMapping` / `@RequestMapping` annotations under `apps/gest/src/main/java`. Spring Cloud Contracts matched on (method, URL) are linked inline. Run `make code-docs` to regenerate; the pre-commit hook does it for you.

**Total routes**: 3

| Method | URL | Controller | Condition | Purpose |
|---|---|---|---|---|
| `GET` | `${spring.gdpr.web.base-path:/gdpr}/access/export` | `GdprController#accessExport` | — | Article 15 right of access: returns the dossier of classified personal-data fields assembled from every registered `SubjectDataProvider`. Empty `fields` when no provider holds data for the subject (the honesty contract: exactly what the providers return, never more). |
| `GET` | `${spring.gdpr.web.base-path:/gdpr}/audit/access` | `GdprController#auditAccess` | — | _(no javadoc)_ |
| `DELETE` | `${spring.gdpr.web.base-path:/gdpr}/erasure/{subjectId}` | `GdprController#erase` | — | _(no javadoc)_ |

## Detail

### `GET ${spring.gdpr.web.base-path:/gdpr}/access/export`

- **Controller**: `GdprController#accessExport`
- **File**: `spring-gdpr-starter/src/main/java/com/iambilotta/gdpr/starter/web/GdprController.java`
- **Purpose** (from javadoc): Article 15 right of access: returns the dossier of classified personal-data fields assembled from every registered `SubjectDataProvider`. Empty `fields` when no provider holds data for the subject (the honesty contract: exactly what the providers return, never more).

### `GET ${spring.gdpr.web.base-path:/gdpr}/audit/access`

- **Controller**: `GdprController#auditAccess`
- **File**: `spring-gdpr-starter/src/main/java/com/iambilotta/gdpr/starter/web/GdprController.java`
- **Purpose**: _(no javadoc on the controller method — add one)_

### `DELETE ${spring.gdpr.web.base-path:/gdpr}/erasure/{subjectId}`

- **Controller**: `GdprController#erase`
- **File**: `spring-gdpr-starter/src/main/java/com/iambilotta/gdpr/starter/web/GdprController.java`
- **Purpose**: _(no javadoc on the controller method — add one)_
