# Security Policy

## Supported versions

`spring-gdpr` is at v0.1, pre-1.0. Only the latest minor receives security fixes during this period.

| Version | Status              |
|---------|---------------------|
| 0.1.x   | Supported           |
| < 0.1   | Not supported (none) |

After v1.0, the support window will be the latest two minor versions.

## Reporting a vulnerability

Do **NOT** open a public GitHub issue for vulnerabilities.

Send a private report to `francesco@iambilotta.com` with:

- A short description of the issue and the affected component (annotation, AOP advisor, JDBC sink, REST endpoint, processor, plugin).
- The version of `spring-gdpr` and the runtime stack (JDK, Spring Boot, RDBMS).
- Reproduction steps or a minimal code sample.
- Whether the vulnerability is already public elsewhere.

## Process

- Acknowledgement within 5 business days.
- Triage and severity assessment within 10 business days.
- Coordinated disclosure: a 90-day embargo from acknowledgement to public advisory, unless an active exploit is observed in which case the timeline shortens.
- A patched release on Maven Central, a CVE if warranted, and a credit line in the changelog and release notes for the reporter (or anonymous, on request).

## Out of scope

- Misconfigurations downstream of the library (publishing `/gdpr/**` without authentication, missing data-at-rest encryption on the audit table, weak Spring Security wiring). These are user responsibility, not library bugs. README + the quickstart example show the right pattern.
- Vulnerabilities in transitive dependencies. Report those to the upstream project; we will track and bump.
- Theoretical issues without a reproduction.

## What we will not do

- Pay bug bounties at v0.1. The repo is a single-maintainer open-source asset, not a funded program.
- Publicly credit a reporter who has actively exploited the issue against a third party.
