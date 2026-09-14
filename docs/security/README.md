# Security Baseline

- Supabase Auth owns student and administrator passwords. AHNI must not store, compare, log, or return passwords or password hashes.
- Authenticate every student and administrator endpoint.
- Authorize by role and resource ownership, not by client-provided identifiers alone.
- Restrict signup through exact entries in `allowed_signup_email_domain`; do not allow a domain solely because it ends in `.edu` or `.ac.kr`.
- Treat email confirmation as proof of mailbox control, not proof of current enrollment or legal identity.
- Do not relink a student profile to a new Supabase user ID using email alone.
- Validate file type, size, and content before OCR or storage.
- Keep secrets in environment variables or a secret manager, never in Git.
- Treat OCR, map, notification, and other external service responses as untrusted input.
