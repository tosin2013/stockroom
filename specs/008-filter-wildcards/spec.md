# Feature Specification: The name filter matches wildcard characters literally

**Feature Branch**: `008-filter-wildcards`

**Created**: 2026-11-19

**Status**: Draft

**Input**: Roadmap item 008, found in the owner's review of 006 (stockroom#33). The name filter
passes the client's text into a SQL `LIKE` pattern unescaped, so `%` and `_` act as wildcards:
`name=%` matches every product, and `name=a_b` also matches `axb`. 006's spec says `name` matches
products whose name *contains* the text, so these characters must match only themselves.
Depends on 006.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - `%` matches only a percent sign (Priority: P1)

As a client, when I filter by text containing `%`, I get only products whose name contains that
text, `%` included.

**Why this priority**: Today `name=%` returns the whole catalogue as a "match".

**Independent Test**: Products with and without `%` in their names; filter by text with `%`.

**Acceptance Scenarios**:

1. **Given** products `wq-100% cotton` and `wq-100 cotton`, **When** a client calls
   `GET /api/products?name=wq-100%25`, **Then** `content` holds only `wq-100% cotton`, and
   `totalElements` is `1`.
2. **Given** those products, **When** a client calls `GET /api/products?name=%25`, **Then**
   every product in `content` has `%` in its name, and `wq-100 cotton` is not among them.

---

### User Story 2 - `_` matches only an underscore (Priority: P1)

As a client, when I filter by text containing `_`, it matches an underscore and nothing else.

**Why this priority**: `_` is common in product codes (`SKU_12`), and today it matches any
character.

**Independent Test**: Products whose names differ only where one has `_`.

**Acceptance Scenarios**:

1. **Given** products `wu-sku_12` and `wu-skuX12`, **When** a client calls
   `GET /api/products?name=wu-sku_12`, **Then** `content` holds only `wu-sku_12`, and
   `totalElements` is `1`.

---

### User Story 3 - The escape character itself is literal (Priority: P2)

As a client, a backslash in my filter text matches a backslash.

**Why this priority**: Escaping usually works by giving `\` a special meaning, which must not
leak to clients either.

**Independent Test**: Products whose names differ only by a backslash.

**Acceptance Scenarios**:

1. **Given** products `wb-a\b` and `wb-ab`, **When** a client calls
   `GET /api/products?name=wb-a%5Cb`, **Then** `content` holds only `wb-a\b`.
2. **Given** products `wb-c\%d` and `wb-c%d`, **When** a client calls
   `GET /api/products?name=wb-c%5C%25d`, **Then** `content` holds only `wb-c\%d`.

---

### Edge Cases

- Matching still ignores case and still matches anywhere in the name: `name=WQ-100%25` finds
  `wq-100% cotton`.
- Paging is unchanged: `page`, `size`, `totalElements` and `totalPages` count only the literal
  matches.
- Text without `%`, `_` or `\` answers exactly as in 006.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: In the `name` filter, `%`, `_` and `\` MUST match only themselves, in both the
  page query and the count behind `totalElements` and `totalPages`.
- **FR-002**: Everything else about 006 MUST stay the same: trimming, ignoring case, matching
  anywhere in the name, ascending id order, paging, validation, and the plain array when no
  parameter is given.
- **FR-003**: The filtering and paging MUST stay in the database query (006 FR-005).
- **FR-004**: Tests for this item MUST be added under `src/test/java/dev/stockroom/`, and every
  existing test MUST keep passing unedited. No dependency may be added.

## Allowed paths

- `src/main/java/dev/stockroom/*`
- `src/test/java/dev/stockroom/*`

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every acceptance scenario above passes as an automated test under `mvn test`, and
  fails on the 006 code (scenario 1 of User Story 3 may pass on it, since a plain backslash
  matches itself there; scenario 2 fails).
- **SC-002**: The 001–007 tests pass without being edited.

## Assumptions

- Product names may contain `%`, `_` and `\` (002 limits only blankness and length).
- Tests share one in-memory database per Spring context, so each scenario's names start with
  its own prefix (`wq-`, `wu-`, `wb-`) to stay apart from other tests' products.
