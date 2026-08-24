# Decision 0001: identity and external verification

## Decision

- Supabase PostgreSQL is the managed database. Spring Boot owns business rules and the HTTP API.
- Supabase Auth owns student and administrator passwords. AHNI does not store, compare, or manage password hashes.
- A student is not treated as verified solely because a local account exists.
- Initial student verification is delegated to an external source verification system using documents such as an enrollment certificate.
- Verification attempts are recorded in `student_verification`; the document binary and external system credentials are not stored in this table.
- The external system integration is isolated behind an application port so the first implementation can use a stub or sandbox adapter.

## Consequences

- AHNI links its student and administrator records to Supabase identity references; it does not implement a local account setup flow.
- The verification provider, reference, status, timestamps, and failure reason are auditable.
- A provider-specific API client must not leak into domain or controller code.
- Document upload and attachment storage are separate work items and are not part of this migration.
- The existing password-hash schema columns conflict with this decision and are tracked as blocking debt for `fix/supabase-auth-schema`; this harness change does not modify migrations.
