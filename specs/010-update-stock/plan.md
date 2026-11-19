# Implementation Plan: Update a product's stock

**Branch**: `010-update-stock` | **Date**: 2026-11-19 | **Spec**: [spec.md](spec.md)

## Summary

Add `PATCH /api/products/{id}`, which takes a small request record with a validated `stock`
(`@NotNull @PositiveOrZero Integer`). A new store method sets the stock with one `UPDATE`
statement and throws `ProductNotFoundException` when no row changed. It then returns the product
read back. The existing `ApiExceptionHandler` gives the `400` and `404` Problem Details.

## Technical Context

**Language/Version**: Java 21 | **Framework**: Spring Boot 4.1.2 (`webmvc`, `validation`,
`data-jdbc`, embedded H2)
**Testing**: JUnit 5, `@SpringBootTest` + MockMvc | **Build**: `mvn test`
**Constraints**: no new dependencies; tests offline; existing tests unchanged

## Constitution Check

I item 010, depends on 003, 004 and 009 ✓ · II owner merges ✓ · III one PR with tests ✓ · IV no
dependency change ✓ · V offline tests ✓ · VI no planning files or CI touched ✓

## Project Structure

The new test file is the build worker's own; its name is chosen here, so the hidden tests use
another (specfoundry's `DRAFT_PATH_IN_SPEC` rule).

```text
src/main/java/dev/stockroom/product/ProductController.java   (PATCH /{id})
src/main/java/dev/stockroom/product/UpdateStockRequest.java  (validated body)
src/main/java/dev/stockroom/product/ProductStore.java        (setStock: one UPDATE, 404 if none)
src/main/java/dev/stockroom/product/ProductRepository.java   (the UPDATE query)
src/test/java/dev/stockroom/product/ProductStockUpdateTests.java
```
