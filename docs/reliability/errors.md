# Errors and Observability

Errors must preserve the operation, actor scope, external dependency, and actionable cause without exposing secrets or personal data.

- Client errors return a stable error shape and safe user-facing message.
- Unexpected failures are logged with a correlation identifier and stack trace on the server.
- External OCR, source-verification, map, and push failures are distinguishable from domain validation failures.
- Retry and timeout policy belongs at the external adapter boundary.
- Error logs must not contain passwords, tokens, raw documents, or unnecessary student data.

Add structured logging and a request correlation strategy when the first HTTP boundary is implemented.
