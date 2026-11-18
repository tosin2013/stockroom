# Implementation Plan: Persistence

**Branch**: `004-persistence` | **Date**: 2026-11-18 | **Spec**: [spec.md](spec.md)

## Summary

A `schema.sql` (run by Spring Boot's SQL initialisation for the embedded database) creates
`product` with an identity `id`, `name VARCHAR(100) NOT NULL` and `stock INT NOT NULL CHECK
(stock >= 0)`. `ProductRepository extends ListCrudRepository<…, Long>` maps it with Spring Data
JDBC, either on the `Product` record (`@Id`, `@Table("product")`) or on a separate row record.
`ProductStore` keeps its public methods and delegates to the repository; `findAll()` sorts by
id. The controller, the 003 error handler and the existing tests don't change.

## Technical Context

**Language/Version**: Java 21 | **Framework**: Spring Boot 4.1.2 (`data-jdbc`, embedded H2)
**Testing**: JUnit 5, `@SpringBootTest` + MockMvc, `JdbcTemplate` for direct table checks,
`@DataJdbcTest` for the repository | **Build**: `mvn test`
**Constraints**: no new dependencies; tests offline; existing tests unchanged

## Constitution Check

I item 004, depends on 003 ✓ · II owner merges ✓ · III one PR with tests ✓ · IV no dependency
change (data-jdbc and H2 declared in planning) ✓ · V offline tests (in-memory H2) ✓ · VI no
planning files or CI touched ✓

## Project Structure

```text
src/main/resources/schema.sql
src/main/java/dev/stockroom/product/ProductRepository.java
src/main/java/dev/stockroom/product/Product.java        (mapping annotations, if mapped here)
src/main/java/dev/stockroom/product/ProductStore.java   (delegates to the repository)
src/test/java/dev/stockroom/product/ProductRepositoryTests.java
src/test/java/dev/stockroom/product/ProductPersistenceTests.java
```
