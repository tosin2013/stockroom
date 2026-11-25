# Feature Specification: Low-stock products

**Feature Branch**: `011-low-stock`

**Created**: 2026-11-25

**Status**: Draft

**Input**: Roadmap item 011. A client lists the products that are running low, to know what to
reorder. Depends on 006 (paging and filtering), 008 (literal name filter) and 010 (setting the
stock). The first item built on a Mac by chalkeus (031).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - List the products running low (Priority: P1)

As a client, I ask for the products whose stock is at or below a number, and get only those, a
page at a time.

**Why this priority**: Without it, a client has to page through every product to find what to
reorder.

**Independent Test**: Create products with stock `0`, `3`, `5` and `9`, then list with
`maxStock=5`.

**Acceptance Scenarios**:

1. **Given** products with stock `0`, `3`, `5` and `9`, **When** a client calls
   `GET /api/products?maxStock=5`, **Then** the answer is `200` with a page (006) whose `content`
   is the products with stock `0`, `3` and `5` in ascending id order, and `totalElements` counts
   only those.
2. **Given** the same products, **When** the client calls `GET /api/products?maxStock=0`,
   **Then** `content` is only the product with stock `0`.
3. **Given** products none of which has stock at or below `n`, **When** the client lists with
   `maxStock=n`, **Then** the answer is `200` with an empty `content` and `totalElements` `0`.
4. **Given** a product with stock `9`, **When** its stock is set to `2` (010), or a reservation
   (005) brings it to `2`, **Then** `maxStock=5` now includes it; and a deleted product (009) is
   never included.

---

### User Story 2 - Combine with the name filter and paging (Priority: P1)

As a client, I narrow the low-stock list by name and read it a page at a time.

**Why this priority**: The low-stock list must behave like the rest of the list API.

**Independent Test**: Create several low-stock products with different names, then combine
`maxStock` with `name`, `page` and `size`.

**Acceptance Scenarios**:

1. **Given** low-stock products named `bolt-a`, `bolt-b` and `nut`, and a well-stocked `bolt-c`,
   **When** a client calls `GET /api/products?maxStock=5&name=bolt`, **Then** `content` is
   `bolt-a` and `bolt-b` only, with `totalElements` `2`.
2. **Given** five products at or below the limit, **When** a client calls
   `GET /api/products?maxStock=5&page=1&size=2`, **Then** `content` is the third and fourth of
   them in id order, `totalElements` is `5` and `totalPages` is `3`.
3. **Given** a name with `%` or `_`, **When** it is combined with `maxStock`, **Then** it still
   matches literally (008).

---

### User Story 3 - An invalid limit is refused (Priority: P1)

As a client, a wrong `maxStock` tells me what's wrong.

**Why this priority**: A bad parameter must never be read as "no limit".

**Independent Test**: Send each invalid value.

**Acceptance Scenarios**:

1. **Given** any products, **When** `maxStock` is `-1`, `1.5`, `x` or empty, **Then** the answer
   is `400` as a Problem Detail (003), as an invalid `page` or `size` is (006).

---

### Edge Cases

- `GET /api/products` with no parameters still answers the plain array of all products (002,
  006 FR-004), whatever the stock levels.
- A product exactly at the limit is included (`<=`, not `<`).
- Very large limits (for example `2147483647`) include every product.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: `GET /api/products` with `maxStock` MUST answer `200` with a page (006 FR-001)
  holding only the products whose stock is at most `maxStock`, in ascending id order, with
  `totalElements` and `totalPages` counting only those products.
- **FR-002**: `maxStock` MUST be an integer `>= 0`. A negative, non-integer, non-numeric or empty
  value MUST answer `400` as a Problem Detail (003 FR-001), as `page` and `size` do (006 FR-002).
- **FR-003**: `maxStock` MUST combine with `name` (006 FR-003, 008), `page` and `size`: a
  product is listed only when it matches every filter given.
- **FR-004**: Like `name`, `page` and `size`, `maxStock` alone MUST select the page form; with
  none of the four parameters the answer stays the plain array (006 FR-004).
- **FR-005**: The filtering and counting MUST happen in the database query, not by loading every
  product (006 FR-005).
- **FR-006**: Everything else MUST stay the same (001-010).
- **FR-007**: Tests for this item MUST be added under `src/test/java/dev/stockroom/`, and every
  existing test MUST keep passing unedited. No dependency may be added.

## Allowed paths

- `src/main/java/dev/stockroom/*`
- `src/test/java/dev/stockroom/*`

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every acceptance scenario above passes as an automated test under `mvn test`, and
  fails on the 010 code.
- **SC-002**: The 001-010 tests pass without being edited.

## Assumptions

- The stock filter is a `stock <= :maxStock` condition added to 006's page and count queries,
  next to the escaped `name` condition (008).
- Tests share one in-memory database per Spring context, so each test creates its own products
  and uses names unique to the test when it counts results.
