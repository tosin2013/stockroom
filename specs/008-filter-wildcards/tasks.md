# Tasks: The name filter matches wildcard characters literally

**Input**: `specs/008-filter-wildcards/` (spec.md, plan.md)

- [X] T001 [US1] [US2] [US3] Escape the trimmed filter text before matching: `\` becomes `\\`, then `%` becomes `\%` and `_` becomes `\_`, where the filter is passed to the repository (`src/main/java/dev/stockroom/product/`) (FR-001)
- [X] T002 [US1] [US2] [US3] In `src/main/java/dev/stockroom/product/ProductRepository.java`, add `ESCAPE '\'` to the `LIKE` in both the page query and the count query (FR-001, FR-003)
- [X] T003 Add tests in `src/test/java/dev/stockroom/product/ProductFilterWildcardTests.java` for every acceptance scenario and edge case, with the products' name prefixes from the spec (FR-004)
- [X] T004 Run `mvn test` and confirm the 001–007 tests pass unedited (FR-002, SC-002)
