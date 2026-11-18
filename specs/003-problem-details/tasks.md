# Tasks: Problem Details errors

**Input**: `specs/003-problem-details/` (spec.md, plan.md)

- [X] T001 [US2] Add `ProductNotFoundException(long id)` in `src/main/java/dev/stockroom/product/`, and make `GET /api/products/{id}` in `ProductController` throw it when the product is missing (FR-003)
- [X] T002 [US1] Add `ApiExceptionHandler` (`@RestControllerAdvice`, extends `ResponseEntityExceptionHandler`) in `src/main/java/dev/stockroom/`: `status`, `title`, non-empty `detail` and `instance` (request path) on every error; `errors` (one `{field, message}` per invalid field, sorted by `field`) on validation failures; `404` for `ProductNotFoundException` with the id in `detail` (FR-001, FR-002, FR-003)
- [X] T003 [US3] Make sure malformed JSON and a non-numeric `{id}` answer `400` Problem Details through the same handler (FR-004)
- [X] T004 Add tests in `src/test/java/dev/stockroom/product/ProductErrorTests.java` for every acceptance scenario of US1–US3, including `Content-Type: application/problem+json` (FR-006)
- [X] T005 Run `mvn test` and confirm the 001 and 002 tests pass unedited (FR-005, SC-002)
