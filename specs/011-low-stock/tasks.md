# Tasks: Low-stock products

**Input**: `specs/011-low-stock/` (spec.md, plan.md)

- [X] T001 [US1] [US2] Add an optional `stock <= :maxStock` condition to the page query and its count in `src/main/java/dev/stockroom/product/ProductRepository.java`, next to the escaped `name` condition (FR-001, FR-003, FR-005)
- [X] T002 [US1] [US2] Pass an optional `maxStock` through `findPage` in `src/main/java/dev/stockroom/product/ProductStore.java` (FR-001, FR-003)
- [X] T003 [US1] [US3] Add an optional `maxStock` parameter to `GET /api/products` in `src/main/java/dev/stockroom/product/ProductController.java`; its presence selects the page form. An empty value (`maxStock=`) is present, not absent: answer `400` for an empty, negative or non-integer value, as an invalid `page` or `size` answers (a plain `@RequestParam(required = false) Integer` would turn the empty value into "not given") (FR-002, FR-004)
- [X] T004 Add tests in `src/test/java/dev/stockroom/product/ProductLowStockTests.java` for every acceptance scenario and edge case (FR-007)
- [X] T005 Run `mvn test` and confirm the 001–010 tests pass unedited (FR-006, SC-002)
