# Feature Specification: Persistence

**Feature Branch**: `004-persistence`

**Created**: 2026-11-18

**Status**: Draft

**Input**: Roadmap item 004. Products are stored in the embedded H2 database through Spring Data
JDBC, with a schema; the products API behaves exactly as before. Depends on 003.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Products live in a database table (Priority: P1)

As the owner, I want products stored in a real table with a schema, so later items
(reservations, paging) can query and update them safely.

**Why this priority**: The in-memory store can't enforce rules, lock rows or page results.

**Independent Test**: Create a product through the API, then read the `product` table directly.

**Acceptance Scenarios**:

1. **Given** the application has started, **When** the database is inspected, **Then** a table
   `product` exists with columns `id`, `name` and `stock`.
2. **Given** a client posts `{"name": "Widget", "stock": 5}`, **When** the `product` table is
   queried for the returned id, **Then** it holds one row with `name` `Widget` and `stock` `5`.
3. **Given** a row inserted directly into `product` (`name` `Bolt`, `stock` `3`), **When** a
   client calls `GET /api/products/{its id}` and `GET /api/products`, **Then** both return it.

---

### User Story 2 - The database enforces the rules too (Priority: P2)

As the owner, I want the table to refuse data the API would refuse, so a bug in one place can't
corrupt the catalogue.

**Why this priority**: Defence in depth for the data later items depend on.

**Independent Test**: Insert bad rows directly and expect the database to refuse them.

**Acceptance Scenarios**:

1. **Given** the `product` table, **When** a row with a `NULL` name, a name of 101 characters,
   or a negative `stock` is inserted directly, **Then** the database refuses it.
2. **Given** a row inserted without an `id`, **When** it is read back, **Then** the database
   assigned a positive id greater than every earlier one.

---

### User Story 3 - Nothing changes for clients (Priority: P1)

As a client, I see the same API as before: same status codes, bodies, ordering and errors.

**Why this priority**: This item changes storage, not behaviour.

**Independent Test**: The 002 and 003 tests pass unedited.

**Acceptance Scenarios**:

1. **Given** products created in an order, **When** a client calls `GET /api/products`, **Then**
   they come back in creation order (ascending id), as in 002.
2. **Given** any request covered by 002 or 003, **When** it is made, **Then** the response is
   the same as before this item.

---

### Edge Cases

- Concurrent creations each get a distinct id (the database assigns them).
- The database is the in-memory embedded H2 the planning PR declared; data does not survive a
  restart, and no file or server database is configured.
- The H2 web console stays off.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The application MUST create a table `product` at startup from a schema file under
  `src/main/resources/`, with `id` (a whole number the database assigns, primary key), `name`
  (text, not null, at most 100 characters) and `stock` (whole number, not null, `>= 0`).
- **FR-002**: Products MUST be read and written through a Spring Data JDBC repository interface
  `ProductRepository` in the package `dev.stockroom.product`.
- **FR-003**: `ProductStore` MUST remain a bean with its public methods `save(String name, int
  stock)`, `findById(long id)` and `findAll()` and their return types, now backed by
  `ProductRepository`; `findAll()` MUST return products in ascending id order.
- **FR-004**: The products API (002) and its errors (003) MUST behave exactly as before.
- **FR-005**: The application MUST use the embedded in-memory H2 database; no other datasource,
  and the H2 console off.
- **FR-006**: Tests for this item MUST be added under `src/test/java/dev/stockroom/`, including
  tests of the repository, and every existing test MUST keep passing unedited. No dependency
  may be added.

## Allowed paths

- `src/main/java/dev/stockroom/*`
- `src/main/resources/*`
- `src/test/java/dev/stockroom/*`

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every acceptance scenario above passes as an automated test under `mvn test`.
- **SC-002**: The 001, 002 and 003 tests pass without being edited.

## Assumptions

- Existing tests inject `ProductStore` and call `findAll()`; keeping its methods (FR-003) means
  no existing test needs to change, so this spec has no `## Tests that may change`.
- Tests share one in-memory database per Spring context, so tests must not assume an empty
  table unless they clear it themselves.
