---
name: Bug report
about: A reproducible defect in the library
labels: bug
---

## What happened

(One paragraph. Concrete, observable behavior.)

## What you expected to happen

(One paragraph. The contract you thought you had.)

## Reproduction

```java
// minimal code that triggers the bug
```

```yaml
# minimal application.yml or relevant config
```

```sql
-- if it touches the JDBC sink: schema state, RDBMS engine + version
```

## Environment

- spring-gdpr version: x.y.z
- Spring Boot: x.y.z
- JDK: vendor + version
- RDBMS (if relevant): vendor + version
- OS: linux/macos/windows + version

## Logs / stack trace

(WARN/ERROR lines with `gdpr` or `spring.gdpr` in them. Redact PII.)

## Have you tried

- [ ] Searched existing issues
- [ ] Reproduced on the latest released version
- [ ] Confirmed it is not a misconfiguration documented in the README
