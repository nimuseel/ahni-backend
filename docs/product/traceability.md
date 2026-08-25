# Product Traceability

This index records how supplied product references map to backend-owned capabilities. The approved cross-repository harness design is authoritative when the sources conflict; unresolved differences remain explicit until the owning feature resolves them.

| Source | Owning capability | Backend responsibility | Consuming flow | Protecting test or planned marker | Decision or conflict |
| --- | --- | --- | --- | --- | --- |
| Mini specifications: student and administrator authentication | Identity | Validate Supabase Auth sessions and enforce authenticated ownership | Mobile student flows; administrator web | Planned: authentication boundary integration tests | Source-managed passwords conflict with Supabase Auth; see Decision 0001 |
| Use-case specifications: enrollment verification | Enrollment verification | Orchestrate external evidence verification and persist safe status metadata | Mobile activation flow | Planned: verification adapter and HTTP boundary tests | Raw-document retention requires a separate policy |
| ERD revisions: student, administrator, and attachment identity modeling | Identity and attachments | Persist application records linked to Supabase identities | Mobile and administrator profile flows | Planned: PostgreSQL migration integration tests | ERD identity and attachment revisions conflict; password-hash columns are blocking debt on `fix/supabase-auth-schema` |
| Meal-recommendation references | Recommendation | Expose the finalized recommendation capability and vocabulary through the API | Mobile recommendation flow | Planned: domain and API tests | Meal naming differs across sources; resolve before endpoint and OpenAPI naming are finalized |
| Future Convergence College scope and institution-expansion references | Institution configuration | Select institution, college, department, and verification policy through stable identifiers and configuration | Mobile and administrator academic flows | Planned: configuration and authorization boundary tests | Phase one is Future Convergence College only; preserve a path to future university expansion without a speculative multi-tenant UI |

## Resolution rules

- Record a source, capability, consumer, test or planned-test marker, and unresolved decision for each new product requirement.
- Treat the backend OpenAPI document as the client contract after the corresponding API capability exists.
- Resolve a source conflict in an approved decision before implementing behavior that depends on it.
