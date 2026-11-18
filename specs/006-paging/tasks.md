# Tasks: Paging and filtering

**Input**: `specs/006-paging/` (spec.md, plan.md)

- [ ] T001 [US1] Add record `ProductPage(List<Product> content, int page, int size, long totalElements, int totalPages)` in `src/main/java/dev/stockroom/product/` (FR-001)
- [ ] T002 [US2] Add paged, case-insensitive name queries ordered by id to `ProductRepository`, and a `findPage(String name, int page, int size)` to `ProductStore`, in `src/main/java/dev/stockroom/product/` (FR-001, FR-003, FR-005)
- [ ] T003 [US1] In `src/main/java/dev/stockroom/product/ProductController.java`, answer `GET /api/products` with a `ProductPage` when `name`, `page` or `size` is present, and the plain array otherwise (FR-001, FR-004)
- [ ] T004 [US4] Validate `page` (`>= 0`, default 0) and `size` (1–100, default 20) so bad values answer `400` Problem Details through `src/main/java/dev/stockroom/ApiExceptionHandler.java` (FR-002)
- [ ] T005 Add tests in `src/test/java/dev/stockroom/product/ProductPagingTests.java` for every acceptance scenario of US1–US4 (FR-006)
- [ ] T006 Run `mvn test` and confirm the earlier items' tests pass unedited (SC-002)
