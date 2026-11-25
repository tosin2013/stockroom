<!--
SYNC IMPACT REPORT
==================
Version change: 1.4.0 → 1.5.0
Bump rationale: MINOR, a new planned spec (011), specced in the same change.

Changes this revision:
  - Added 011 (Low-stock products), specced in specs/011-low-stock/

Earlier revisions:
  - 1.4.0: added 010 (Update a product's stock), specced
  - 1.3.0: added 009 (Delete a product), specced
  - 1.2.0: added 008 (The name filter matches wildcard characters literally), specced
  - 1.1.0: added 007 (Reservations report the stock they left), specced
  - 1.0.3: 005 and 006 specced (specs/005-reservations/, specs/006-paging/)
  - 1.0.2: 004 specced (specs/004-persistence/)
  - 1.0.1: 003 specced (specs/003-problem-details/)
  - 1.0.0: added specs 001–006 (001 and 002 specced; 003–006 planned); decisions C-01–C-04

Specs affected: 011
Open questions added/resolved: none

Notes: drafted by the controller with the owner (planning PR); the owner's merge ratifies it.
Concept and dependency choices come from a dated survey of current Java practice (JetBrains
State of Java 2025, Spring Boot release data, start.spring.io), recorded in the planning PR.
-->

# Stockroom — Spec Roadmap

Living, non-binding map of the specs planned for Stockroom. It is **not a commitment to order or
scope**; specs are scoped and clarified when they are actually started. Foundations: the
project [constitution](constitution.md).

Status legend (lifecycle): **undecided** · **needs-info** · **planned** ·
**specced** · **in-progress** · **implemented** · **verified** · **deferred** ·
**abandoned**.

---

## Vision & End States

- A small, idiomatic Spring Boot 4 inventory service: a product catalogue with stock levels and
  reservations, over a JSON API with clear errors.
- A reference for greenfield, roadmap-driven development: every line traces to a roadmap item,
  a spec, and a pull request the owner merged.

## Constraints & Decisions

- **C-01 — Current, mainstream Java:** Java 21 and Spring Boot 4.1, using only start.spring.io
  starters (`webmvc`, `validation`, `actuator`, `data-jdbc`, embedded H2). Mainstream choices
  make the project a useful reference.
- **C-02 — Offline, deterministic tests:** every test runs under `mvn test` with no network,
  Docker or external service (constitution V).
- **C-03 — Dependencies approved up front:** `pom.xml` declares what every item below needs; an
  item never adds a dependency (constitution IV).
- **C-04 — Planning files and CI are the owner's:** no item's allowed paths include the
  constitution, roadmap, specs, `pom.xml` or `.github/` (constitution VI).

## Planned Specs

### 001 — Scaffold  [status: verified]

- **Description:** The application starts, reports its health, and has its first test.
- **Outcome:** `GET /actuator/health` answers `{"status":"UP"}`, only the health endpoint is
  exposed, virtual threads are on, and `mvn test` runs the project's own tests.
- **Scope (in):** the application class; `application.properties`; a first test.
- **Scope (out):** any business endpoint (002 onward); dependencies (C-03); CI (C-04).
- **Governed by:** C-01, C-02, C-03, C-04
- **Spec dir:** specs/001-scaffold/

### 002 — Products API  [status: verified]

- **Description:** Create, read and list products held in memory.
- **Outcome:** `POST /api/products` creates a product with a validated name and stock level;
  `GET /api/products/{id}` and `GET /api/products` return them.
- **Scope (in):** request and response records, validation, an in-memory store, controller
  tests.
- **Scope (out):** error body format (003); persistence (004); reservations (005); paging (006).
- **Depends on:** 001
- **Governed by:** C-01, C-02, C-03, C-04
- **Spec dir:** specs/002-products-api/

### 003 — Problem Details errors  [status: verified]

- **Description:** Errors are RFC 9457 Problem Details.
- **Outcome:** validation failures return `400 application/problem+json` with per-field errors,
  and an unknown product id returns `404 application/problem+json`.
- **Scope (in):** error handling for the products API.
- **Scope (out):** per-error `type` URIs; error formats outside `/api/products`.
- **Depends on:** 002
- **Governed by:** C-01, C-02, C-03, C-04
- **Spec dir:** specs/003-problem-details/

### 004 — Persistence  [status: verified]

- **Description:** Products are stored in embedded H2 through Spring Data JDBC.
- **Outcome:** the products API behaves exactly as before, backed by a repository and a schema.
- **Scope (in):** schema, repository, wiring; repository tests.
- **Scope (out):** a file or server database; migrations tooling; the H2 console.
- **Depends on:** 003
- **Governed by:** C-01, C-02, C-03, C-04
- **Spec dir:** specs/004-persistence/

### 005 — Reservations  [status: verified]

- **Description:** Reserve stock for a product.
- **Outcome:** `POST /api/products/{id}/reservations` lowers stock, or answers `409` as a
  Problem Detail when stock is short, and publishes a `StockReserved` application event.
- **Scope (in):** the reservation endpoint, the event, tests.
- **Scope (out):** storing reservations as records; releasing or expiring them.
- **Depends on:** 004
- **Governed by:** C-01, C-02, C-03, C-04
- **Spec dir:** specs/005-reservations/

### 006 — Paging and filtering  [status: verified]

- **Description:** List products a page at a time, optionally filtered by name.
- **Outcome:** `GET /api/products?name=&page=&size=` returns a page with stable ordering.
- **Scope (in):** query parameters, paging, tests.
- **Scope (out):** changing the plain list (no parameters stays an array); sorting options.
- **Depends on:** 004
- **Governed by:** C-01, C-02, C-03, C-04
- **Spec dir:** specs/006-paging/

### 007 — Reservations report the stock they left  [status: verified]

- **Description:** A reservation's `remainingStock` (and a refusal's `available`) is the stock
  right after its own update, even under concurrent reservations.
