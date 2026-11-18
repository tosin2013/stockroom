# Implementation Plan: Reservations

**Branch**: `005-reservations` | **Date**: 2026-11-18 | **Spec**: [spec.md](spec.md)

## Summary

`ReservationController` (or a method on `ProductController`) takes a validated
`ReservationRequest(@NotNull @Positive Integer quantity)`. The store runs one conditional update,
`UPDATE product SET stock = stock - :q WHERE id = :id AND stock >= :q` (a `@Modifying @Query` on
`ProductRepository`), in a transaction. One row updated: read the new stock, publish
`StockReserved` through `ApplicationEventPublisher`, answer `200`. No row: `404` if the product
doesn't exist, otherwise `409` via an `InsufficientStockException` that `ApiExceptionHandler`
maps to a Problem Detail with `requested` and `available`.

## Technical Context

**Language/Version**: Java 21 | **Framework**: Spring Boot 4.1.2 (`webmvc`, `validation`,
`data-jdbc`, embedded H2) | **Testing**: JUnit 5, `@SpringBootTest` + MockMvc,
`@RecordApplicationEvents`, an `ExecutorService` for the concurrency test | **Build**: `mvn test`
**Constraints**: no new dependencies; tests offline; existing tests unchanged

## Constitution Check

I item 005, depends on 004 ✓ · II owner merges ✓ · III one PR with tests ✓ · IV no dependency
change ✓ · V offline tests ✓ · VI no planning files or CI touched ✓

## Project Structure

```text
src/main/java/dev/stockroom/product/{ReservationRequest,Reservation,StockReserved,InsufficientStockException}.java
src/main/java/dev/stockroom/product/{ProductRepository,ProductStore,ProductController}.java  (reserve)
src/main/java/dev/stockroom/ApiExceptionHandler.java   (409)
src/test/java/dev/stockroom/product/ReservationTests.java
```
