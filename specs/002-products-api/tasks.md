# Tasks: Products API

**Input**: `specs/002-products-api/` (spec.md, plan.md)

- [ ] T001 [US1] Add records `Product(long id, String name, int stock)` and `CreateProductRequest(@NotBlank @Size(max = 100) String name, @NotNull @PositiveOrZero Integer stock)` in `src/main/java/dev/stockroom/product/` (FR-002, FR-006)
- [ ] T002 [US1] Add `ProductStore` in `src/main/java/dev/stockroom/product/`: increasing ids, concurrent-safe, creation order kept (FR-003, FR-006)
- [ ] T003 [US1] Add `ProductController` in `src/main/java/dev/stockroom/product/`: `POST /api/products` (trim the name; `201` + `Location`) (FR-001, FR-002)
- [ ] T004 [US3] Add `GET /api/products/{id}` (`200`/`404`) and `GET /api/products` (creation order) to `ProductController` (FR-004, FR-005)
- [ ] T005 [US2] Add tests in `src/test/java/dev/stockroom/product/ProductControllerTests.java` for every acceptance scenario of US1–US3 (FR-007)
- [ ] T006 In `src/test/java/dev/stockroom/StockroomApplicationTests.java`, change `productsEndpointDoesNotExistYet` to expect `200` from `GET /api/products` (rename it to match); keep its other tests unchanged (spec: Tests that may change)
- [ ] T007 Run `mvn test` and confirm the 001 tests still pass (SC-002)
