# Errors and Observability

Errors must preserve the operation, actor scope, external dependency, and actionable cause without exposing secrets or personal data.

## HTTP error contract

Every HTTP error uses this documented shape:

```json
{
  "code": "VALIDATION_FAILED",
  "message": "One or more fields are invalid.",
  "correlationId": "01J...",
  "fieldErrors": {
    "email": "must be a valid email address"
  }
}
```

- `code` is a stable machine-readable error code.
- `message` is a user-safe message.
- `correlationId` identifies the request for support and server-side diagnosis.
- `fieldErrors` contains field-level validation failures; it is omitted when no boundary validation failure applies.

- Client errors return a stable error shape and safe user-facing message.
- Unexpected failures are logged with a correlation identifier and stack trace on the server.
- External OCR, source-verification, map, and push failures are distinguishable from domain validation failures.
- Retry and timeout policy belongs at the external adapter boundary.
- Error logs must not contain passwords, tokens, raw documents, or unnecessary student data.

Add structured logging and a request correlation strategy when the first HTTP boundary is implemented. Every API change documents stable error codes and failure examples in its OpenAPI contract.
