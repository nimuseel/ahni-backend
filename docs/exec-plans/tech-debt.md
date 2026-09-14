# Technical Debt

| Problem | Impact | Location | Priority |
| --- | --- | --- | --- |
| Structured request logging is not implemented | Production diagnosis will be slow | `docs/reliability/errors.md` | Medium |
| The documented error contract is ahead of the implemented response | Clients currently receive `code` and `message`, without `correlationId` or `fieldErrors` | `ApiErrorResponse`; `docs/reliability/errors.md` | Medium |
| Recreated Supabase users cannot reclaim a profile bound to an older auth user ID | Registration returns an email conflict and currently requires manual support; automatic email-based relinking could transfer an account when a school address is recycled | `StudentService.registerProfile` | High |
