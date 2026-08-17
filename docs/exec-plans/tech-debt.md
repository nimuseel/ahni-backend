# Technical Debt

| Problem | Impact | Location | Priority |
| --- | --- | --- | --- |
| No persistent adapter or migration strategy yet | Domain data cannot be stored | `src/main` | High |
| Architecture rules are documented but not machine-checked | Dependency drift can go unnoticed | `ARCHITECTURE.md` | Medium |
| Structured request logging is not implemented | Production diagnosis will be slow | `docs/reliability/errors.md` | Medium |
