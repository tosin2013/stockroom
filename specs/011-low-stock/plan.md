# Implementation Plan: Low-stock products

**Branch**: `011-low-stock` | **Date**: 2026-11-25 | **Spec**: [spec.md](spec.md)

## Summary

Add an optional `maxStock` query parameter (`@Min(0) Integer`) to `GET /api/products`. Its
presence selects the page form, like `name`, `page` and `size`. The store's page query and its
count gain an optional `stock <= :maxStock` condition next to the escaped `name` condition (008),
so the filtering and counting stay in the database. The existing `ApiExceptionHandler` gives the
`400` Problem Detail for an invalid value, as it does for `page` and `size`.

## Technical Context

**Language/Version**: Java 21 | **Framework**: Spring Boot 4.1.2 (`webmvc`, `validation`,
`data-jdbc`, embedded H2)
**Testing**: JUnit 5, `@SpringBootTest` + MockMvc | **Build**: `mvn test`
**Constraints**: no new dependencies; tests offline; existing tests unchanged

## Constitution Check

I item 011, depends on 006, 008 and 010 ✓ · II owner merges ✓ · III one PR with tests ✓ · IV no
dependency change ✓ · V offline tests ✓ · VI no planning files or CI touched ✓

## Project Structure

The new test file is the build worker's own; its name is chosen here, so the hidden tests use
another (chalkeus's `DRAFT_PATH_IN_SPEC` rule).

```text
src/main/java/dev/stockroom/product/ProductController.java   (maxStock parameter)
src/main/java/dev/stockroom/product/ProductStore.java        (findPage with an optional maxStock)
src/main/java/dev/stockroom/product/ProductRepository.java   (page and count queries with stock <= :maxStock)
src/test/java/dev/stockroom/product/ProductLowStockTests.java
```
