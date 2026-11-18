# Implementation Plan: Scaffold

**Branch**: `001-scaffold` | **Date**: 2026-11-18 | **Spec**: [spec.md](spec.md)

## Summary

Add the Spring Boot application class, `application.properties` (name, virtual threads, health
only), and a first test that starts the context and checks the health endpoint with MockMvc.

## Technical Context

**Language/Version**: Java 21 | **Framework**: Spring Boot 4.1.2 (parent in `pom.xml`)
**Testing**: JUnit 5, `@SpringBootTest` with `@AutoConfigureMockMvc` | **Build**: Maven (`mvn test`)
**Constraints**: no new dependencies; tests offline (constitution IV, V)

## Constitution Check

I roadmap item 001 ✓ · II owner merges ✓ · III one PR with tests ✓ · IV no dependency change ✓ ·
V offline tests ✓ · VI no planning files or CI touched ✓

## Project Structure

```text
src/main/java/dev/stockroom/StockroomApplication.java
src/main/resources/application.properties
src/test/java/dev/stockroom/StockroomApplicationTests.java
```
