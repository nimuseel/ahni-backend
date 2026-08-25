# Security Baseline

- Supabase Auth owns student and administrator passwords. AHNI must not store, compare, log, or return passwords or password hashes.
- Authenticate every student and administrator endpoint.
- Authorize by role and resource ownership, not by client-provided identifiers alone.
- Validate file type, size, and content before OCR or storage.
- Keep secrets in environment variables or a secret manager, never in Git.
- Treat external verification and map responses as untrusted input.
