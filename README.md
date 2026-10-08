# Financial Action Engine

A backend system designed to safely process financial actions proposed by AI agents, with a focus on correctness, reliability, and auditability.

## Overview

As AI agents become more capable of interacting with business systems, executing financial operations requires strong guarantees around validation, authorization, consistency, and traceability.

The Financial Action Engine explores how to build a reliable boundary between AI-driven decision-making and deterministic financial execution.

AI agents can propose financial actions, but they cannot directly modify the ledger. Every action must go through validation, risk assessment, and the appropriate approval workflow before it can be executed.

**Core principle: AI can propose. Deterministic systems validate. Financial execution must be reliable and auditable.**

## Goals

The project aims to demonstrate backend engineering practices for financial systems, including:

* Double-entry bookkeeping and financial data integrity.
* Idempotent APIs and safe handling of concurrent requests.
* Transactional consistency across financial records.
* Risk-based approval workflows.
* Immutable audit history.
* Reliable event publishing with the Transactional Outbox pattern.
* Asynchronous event processing with Apache Kafka.
* Automated testing of business rules, failure scenarios, and concurrency.
* Production-oriented observability and error handling.

## Planned Features

* **Financial action management:** Create, validate, approve, execute, and track financial actions.
* **Ledger:** Record balanced journal entries using double-entry bookkeeping.
* **Idempotency:** Prevent duplicate financial operations caused by retries or concurrent requests.
* **Risk assessment:** Evaluate actions and require approval when defined risk policies are triggered.
* **Accounting periods:** Prevent new entries from being recorded in closed accounting periods.
* **Audit trail:** Track relevant changes and execution history.
* **Transactional Outbox:** Persist domain events atomically with financial transactions and publish them asynchronously.
* **Event-driven processing:** Publish financial events through Kafka and handle duplicate deliveries safely.
* **Observability:** Expose application health, metrics, and structured logs.

## Architecture

The initial design follows a modular monolith approach: a single Spring Boot application with clearly separated responsibilities.

```text
                    AI Agent / API Client
                            |
                            v
                  Financial Action API
                            |
                            v
                 Business Rules & Policies
                  /        |         \
                 /         |          \
          Validation    Risk Check   Authorization
                 \         |          /
                  \        v         /
                   Financial Execution
                            |
                            v
                      PostgreSQL
                 /          |          \
                /           |           \
         Journal Entries  Audit Events  Outbox Events
                                           |
                                           v
                                    Outbox Publisher
                                           |
                                           v
                                        Kafka
```

The architecture is intentionally designed to start simple while preserving clear boundaries between business logic, persistence, and asynchronous event processing.

## Technology Stack

| Technology                 | Purpose                                          |
| -------------------------- | ------------------------------------------------ |
| Java 25                    | Primary programming language                     |
| Spring Boot                | Application framework                            |
| Spring Data JPA            | Persistence and database access                  |
| PostgreSQL                 | Relational storage and transactional consistency |
| Flyway                     | Versioned database migrations                    |
| Apache Kafka               | Asynchronous event distribution                  |
| Docker                     | Local development and infrastructure             |
| JUnit and Spring Boot Test | Automated testing                                |
| Testcontainers             | Integration testing with real infrastructure     |
| Spring Boot Actuator       | Application health and metrics                   |

Some components will be introduced as their corresponding features are implemented.

## Key Engineering Challenges

### Financial Consistency

Financial execution must not leave partially persisted journal entries or inconsistent account balances. Related changes must be committed or rolled back together.

### Idempotency and Concurrency

Retries and concurrent requests must not result in duplicate financial operations. The design will combine application-level handling with database constraints and transactional guarantees.

### Reliable Event Publishing

Database transactions and Kafka publishing cannot be treated as a single local transaction. The Transactional Outbox pattern will be used to persist events alongside financial changes, with asynchronous publishing and duplicate-tolerant consumers.

### Risk and Authorization

Financial actions must be evaluated against explicit business rules. Approval requirements and execution permissions must be enforced by the backend rather than trusted to the API client or AI agent.

### Auditability

Relevant state changes and financial execution decisions must be traceable, supporting investigation and accountability.

## Development Roadmap

The implementation will be developed incrementally:

1. **Domain and persistence:** Define the core domain model, accounting rules, PostgreSQL schema, and database migrations.
2. **Financial execution:** Implement validation, double-entry bookkeeping, transactional execution, and idempotency.
3. **Risk and approvals:** Introduce risk policies, authorization, and approval workflows.
4. **Accounting controls and auditing:** Implement accounting periods, audit history, and concurrency safeguards.
5. **Event-driven architecture:** Add the Transactional Outbox, Kafka publishing, and idempotent event consumption.
6. **Production readiness:** Improve observability, integration testing, failure handling, and technical documentation.

## Testing Strategy

Testing will focus not only on successful execution but also on conditions that could compromise financial integrity.

Planned scenarios include:

* Unbalanced journal entries.
* Invalid accounts and unsupported currency combinations.
* Duplicate requests and concurrent execution attempts.
* Reuse of an idempotency key with a different request payload.
* Unauthorized approvals and invalid state transitions.
* Attempts to execute entries in closed accounting periods.
* Transaction failures and partial-write prevention.
* Outbox publishing failures and duplicate event delivery.

Integration tests will use PostgreSQL and Kafka through Testcontainers where appropriate.

## Design Principles

* Financial correctness takes precedence over convenience.
* Business rules must be enforced on the server side.
* Database constraints complement application-level validation.
* Financial records must remain consistent under concurrency and failures.
* Event consumers must tolerate duplicate delivery.
* Architectural complexity should be justified by a concrete requirement.
* Every significant design decision should have a clear rationale.

## Project Status

**Status: Planning and initial design.**

The repository is being developed incrementally. Planned features and architectural decisions will be documented as implementation progresses.

## Documentation

Detailed documentation will be maintained alongside the code:

* `docs/requirements/` — Business rules and functional requirements.
* `docs/architecture/` — Architecture diagrams and system workflows.
* `docs/api/` — API contracts and endpoint documentation.
* `docs/decisions/` — Architecture Decision Records (ADRs).
* `docs/runbooks/` — Operational procedures and troubleshooting guides.

## Scope and Trade-offs

This project is an engineering portfolio project intended to explore the design of reliable financial backend systems. It will use simulated financial actions rather than connect to real banking infrastructure or execute real payments.

The initial implementation will prioritize correctness, meaningful tests, and understandable architecture over unnecessary infrastructure or premature distribution into multiple services.

## License

To be defined.
