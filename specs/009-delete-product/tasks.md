# Tasks: Delete a product

**Input**: `specs/009-delete-product/` (spec.md, plan.md)

- [X] T001 [US1] [US2] Add `delete(long id)` to `src/main/java/dev/stockroom/product/ProductStore.java`: delete the row, and throw `ProductNotFoundException` when no row was deleted (FR-001, FR-002)
- [X] T002 [US1] [US2] Add `DELETE /api/products/{id}` to `src/main/java/dev/stockroom/product/ProductController.java`, answering `204` with no body (FR-001)
- [X] T003 Add tests in `src/test/java/dev/stockroom/product/ProductDeleteTests.java` for every acceptance scenario and edge case of US1–US3 (FR-003, FR-005)
- [X] T004 Run `mvn test` and confirm the 001–008 tests pass unedited (FR-004, SC-002)
