# Implementation Plan: Low-stock products

**Branch**: `011-low-stock` | **Date**: 2026-11-25 | **Spec**: [spec.md](spec.md)

## Summary

Add an optional `maxStock` query parameter to `GET /api/products`. Its presence selects the page
form, like `name`, `page` and `size`. A parameter that is present but empty (`maxStock=`) is
invalid, not absent (spec FR-002): Spring turns an empty value bound to `Integer` into `null`,
so the controller must tell "present but empty" from "not given", for example by reading the
parameter as a `String` and validating it, and answer `400` for an empty, negative or
non-integer value. The store's page query and its
count gain an optional `stock <= :maxStock` condition next to the escaped `name` condition (008),
so the filtering and counting stay in the database. Every invalid value answers the same `400`
Problem Detail as an invalid `page` or `size` (003, 006), through the existing
`ApiExceptionHandler`.

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
src/main/java/dev/stockroom/product/ProductController.java   (maxStock parameter; empty is a 400)
src/main/java/dev/stockroom/product/ProductStore.java        (findPage with an optional maxStock)
src/main/java/dev/stockroom/product/ProductRepository.java   (page and count queries with stock <= :maxStock)
src/test/java/dev/stockroom/product/ProductLowStockTests.java
```
