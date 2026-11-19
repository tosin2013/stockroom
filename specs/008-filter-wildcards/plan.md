# Implementation Plan: The name filter matches wildcard characters literally

**Branch**: `008-filter-wildcards` | **Date**: 2026-11-19 | **Spec**: [spec.md](spec.md)

## Summary

Escape the filter text before it reaches `LIKE`: backslash first, then `%` and `_`, each
prefixed with `\`. Then declare that escape character in both repository queries
(`... LIKE ... ESCAPE '\'`). The page query and the count both use the escaped text. No other
behaviour changes.

## Technical Context

**Language/Version**: Java 21 | **Framework**: Spring Boot 4.1.2 (`data-jdbc`, embedded H2)
**Testing**: JUnit 5, `@SpringBootTest` + MockMvc | **Build**: `mvn test`
**Constraints**: no new dependencies; tests offline; existing tests unchanged

## Constitution Check

I item 008, depends on 006 ✓ · II owner merges ✓ · III one PR with tests ✓ · IV no dependency
change ✓ · V offline tests ✓ · VI no planning files or CI touched ✓

## Project Structure

```text
src/main/java/dev/stockroom/product/ProductRepository.java   (ESCAPE clause on both queries)
src/main/java/dev/stockroom/product/ProductStore.java        (escape the filter text)
src/test/java/dev/stockroom/product/ProductFilterWildcardTests.java
```
