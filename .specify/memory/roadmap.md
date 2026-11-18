<!--
SYNC IMPACT REPORT
==================
Version change: none → 1.0.0
Bump rationale: initial roadmap.

Changes this revision:
  - Added specs 001–006 (001 and 002 specced; 003–006 planned)
  - Decisions recorded: C-01–C-04

Specs affected: 001, 002, 003, 004, 005, 006
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

### 003 — Problem Details errors  [status: planned]

- **Description:** Errors are RFC 9457 Problem Details.
- **Outcome:** validation failures return `400 application/problem+json` with per-field errors,
  and an unknown product id returns `404 application/problem+json`.
- **Scope (in):** error handling for the products API.
- **Depends on:** 002

### 004 — Persistence  [status: planned]

- **Description:** Products are stored in embedded H2 through Spring Data JDBC.
- **Outcome:** the products API behaves exactly as before, backed by a repository and a schema.
- **Scope (in):** schema, repository, wiring; repository tests.
- **Depends on:** 003

### 005 — Reservations  [status: planned]

- **Description:** Reserve stock for a product.
- **Outcome:** `POST /api/products/{id}/reservations` lowers stock, or answers `409` as a
  Problem Detail when stock is short, and publishes a `StockReserved` application event.
- **Scope (in):** the reservation endpoint, the event, tests.
- **Depends on:** 004

### 006 — Paging and filtering  [status: planned]

- **Description:** List products a page at a time, optionally filtered by name.
- **Outcome:** `GET /api/products?name=&page=&size=` returns a page with stable ordering.
- **Scope (in):** query parameters, paging, tests.
- **Depends on:** 004

---

## Open Questions

- none

---

**Version**: 1.0.0 | **Ratified**: 2026-11-18 | **Last Amended**: 2026-11-18
