# Security Baseline

- Store passwords only as adaptive hashes; never log or return them.
- Authenticate every student and administrator endpoint.
- Authorize by role and resource ownership, not by client-provided identifiers alone.
- Validate file type, size, and content before OCR or storage.
- Keep secrets in environment variables or a secret manager, never in Git.
- Treat external verification and map responses as untrusted input.
