# Tasks: Update a product's stock

**Input**: `specs/010-update-stock/` (spec.md, plan.md)

- [X] T001 [US1] [US2] Add `UpdateStockRequest` in `src/main/java/dev/stockroom/product/UpdateStockRequest.java` with `@NotNull @PositiveOrZero Integer stock` (FR-001, FR-002)
- [X] T002 [US1] [US2] Add a conditional `UPDATE product SET stock = :stock WHERE id = :id` to `src/main/java/dev/stockroom/product/ProductRepository.java`, and `setStock(long id, int stock)` to `src/main/java/dev/stockroom/product/ProductStore.java` that throws `ProductNotFoundException` when no row changed and returns the product read back (FR-001, FR-003)
- [X] T003 [US1] [US2] Add `PATCH /api/products/{id}` to `src/main/java/dev/stockroom/product/ProductController.java`, validating the body and answering `200` with the product (FR-001, FR-002, FR-004)
- [X] T004 Add tests in `src/test/java/dev/stockroom/product/ProductStockUpdateTests.java` for every acceptance scenario and edge case (FR-006)
- [X] T005 Run `mvn test` and confirm the 001–009 tests pass unedited (FR-005, SC-002)
