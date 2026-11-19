# Feature Specification: Update a product's stock

**Feature Branch**: `010-update-stock`

**Created**: 2026-11-19

**Status**: Draft

**Input**: Roadmap item 010. A client sets a product's stock after a delivery or a count.
Depends on 003 (Problem Details errors), 004 (persistence) and 009 (delete). The first item
carried end to end by specfoundry's step mode, including its automatically drafted hidden tests
(specfoundry 021, 024).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Set the stock (Priority: P1)

As a client, I set a product's stock to a new number, and every later read and reservation sees
it.

**Why this priority**: Stock today can only go down (reservations); a delivery can't be
recorded.

**Independent Test**: Create a product with stock `5`, set it to `12`, read it back, and reserve
from it.

**Acceptance Scenarios**:

1. **Given** a product with stock `5`, **When** a client calls `PATCH /api/products/{id}` with
   `{"stock": 12}`, **Then** the answer is `200` with the product `{id, name, stock: 12}`.
2. **Given** that update, **When** a client reads the product, **Then** its stock is `12`, and a
   reservation of `10` then succeeds with `remainingStock` `2`.
3. **Given** a product, **When** its stock is set to `0`, **Then** the answer is `200`, and a
   reservation of `1` answers `409` with `available` `0`.

---

### User Story 2 - Invalid updates are refused (Priority: P1)

As a client, a wrong update tells me what's wrong and changes nothing.

**Why this priority**: A bad request must never corrupt the stock.

**Independent Test**: Send each invalid body and id, then read the product.

**Acceptance Scenarios**:

1. **Given** a product with stock `5`, **When** the body is `{"stock": -1}`, `{"stock": 1.5}`,
   `{"stock": "x"}`, `{}` or not JSON, **Then** the answer is `400` as a Problem Detail (003),
   and the stock is still `5`. For a validation failure, `errors` names the field `stock`.
2. **Given** no product with id `999999`, **When** a client patches it, **Then** the answer is
   `404` as a Problem Detail whose `detail` names the id.
3. **Given** a deleted product (009), **When** a client patches it, **Then** the answer is
   `404`.
4. **Given** a product, **When** the body also has `"name": "other"`, **Then** the name is
   unchanged: only `stock` can be updated here.

---

### Edge Cases

- Setting the stock to its current value answers `200` and changes nothing else.
- Updates and reservations on the same product don't interleave inside each other: each update
  is one atomic write.
- The plain list and paging (006) show the new stock.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: `PATCH /api/products/{id}` with `{"stock": <integer >= 0>}` MUST set the product's
  stock and answer `200` with the updated product.
- **FR-002**: A missing, negative, non-integer or non-numeric `stock`, or a body that isn't JSON,
  MUST answer `400` as a Problem Detail (003 FR-001, FR-002, FR-004) and change nothing.
- **FR-003**: An id with no product MUST answer `404` as a Problem Detail (003 FR-003).
- **FR-004**: Fields other than `stock` MUST be ignored; the name never changes through this
  endpoint.
- **FR-005**: Everything else MUST stay the same (001-009).
- **FR-006**: Tests for this item MUST be added under `src/test/java/dev/stockroom/`, and every
  existing test MUST keep passing unedited. No dependency may be added.

## Allowed paths

- `src/main/java/dev/stockroom/*`
- `src/test/java/dev/stockroom/*`

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every acceptance scenario above passes as an automated test under `mvn test`, and
  fails on the 009 code.
- **SC-002**: The 001-009 tests pass without being edited.

## Assumptions

- The update is a single conditional write (`UPDATE … SET stock = ? WHERE id = ?`) whose row
  count decides `404`.
- Tests share one in-memory database per Spring context, so each test creates its own products.
