# Tasks: Low-stock products

**Input**: `specs/011-low-stock/` (spec.md, plan.md)

- [ ] T001 [US1] [US2] Add an optional `stock <= :maxStock` condition to the page query and its count in `src/main/java/dev/stockroom/product/ProductRepository.java`, next to the escaped `name` condition (FR-001, FR-003, FR-005)
- [ ] T002 [US1] [US2] Pass an optional `maxStock` through `findPage` in `src/main/java/dev/stockroom/product/ProductStore.java` (FR-001, FR-003)
- [ ] T003 [US1] [US3] Add the `@RequestParam(required = false) @Min(0) Integer maxStock` parameter to `GET /api/products` in `src/main/java/dev/stockroom/product/ProductController.java`; its presence selects the page form (FR-002, FR-004)
- [ ] T004 Add tests in `src/test/java/dev/stockroom/product/ProductLowStockTests.java` for every acceptance scenario and edge case (FR-007)
- [ ] T005 Run `mvn test` and confirm the 001–010 tests pass unedited (FR-006, SC-002)
