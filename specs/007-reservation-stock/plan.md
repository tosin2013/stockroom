# Implementation Plan: Reservations report the stock they left

**Branch**: `007-reservation-stock` | **Date**: 2026-11-18 | **Spec**: [spec.md](spec.md)

## Summary

In `ProductStore.reserve` (already `@Transactional`), run the conditional decrement first. If it
updated a row, read the product's stock again in the same transaction and use that figure for
the `Reservation` and the `StockReserved` event. If it updated nothing, read the product: missing
→ `404`; present → `409` with that freshly read stock as `available`. Drop the read taken before
the update.

## Technical Context

**Language/Version**: Java 21 | **Framework**: Spring Boot 4.1.2 (`data-jdbc`, embedded H2)
**Testing**: JUnit 5, `@SpringBootTest` + MockMvc, an `ExecutorService` for concurrency, a test
`@EventListener` that collects events from every thread | **Build**: `mvn test`
**Constraints**: no new dependencies; tests offline; existing tests unchanged

## Constitution Check

I item 007, depends on 005 ✓ · II owner merges ✓ · III one PR with tests ✓ · IV no dependency
change ✓ · V offline tests ✓ · VI no planning files or CI touched ✓

## Project Structure

```text
src/main/java/dev/stockroom/product/ProductStore.java   (reserve: update, then read)
src/test/java/dev/stockroom/product/ReservationConcurrencyTests.java
```
