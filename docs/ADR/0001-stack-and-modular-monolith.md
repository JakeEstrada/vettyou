# ADR 0001: Angular + Spring Boot modular monolith

- **Status:** Accepted
- **Date:** 2026-11-06

## Context

Vettyou needs a typed frontend, a backend that owns validation and security, relational data with strong provenance, and room to grow. It is also a deliberate portfolio step into Java and Angular.

## Decision

- Angular (standalone components) + TypeScript frontend
- Java 21 + Spring Boot backend as a single modular monolith
- PostgreSQL with Flyway
- REST/JSON under `/api/v1`
- AI behind a provider-neutral backend interface

## Consequences

- One deployable backend, simpler ops and testing.
- Module boundaries live in packages, so discipline matters.
- No Kafka, Redis, microservices, or GraphQL unless a future ADR justifies them.
