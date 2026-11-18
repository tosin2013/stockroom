# Implementation Plan: Problem Details errors

**Branch**: `003-problem-details` | **Date**: 2026-11-18 | **Spec**: [spec.md](spec.md)

## Summary

A `@RestControllerAdvice` in `dev.stockroom` that extends Spring's
`ResponseEntityExceptionHandler`, so the framework's own errors (validation, unreadable body,
type mismatch) already come back as `ProblemDetail`. It adds the `errors` list to validation
failures, sets `instance` to the request path, and maps a new `ProductNotFoundException` to
`404`. The controller throws that exception instead of building an empty `404`.

## Technical Context

**Language/Version**: Java 21 | **Framework**: Spring Boot 4.1.2 (`webmvc`, `validation`)
**Testing**: JUnit 5, `@SpringBootTest` + MockMvc | **Build**: `mvn test`
**Constraints**: no new dependencies; tests offline; existing tests unchanged

## Constitution Check

I item 003, depends on 002 ✓ · II owner merges ✓ · III one PR with tests ✓ · IV no dependency
change ✓ · V offline tests ✓ · VI no planning files or CI touched ✓

## Project Structure

```text
src/main/java/dev/stockroom/ApiExceptionHandler.java
src/main/java/dev/stockroom/product/ProductNotFoundException.java
src/main/java/dev/stockroom/product/ProductController.java   (404 via the exception)
src/test/java/dev/stockroom/product/ProductErrorTests.java
```
