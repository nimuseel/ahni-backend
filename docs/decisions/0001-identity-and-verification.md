# Decision 0001: identity and external verification

## Decision

- Supabase PostgreSQL is the managed database. Spring Boot owns business rules and the HTTP API.
- Student and admin passwords are managed by Spring Boot and stored only as password hashes.
- A student is not treated as verified solely because a local account exists.
- Initial student verification is delegated to an external source verification system using documents such as an enrollment certificate.
- Verification attempts are recorded in `student_verification`; the document binary and external system credentials are not stored in this table.
- The external system integration is isolated behind an application port so the first implementation can use a stub or sandbox adapter.

## Consequences

- `student.password_hash` is nullable until the student completes the verification and local account setup flow.
- The verification provider, reference, status, timestamps, and failure reason are auditable.
- A provider-specific API client must not leak into domain or controller code.
- Document upload and attachment storage are separate work items and are not part of this migration.
