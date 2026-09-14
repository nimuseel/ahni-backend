# Decision 0001: Supabase identity and school email access

**Status:** Accepted, updated 2026-09-14. This replaces the earlier enrollment-certificate and administrator-review policy.

## Decision

- Supabase PostgreSQL is the managed database. Spring Boot owns business rules and the HTTP API.
- Supabase Auth owns student and administrator passwords. AHNI does not store, compare, or manage password hashes.
- Supabase Auth email confirmation is the signup verification step. The mobile client receives an authenticated session only after the user confirms the email address.
- Signup accepts only exact domains in `allowed_signup_email_domain`. The initial allowlist contains `inha.edu`; future institutions are added explicitly instead of accepting every `.edu` or `.ac.kr` address.
- Enrollment certificates, document uploads, external source verification, and administrator approval are not part of the signup flow.
- Email confirmation proves control of an approved school email address. It does not independently prove the user's current enrollment status or legal identity.
- A confirmed user registers a local student profile with department, admission year, enrollment status, and an optional nickname. Enrollment status is user-provided profile data.
- Supabase owns the pre-confirmation pending state. A persisted student account uses only `ACTIVE` or `SUSPENDED` as its application account status.

## Consequences

- AHNI links student and administrator records to the immutable Supabase user ID and never accepts a client-provided user ID as ownership evidence.
- The backend derives the student email from the verified JWT claim when creating the local profile.
- A user cannot automatically reclaim a student profile merely by presenting the same email with a different Supabase user ID. Account recovery requires an explicit policy because school addresses can be recycled.
- `V3__align_identity_schema_with_supabase_auth.sql` removed local password hashes, and `V6__align_student_profile_with_email_signup.sql` removed `student_verification`.
- Expanding to another school requires adding its exact domain to the allowlist and aligning client validation. It does not require a document-verification workflow.
- Any future decision to verify enrollment independently must define its evidence, retention, privacy, expiration, and recovery rules in a new decision before implementation.
