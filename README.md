# Stockroom

A small inventory service in Java 21 and Spring Boot 4.1: a product catalogue with stock levels
and reservations, over a JSON API with clear errors.

It is also a **reference for greenfield, roadmap-driven development**. The repository started
empty. Everything in it traces back to a roadmap item, a spec and a pull request the owner
merged:

- [`.specify/memory/constitution.md`](.specify/memory/constitution.md): how decisions are made
  and what every change must meet.
- [`.specify/memory/roadmap.md`](.specify/memory/roadmap.md): what Stockroom will become, item by
  item, and each item's status.
- [`specs/`](specs/): one folder per item, with its spec, plan and tasks.

Items are built one at a time, each as one pull request with tests, by people or by agents; the
owner reviews and merges each one.

## Run it

```sh
mvn test              # the tests (offline once dependencies are downloaded)
mvn spring-boot:run   # then: curl localhost:8080/actuator/health
```

Requires Java 21 and Maven 3.9.

## Roadmap at a glance

| Item | | Status |
|---|---|---|
| 001 | Scaffold: starts, reports health | specced |
| 002 | Products API (in memory) | specced |
| 003 | Problem Details errors | planned |
| 004 | Persistence (Spring Data JDBC, embedded H2) | planned |
| 005 | Reservations | planned |
| 006 | Paging and filtering | planned |

The roadmap file is the record; this table is a summary.
