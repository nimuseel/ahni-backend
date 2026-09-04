# Technical Debt

| Problem | Impact | Location | Priority |
| --- | --- | --- | --- |
| Business JPA entities and repositories are not implemented yet | Business persistence APIs are not available; dev schema creation and legacy migration tests already exist | `src/main` | High |
| Structured request logging is not implemented | Production diagnosis will be slow | `docs/reliability/errors.md` | Medium |
| `student.password_hash` and `admin.password_hash` conflict with the Supabase Auth identity policy | Blocking schema mismatch; local password storage contradicts the canonical identity model | `src/main/resources/db/migration/V1__create_core_identity_tables.sql`; remove the columns and replace them with Supabase identity references on `fix/supabase-auth-schema` | Blocking |
