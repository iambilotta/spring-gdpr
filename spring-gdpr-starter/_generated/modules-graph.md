# Module map — spring-gdpr-starter (as-is)

Auto-generated from the same data as `modules.md`: each top-level package under `com.iambilotta.gdpr.starter` is a module, the arrows are the cross-module `import` dependencies. A cycle is a Modulith boundary violation (highlighted below). Rendered as Mermaid so it shows inline on GitHub and the intranet. Run `make code-docs`.

✓ No module cycles.

```mermaid
flowchart TD
    _root_["(root)<br/>(1 files)"]
    access["access<br/>(5 files)"]
    audit["audit<br/>(9 files)"]
    autoconfig["autoconfig<br/>(2 files)"]
    erasure["erasure<br/>(21 files)"]
    jdbc["jdbc<br/>(1 files)"]
    logging["logging<br/>(2 files)"]
    retention["retention<br/>(3 files)"]
    web["web<br/>(3 files)"]
    access --> jdbc
    autoconfig --> access
    autoconfig --> audit
    autoconfig --> erasure
    autoconfig --> retention
    autoconfig --> web
    erasure --> audit
    erasure --> jdbc
    retention --> jdbc
    web --> access
    web --> audit
    web --> erasure
```
