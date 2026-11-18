# Feature Specification: Reservations

**Feature Branch**: `005-reservations`

**Created**: 2026-11-18

**Status**: Draft

**Input**: Roadmap item 005. Reserve stock for a product: the reservation lowers stock, or answers
`409` as a Problem Detail when stock is short, and publishes a `StockReserved` application
event. Depends on 004.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Reserve stock (Priority: P1)

As a client, I reserve a quantity of a product, and its stock goes down by that amount.

**Why this priority**: Reservations are the point of an inventory service.

**Independent Test**: Create a product with stock `5`, reserve `2`, read the product.

**Acceptance Scenarios**:

1. **Given** a product with stock `5`, **When** a client posts `{"quantity": 2}` to
   `/api/products/{id}/reservations`, **Then** the response is `200` with `Content-Type:
   application/json` and the body `{"productId": <id>, "quantity": 2, "remainingStock": 3}`, and
   `GET /api/products/{id}` then shows `"stock": 3`.
2. **Given** a product with stock `5`, **When** a client reserves `5`, **Then** the response is
   `200` with `"remainingStock": 0`.

---

### User Story 2 - Refuse what can't be reserved (Priority: P1)

As a client, I get a clear refusal when there isn't enough stock, and nothing changes.

**Why this priority**: Overselling is the failure an inventory service exists to prevent.

**Independent Test**: Reserve more than the stock; reserve with a bad quantity; reserve for an
unknown product.

**Acceptance Scenarios**:

1. **Given** a product with stock `2`, **When** a client reserves `3`, **Then** the response is
   `409` with `Content-Type: application/problem+json`, `"status": 409`, `"title":
   "Conflict"`, a `detail` that contains both `3` and `2`, `"instance":
   "/api/products/{id}/reservations"`, `"requested": 3` and `"available": 2`; the stock stays `2`.
2. **Given** a body with a missing `quantity`, or a `quantity` of `0` or less, **When** it is
   posted, **Then** the response is `400` as a Problem Detail with one `errors` entry, for
   `quantity` (as in 003), and the stock is unchanged.
3. **Given** no product with id `999`, **When** a client posts a reservation to
   `/api/products/999/reservations`, **Then** the response is `404` as a Problem Detail (as in
   003).

---

### User Story 3 - Concurrent reservations never oversell (Priority: P1)

As the owner, I want the stock never to go below zero, however many clients reserve at once.

**Why this priority**: A check-then-update race would oversell under load.

**Independent Test**: Fire many reservations at once and count the outcomes.

**Acceptance Scenarios**:

1. **Given** a product with stock `10`, **When** 20 clients each reserve `1` at the same time,
   **Then** exactly 10 get `200`, exactly 10 get `409`, and the stock ends at `0`.

---

### User Story 4 - Other parts of the system hear about reservations (Priority: P2)

As a developer of later features, I can react to each reservation through an application event.

**Why this priority**: Notifications and reporting can subscribe without touching the endpoint.

**Independent Test**: Record application events during a reservation.

**Acceptance Scenarios**:

1. **Given** a product with stock `5`, **When** a client reserves `2`, **Then** exactly one
   `dev.stockroom.product.StockReserved` event is published, with `productId()` the product's id,
   `quantity()` `2` and `remainingStock()` `3`.
2. **Given** a refused reservation (`409`, `400` or `404`), **When** it completes, **Then** no
   `StockReserved` event is published.

---

### Edge Cases

- A reservation of exactly the remaining stock succeeds and leaves `0`.
- Reservations are not stored as records of their own in this item; only the stock changes.
- The products API (002–004) and its error format (003) don't change.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: `POST /api/products/{id}/reservations` MUST accept `{"quantity": integer}` with
  `quantity` required and `>= 1`; a violation MUST answer `400` as in 003 and change nothing.
- **FR-002**: When the product's stock is at least `quantity`, the stock MUST be lowered by
  `quantity` and the response MUST be `200` with `{"productId", "quantity", "remainingStock"}`.
- **FR-003**: When the stock is less than `quantity`, the response MUST be `409` as a Problem
  Detail (003 FR-001) with the extra properties `requested` and `available`, and the stock MUST
  NOT change.
- **FR-004**: An unknown product MUST answer `404` as in 003.
- **FR-005**: The check and the decrement MUST be one atomic step, so concurrent reservations can
  never take stock below `0` or reserve more than was available.
- **FR-006**: Each successful reservation MUST publish exactly one Spring application event of
  the record type `dev.stockroom.product.StockReserved(long productId, int quantity, int
  remainingStock)`; a refused one MUST publish none.
- **FR-007**: Tests for this item MUST be added under `src/test/java/dev/stockroom/`, and every
  existing test MUST keep passing unedited. No dependency may be added.

## Allowed paths

- `src/main/java/dev/stockroom/*`
- `src/main/resources/*`
- `src/test/java/dev/stockroom/*`

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every acceptance scenario above passes as an automated test under `mvn test`.
- **SC-002**: The 001–004 tests pass without being edited.

## Assumptions

- No existing test calls `/api/products/{id}/reservations`, so none needs to change.
- Tests share one in-memory database per Spring context; each test creates its own products.
