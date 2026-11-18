# Feature Specification: Products API

**Feature Branch**: `002-products-api`

**Created**: 2026-11-18

**Status**: Draft

**Input**: Roadmap item 002. Create, read and list products held in memory, with validation.
Depends on 001.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Add a product to the catalogue (Priority: P1)

As a store clerk's tool, I can add a product with its name and stock level, and get back where
to find it.

**Why this priority**: Nothing else in the catalogue works without products.

**Independent Test**: `POST /api/products` with a valid body, then follow `Location`.

**Acceptance Scenarios**:

1. **Given** no products, **When** a client posts `{"name": "Widget", "stock": 5}` to
   `/api/products`, **Then** the response is `201` with a `Location` header
   `/api/products/{id}` and a JSON body `{"id": <id>, "name": "Widget", "stock": 5}`.
2. **Given** a name with surrounding spaces `"  Widget  "`, **When** it is posted, **Then** the
   stored and returned name is `"Widget"`.
3. **Given** two products posted one after another, **When** their ids are compared, **Then**
   each id is a positive whole number and the second is greater than the first.

---

### User Story 2 - Reject invalid products (Priority: P1)

As a client, I get a clear `400` when I send a product the catalogue can't accept.

**Why this priority**: Bad data must never enter the catalogue.

**Independent Test**: Post each invalid body and expect `400`.

**Acceptance Scenarios**:

1. **Given** a body with a missing, empty or blank `name`, **When** it is posted, **Then** the
   response is `400` and nothing is stored.
2. **Given** a `name` longer than 100 characters (after trimming), **When** it is posted,
   **Then** the response is `400`.
3. **Given** a missing or negative `stock`, **When** it is posted, **Then** the response is
   `400`. A `stock` of `0` is accepted.

---

### User Story 3 - Read the catalogue (Priority: P1)

As a client, I can fetch one product by id or list them all.

**Why this priority**: Stored products are useless if they can't be read.

**Independent Test**: Create products, then `GET` one and the list.

**Acceptance Scenarios**:

1. **Given** a created product, **When** a client calls `GET /api/products/{id}`, **Then** the
   response is `200` with the same body the creation returned.
2. **Given** no product with id `999`, **When** a client calls `GET /api/products/999`, **Then**
   the response is `404`.
3. **Given** products created in an order, **When** a client calls `GET /api/products`, **Then**
   the response is `200` with a JSON array of them in creation order (an empty array when there
   are none).

---

### Edge Cases

- Concurrent creations each get a distinct id (the store must be safe for concurrent use).
- The error body format is not specified here; 003 makes errors Problem Details.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: `POST /api/products` MUST accept `{"name": string, "stock": integer}` and answer
  `201` with `Location: /api/products/{id}` and the created product as JSON.
- **FR-002**: `name` MUST be required, not blank, at most 100 characters, and trimmed before it
  is stored; `stock` MUST be required and `>= 0`. Any violation MUST answer `400` and store
  nothing.
- **FR-003**: Ids MUST be positive whole numbers assigned in increasing order.
- **FR-004**: `GET /api/products/{id}` MUST answer `200` with the product, or `404` when it
  doesn't exist.
- **FR-005**: `GET /api/products` MUST answer `200` with all products in creation order.
- **FR-006**: Request and response bodies MUST be Java records in the package
  `dev.stockroom.product`; products MUST be held in memory in a store safe for concurrent use.
- **FR-007**: Tests for this item MUST be added under `src/test/java/dev/stockroom/`, and every
  existing test MUST keep passing, except the one 001 test that expects `/api/products` not to
  exist: this item replaces it (see Tests that may change). No dependency may be added.

## Allowed paths

- `src/main/java/dev/stockroom/*`
- `src/test/java/dev/stockroom/*`

## Tests that may change

- `src/test/java/dev/stockroom/StockroomApplicationTests.java`: its
  `productsEndpointDoesNotExistYet` test expects `GET /api/products` to answer `404`; this item
  adds that endpoint, so the test is removed or changed to expect `200`. Its other tests stay.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every acceptance scenario above passes as an automated test under `mvn test`.
- **SC-002**: The 001 tests still pass.

## Assumptions

- Persistence arrives in 004; this item's store is in memory and starts empty.
