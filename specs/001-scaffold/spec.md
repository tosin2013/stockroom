# Feature Specification: Scaffold

**Feature Branch**: `001-scaffold`

**Created**: 2026-11-18

**Status**: Draft

**Input**: Roadmap item 001. The empty repository becomes a Spring Boot application that starts,
reports its health and has its first test. Dependencies and CI already exist from the planning
pull request.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The service starts and reports its health (Priority: P1)

As an operator, I can start Stockroom and ask whether it is healthy, so that I can run it and
monitor it from day one.

**Why this priority**: Every later item builds on a running application.

**Independent Test**: Start the application in a test and call `GET /actuator/health`.

**Acceptance Scenarios**:

1. **Given** the application, **When** its Spring context starts in a test, **Then** it starts
   without errors.
2. **Given** the running application, **When** a client calls `GET /actuator/health`, **Then**
   the response is `200` with a JSON body whose `status` is `UP`.
3. **Given** the running application, **When** a client calls `GET /actuator/info` or
   `GET /actuator/env`, **Then** the response is `404`: only the health endpoint is exposed.

---

### User Story 2 - The project is ready to grow (Priority: P1)

As a developer, I find a conventional layout, virtual threads on, and a passing test, so that
the next roadmap item starts from a working base.

**Why this priority**: The scaffold is only useful if the next item can build on it.

**Independent Test**: Read the configuration and run `mvn test`.

**Acceptance Scenarios**:

1. **Given** the application's configuration, **When** it is read, **Then**
   `spring.threads.virtual.enabled` is `true`.
2. **Given** the project, **When** `mvn test` runs, **Then** at least one test of the project's
   own passes.

---

### Edge Cases

- The application must start without a database configured explicitly: embedded H2 is on the
  classpath (004 uses it), and starting must not fail because of it.
- No business endpoints exist yet: `GET /api/products` returns `404` until 002.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The application class MUST be `dev.stockroom.StockroomApplication`, annotated
  `@SpringBootApplication`, with a `main` method that starts it.
- **FR-002**: `GET /actuator/health` MUST return `200` and a JSON body with `"status": "UP"`.
- **FR-003**: Only the `health` actuator endpoint MUST be exposed over HTTP
  (`management.endpoints.web.exposure.include=health`).
- **FR-004**: `src/main/resources/application.properties` MUST set
  `spring.threads.virtual.enabled=true` and `spring.application.name=stockroom`.
- **FR-005**: The project MUST include at least one test of its own under
  `src/test/java/dev/stockroom/`, and `mvn test` MUST pass.
- **FR-006**: No dependency may be added or changed (`pom.xml` is outside this item).

## Allowed paths

- `src/main/java/dev/stockroom/*`
- `src/main/resources/*`
- `src/test/java/dev/stockroom/*`

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: `mvn test` passes on a clean checkout with no network beyond the approved
  dependencies.
- **SC-002**: The health endpoint answers `UP` within the test's application start.

## Assumptions

- Java 21 and the dependencies in `pom.xml` (approved in the planning pull request).
