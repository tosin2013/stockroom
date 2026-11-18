# Tasks: Reservations report the stock they left

**Input**: `specs/007-reservation-stock/` (spec.md, plan.md)

- [ ] T001 [US1] In `src/main/java/dev/stockroom/product/ProductStore.java`, make `reserve` run the conditional decrement first and then read the stock in the same transaction; use that figure for `Reservation.remainingStock` and `StockReserved.remainingStock` (FR-001)
- [ ] T002 [US3] In `src/main/java/dev/stockroom/product/ProductStore.java`, when the decrement updates nothing, read the product then: missing → `ProductNotFoundException`; present → `InsufficientStockException` with that stock as `available` (FR-002)
- [ ] T003 Add tests in `src/test/java/dev/stockroom/product/ReservationConcurrencyTests.java` for every acceptance scenario of US1–US3, collecting `StockReserved` events from all threads (FR-004)
- [ ] T004 Run `mvn test` and confirm the 001–005 tests pass unedited (FR-003, SC-002)
