# Tasks: Scaffold

**Input**: `specs/001-scaffold/` (spec.md, plan.md)

- [X] T001 [US1] Create `src/main/java/dev/stockroom/StockroomApplication.java` (`@SpringBootApplication`, `main`) (FR-001)
- [X] T002 [US1] Create `src/main/resources/application.properties`: `spring.application.name=stockroom`, `spring.threads.virtual.enabled=true`, `management.endpoints.web.exposure.include=health` (FR-003, FR-004)
- [X] T003 [US1] Add `src/test/java/dev/stockroom/StockroomApplicationTests.java`: the context starts; `GET /actuator/health` returns `200` with `status` `UP` (FR-002, FR-005)
- [X] T004 [US2] Run `mvn test` and confirm it passes (SC-001)
