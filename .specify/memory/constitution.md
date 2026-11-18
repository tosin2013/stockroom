# Stockroom Constitution

Stockroom is a small inventory service, built item by item from its roadmap. It is also a
reference: every change is planned first, approved by the owner, and traceable to a roadmap
item. Agents may draft and build; the owner decides.

## Core Principles

### I. The roadmap decides what gets built

`.specify/memory/roadmap.md` is the record of what Stockroom will become and in what order.
Work starts only on a roadmap item whose spec and tasks exist and whose dependencies are
`verified`. A change that no item asks for is not made.

### II. The owner decides, in writing

The owner and the controller (the agent driving the work) plan together: the controller may
draft the roadmap, specs and tasks; the owner reviews, edits and merges them. Merging is the
approval. An item becomes `verified` only when the owner merges the change that says so.

### III. Every change arrives by pull request, with tests

Each change is one pull request for one roadmap item, titled in
[Conventional Commits](https://www.conventionalcommits.org/) form. It adds or updates tests
for what it changes, keeps every existing test passing, and passes CI. The owner merges; no
one pushes to `main`.

### IV. Dependencies are approved in planning

`pom.xml` declares every dependency the roadmap needs, and it changes only through a planning
pull request the owner merges. An item never adds a dependency.

### V. Tests are deterministic and offline

Tests run with `mvn test` and need no network, no Docker and no external service: embedded H2
for persistence, in-process HTTP with MockMvc. A test that fails sometimes is a defect.

### VI. Planning files and CI belong to the owner

The constitution, the roadmap, specs, `pom.xml` and `.github/` change only through owner
pull requests. An item's allowed paths never include them.

## Technology

Java 21, Spring Boot 4.1 (`webmvc`, `validation`, `actuator`, `data-jdbc`), embedded H2, JUnit 5,
Maven. Records for request and response bodies; RFC 9457 Problem Details for errors; virtual
threads on.

## Governance

This constitution outranks other practices. Amendments are owner pull requests that state the
reason, update the version below, and record the change at the top of this file.

**Version**: 1.0.0 | **Ratified**: 2026-11-18 | **Last Amended**: 2026-11-18
