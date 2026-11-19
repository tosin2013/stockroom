# Feature Specification: Delete a product

**Feature Branch**: `009-delete-product`

**Created**: 2026-11-19

**Status**: Draft

**Input**: Roadmap item 009. A client removes a product it no longer stocks. Depends on 003
(Problem Details errors) and 004 (persistence). Also the first item whose hidden tests are
drafted automatically (specfoundry 024).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Delete an existing product (Priority: P1)

As a client, I delete a product by its id, and it is gone from every read.

**Why this priority**: The catalogue can only grow today; a discontinued product stays forever.

**Independent Test**: Create a product, delete it, then read it and list the catalogue.

**Acceptance Scenarios**:

1. **Given** an existing product, **When** a client calls `DELETE /api/products/{id}`, **Then**
   the answer is `204` with an empty body.
2. **Given** that product was deleted, **When** a client calls `GET /api/products/{id}`, **Then**
   the answer is `404` as a Problem Detail (003), and `GET /api/products` no longer lists it.
3. **Given** two products, **When** one is deleted, **Then** the other is unchanged: same name and
   stock, still readable by its id and still listed.

---

### User Story 2 - Deleting what isn't there (Priority: P1)

As a client, deleting an unknown product tells me so, the same way reading one does.

**Why this priority**: A clear error stops a client from believing a delete worked.

**Independent Test**: Delete an id that never existed, and one already deleted.

**Acceptance Scenarios**:

1. **Given** no product with id `999999`, **When** a client calls `DELETE /api/products/999999`,
   **Then** the answer is `404` as a Problem Detail with `status` `404` and a `detail` naming
   the id.
2. **Given** a product that was just deleted, **When** a client deletes it again, **Then** the
   answer is `404`.
3. **Given** an id that isn't a whole number, **When** a client calls `DELETE /api/products/abc`,
   **Then** the answer is `400` as a Problem Detail (003 FR-004).

---

### User Story 3 - A deleted product can't be reserved (Priority: P2)

As a client, reserving a deleted product fails like reserving one that never existed.

**Why this priority**: Stock must not be taken from a product that no longer exists.

**Independent Test**: Delete a product, then reserve it.

**Acceptance Scenarios**:

1. **Given** a deleted product, **When** a client calls `POST /api/products/{id}/reservations`
   with a quantity of `1`, **Then** the answer is `404` as a Problem Detail, and no
   `StockReserved` event is published.

---

### Edge Cases

- Deleting doesn't renumber anything: other products keep their ids, and a new product never
  reuses a deleted one's id.
- Paging (006) counts only existing products after a delete: `totalElements` drops by one.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: `DELETE /api/products/{id}` MUST remove the product and answer `204` with no body.
- **FR-002**: For an id with no product, it MUST answer `404` as a Problem Detail (003 FR-001,
  FR-003). An id that isn't a whole number MUST answer `400` (003 FR-004).
- **FR-003**: After a delete, every read (by id, the plain list, paging and filtering) and every
  reservation MUST behave as if the product never existed.
- **FR-004**: Everything else MUST stay the same (001-008).
- **FR-005**: Tests for this item MUST be added under `src/test/java/dev/stockroom/`, and every
  existing test MUST keep passing unedited. No dependency may be added.

## Allowed paths

- `src/main/java/dev/stockroom/*`
- `src/test/java/dev/stockroom/*`

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every acceptance scenario above passes as an automated test under `mvn test`, and
  fails on the 008 code.
- **SC-002**: The 001-008 tests pass without being edited.

## Assumptions

- A delete is permanent; there is no soft delete or undo.
- Reservations aren't stored (005), so nothing else references a product.
- Tests share one in-memory database per Spring context, so each test creates its own products.
