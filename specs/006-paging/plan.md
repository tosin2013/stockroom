# Implementation Plan: Paging and filtering

**Branch**: `006-paging` | **Date**: 2026-11-18 | **Spec**: [spec.md](spec.md)

## Summary

A second handler on `GET /api/products`, selected when any of `name`, `page`, `size` is present
(`@GetMapping(params = …)`, or one handler that branches on them). Validated `@RequestParam`s
(`@Min(0)`, `@Min(1) @Max(100)`) give `400` through the 003 handler. `ProductRepository` adds
`findByNameContainingIgnoreCase(String, Pageable)` and `countByNameContainingIgnoreCase`
(or `ListPagingAndSortingRepository` for the unfiltered case), sorted by id. The response is a
`ProductPage` record with the five fields.

## Technical Context

**Language/Version**: Java 21 | **Framework**: Spring Boot 4.1.2 (`webmvc`, `validation`,
`data-jdbc`, embedded H2) | **Testing**: JUnit 5, `@SpringBootTest` + MockMvc, `JdbcTemplate` to
clear the table | **Build**: `mvn test`
**Constraints**: no new dependencies; tests offline; existing tests unchanged

## Constitution Check

I item 006, depends on 004 ✓ · II owner merges ✓ · III one PR with tests ✓ · IV no dependency
change ✓ · V offline tests ✓ · VI no planning files or CI touched ✓

## Project Structure

```text
src/main/java/dev/stockroom/product/ProductPage.java
src/main/java/dev/stockroom/product/{ProductRepository,ProductStore,ProductController}.java
src/test/java/dev/stockroom/product/ProductPagingTests.java
```
