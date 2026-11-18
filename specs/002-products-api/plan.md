# Implementation Plan: Products API

**Branch**: `002-products-api` | **Date**: 2026-11-18 | **Spec**: [spec.md](spec.md)

## Summary

A `ProductController` under `dev.stockroom.product` with `CreateProductRequest` (validated
record), `Product` (record), and an in-memory `ProductStore` (an `AtomicLong` for ids and a
concurrent, insertion-ordered map). Controller tests with MockMvc.

## Technical Context

**Language/Version**: Java 21 | **Framework**: Spring Boot 4.1.2 (`webmvc`, `validation`)
**Testing**: JUnit 5, `@WebMvcTest` or `@SpringBootTest` + MockMvc | **Build**: `mvn test`
**Constraints**: no new dependencies; tests offline

## Constitution Check

I item 002, depends on 001 ✓ · II owner merges ✓ · III one PR with tests ✓ · IV no dependency
change ✓ · V offline tests ✓ · VI no planning files or CI touched ✓

## Project Structure

```text
src/main/java/dev/stockroom/product/{Product,CreateProductRequest,ProductStore,ProductController}.java
src/test/java/dev/stockroom/product/ProductControllerTests.java
```
