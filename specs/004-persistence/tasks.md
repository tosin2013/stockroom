# Tasks: Persistence

**Input**: `specs/004-persistence/` (spec.md, plan.md)

- [X] T001 [US1] Add `src/main/resources/schema.sql` creating table `product`: `id` identity primary key, `name VARCHAR(100) NOT NULL`, `stock INT NOT NULL CHECK (stock >= 0)` (FR-001)
- [X] T002 [US1] Add `ProductRepository` (Spring Data JDBC, `ListCrudRepository`) in `src/main/java/dev/stockroom/product/`, with the mapping for table `product` (FR-002)
- [X] T003 [US3] Back `ProductStore` in `src/main/java/dev/stockroom/product/ProductStore.java` with `ProductRepository`, keeping `save(String, int)`, `findById(long)` and `findAll()` and their return types; `findAll()` in ascending id order (FR-003, FR-004)
- [X] T004 [US2] Add repository tests in `src/test/java/dev/stockroom/product/ProductRepositoryTests.java`: save and find, ids increase, and the table refuses a null name, a 101-character name and negative stock (FR-001, FR-006)
- [X] T005 [US1] Add tests in `src/test/java/dev/stockroom/product/ProductPersistenceTests.java`: a product posted through the API is a row in `product`, and a row inserted directly is returned by the API (FR-002, FR-004)
- [X] T006 Run `mvn test` and confirm the 001, 002 and 003 tests pass unedited (FR-004, SC-002)
