# Implementation Plan: Delete a product

**Branch**: `009-delete-product` | **Date**: 2026-11-19 | **Spec**: [spec.md](spec.md)

## Summary

Add `DELETE /api/products/{id}` to `ProductController`, backed by a new
`ProductStore.delete(long id)`. The store throws `ProductNotFoundException` when no row was
deleted, and the existing `ApiExceptionHandler` turns that into a `404` Problem Detail. Reads and
reservations already answer `404` for a missing row, so nothing else changes.

## Technical Context

**Language/Version**: Java 21 | **Framework**: Spring Boot 4.1.2 (`data-jdbc`, embedded H2)
**Testing**: JUnit 5, `@SpringBootTest` + MockMvc | **Build**: `mvn test`
**Constraints**: no new dependencies; tests offline; existing tests unchanged

## Constitution Check

I item 009, depends on 003 and 004 ✓ · II owner merges ✓ · III one PR with tests ✓ · IV no
dependency change ✓ · V offline tests ✓ · VI no planning files or CI touched ✓

## Project Structure

```text
src/main/java/dev/stockroom/product/ProductController.java   (DELETE /{id} → 204)
src/main/java/dev/stockroom/product/ProductStore.java        (delete: 404 when nothing deleted)
src/test/java/dev/stockroom/product/ProductDeleteTests.java
```