- **Outcome:** concurrent reservations each report a distinct, correct `remainingStock`, in the
  response and the `StockReserved` event; `409` reports the stock as it really is.
- **Scope (in):** the reservation logic in `ProductStore`; concurrency tests.
- **Scope (out):** any change to the endpoint's shape, status codes or the atomic decrement.
- **Depends on:** 005
- **Governed by:** C-01, C-02, C-03, C-04
- **Spec dir:** specs/007-reservation-stock/
- **Notes:** found in the owner's review of 005's PR (stockroom#25), merged as is: stock was
  never oversold, but the reported figure came from a read taken before the update. 005's hidden
  tests counted status codes and the final stock, so they did not catch it.

### 008 — The name filter matches wildcard characters literally  [status: verified]

- **Description:** `%`, `_` and `\` in the `name` filter match only themselves.
- **Outcome:** `GET /api/products?name=%` no longer matches every product, and `name=a_b` no
  longer matches `axb`; the page and its counts include only literal matches.
- **Scope (in):** escaping the filter text and the `LIKE` queries; tests.
- **Scope (out):** any other change to paging or filtering (006).
- **Depends on:** 006
- **Governed by:** C-01, C-02, C-03, C-04
- **Spec dir:** specs/008-filter-wildcards/
- **Notes:** found in the owner's review of 006's PR (stockroom#33), merged as is: the filter
  text went into the `LIKE` pattern unescaped. 006's hidden tests used plain text, so they did
  not catch it.

### 009 — Delete a product  [status: verified]

- **Description:** A client removes a product it no longer stocks.
- **Outcome:** `DELETE /api/products/{id}` answers `204` and the product is gone from every read
  and reservation; an unknown id answers `404` as a Problem Detail.
- **Scope (in):** the endpoint, the store method, tests.
- **Scope (out):** soft delete, undo, bulk delete.
- **Depends on:** 003, 004
- **Governed by:** C-01, C-02, C-03, C-04
- **Spec dir:** specs/009-delete-product/
- **Notes:** the first item whose hidden tests are drafted automatically by specfoundry's test
  author (specfoundry 024); the owner still reviews and merges them.


### 010 — Update a product's stock  [status: verified]

- **Description:** A client sets a product's stock after a delivery or a count.
- **Outcome:** `PATCH /api/products/{id}` with `{"stock": n}` sets the stock (`n >= 0`) and
  answers `200` with the product; invalid bodies answer `400` and unknown ids `404` as Problem
  Details, changing nothing.
- **Scope (in):** the endpoint, its validated body, the store method, tests.
- **Scope (out):** changing a product's name; relative adjustments (`+n`/`-n`); bulk updates.
- **Depends on:** 003, 004, 009
- **Governed by:** C-01, C-02, C-03, C-04
- **Spec dir:** specs/010-update-stock/
- **Notes:** the first item carried end to end by specfoundry's step mode (specfoundry 021),
  hidden tests drafted automatically (024); the owner still reviews and merges every PR.

### 011 — Low-stock products  [status: specced]

- **Description:** A client lists the products that are running low, to know what to reorder.
- **Outcome:** `GET /api/products?maxStock=n` returns, as a page (006), only the products whose
  stock is at most `n`, combinable with `name`, `page` and `size`; an invalid `maxStock`
  answers `400` as a Problem Detail.
- **Scope (in):** the query parameter, the filtered and counted queries, tests.
- **Scope (out):** a minimum filter; sorting by stock; per-product reorder levels; the plain
  list without parameters.
- **Depends on:** 006, 008, 010
- **Governed by:** C-01, C-02, C-03, C-04
- **Spec dir:** specs/011-low-stock/
- **Notes:** the first item built on a Mac (Apple Silicon, Docker Desktop) by chalkeus 031; its
  hidden tests come from a separate test-author session, and the owner reviews and merges
  every PR.
---

## Open Questions

- none

---

**Version**: 1.5.0 | **Ratified**: 2026-11-18 | **Last Amended**: 2026-11-25
