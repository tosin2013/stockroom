# Tasks: Reservations

**Input**: `specs/005-reservations/` (spec.md, plan.md)

- [ ] T001 [US1] Add records `ReservationRequest(@NotNull @Positive Integer quantity)`, `Reservation(long productId, int quantity, int remainingStock)` and `StockReserved(long productId, int quantity, int remainingStock)` in `src/main/java/dev/stockroom/product/` (FR-001, FR-002, FR-006)
- [ ] T002 [US3] Add an atomic conditional decrement to `ProductRepository` and a transactional `reserve(long id, int quantity)` to `ProductStore` in `src/main/java/dev/stockroom/product/` (FR-002, FR-005)
- [ ] T003 [US1] Add `POST /api/products/{id}/reservations` in `src/main/java/dev/stockroom/product/`: `200` with `Reservation`, and publish `StockReserved` only on success (FR-002, FR-006)
- [ ] T004 [US2] Add `InsufficientStockException` in `src/main/java/dev/stockroom/product/` and map it in `src/main/java/dev/stockroom/ApiExceptionHandler.java` to `409` with `requested` and `available`; unknown product answers `404` (FR-003, FR-004)
- [ ] T005 Add tests in `src/test/java/dev/stockroom/product/ReservationTests.java` for every acceptance scenario of US1–US4, including the 20-client concurrency test (FR-007)
- [ ] T006 Run `mvn test` and confirm the 001–004 tests pass unedited (SC-002)
