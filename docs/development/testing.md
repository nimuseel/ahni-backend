# Testing Guide

## Layers

- Unit: pure domain calculations and policy decisions. Fast, no Spring context.
- Integration: Spring boundary, serialization, persistence, and external adapter contracts.
- Architecture: dependency direction once domain/application modules exist.
- E2E: API scenarios covering a user-visible outcome when the HTTP surface is available.

## Rules

Write a failing test before a new domain behavior. Use real value objects where possible, keep test names behavior-focused, and run the narrowest test during development before `./scripts/verify`.

Current baseline: `AhniBackendApplicationTests` proves the Spring context starts. Domain unit tests are added with the first GPA and graduation-rule implementation.
