# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

An e-commerce mall interview assignment (2-day delivery scope): product creation, in-stock product listing,
and order creation with stock deduction. Three-tier architecture — Vue 3 + TypeScript frontend, Spring Boot 3
(Java 17) backend, MySQL 8 — deployed via Docker Compose (Nginx / app / db).

Governing documents, in order of authority for anything not covered below:

- `GUARDRAILS.md` — hard constraints, violation of any one blocks delivery. Tracked in git.
- `docs/ADR.md` — architecture decisions with alternatives/consequences. Tracked in git.
- `spec-v3.1.md` — full product spec (requirements traceability, API contracts, DB schema, SP signatures,
  validation rules, test case catalogue). **Gitignored** (contains the original assignment text) but present
  locally — read it for anything spec-related.
- `roadmap.md` — phase-by-phase task/DoD checklist with checkboxes kept up to date as work completes.
  **Gitignored**, local planning doc only.

Current status: Phase 1 (DB layer) and Phase 2 (backend API) are done and committed. Phase 3 (integration/
concurrency tests), Phase 4 (frontend), Phase 5 (containerization + docs) are not yet started — `frontend/`
and `nginx/` are empty.

## Commands

All backend commands run from `backend/`.

```bash
# Start the DB only (Nginx/app services don't exist until Phase 5)
docker compose up -d db          # from repo root
docker compose down -v           # full reset incl. volume; re-runs DB/*.sql seed on next up

# Build + run all tests (unit, slice, and Testcontainers DB/integration tests)
mvn clean package

# Run one test class / one test method
mvn test -Dtest=OrderServiceTest
mvn test -Dtest=OrderServiceTest#itemsAreProcessedInProductIdOrder_andTotalIsSumOfItemPrices

# Run only one layer of tests
mvn test -Dtest='com.demo.mall.db.*Test'                              # SP behavior (Testcontainers MySQL)
mvn test -Dtest='com.demo.mall.service.*Test,com.demo.mall.controller.*Test'  # Mockito + MockMvc

# Run the app locally against the docker-compose db (mapped to host port 3307)
DB_HOST=localhost DB_PORT=3307 DB_USER=mall_app DB_PASSWORD=<from .env> \
  java -jar target/mall-0.0.1-SNAPSHOT.jar
curl -s http://localhost:8080/actuator/health
```

**Windows/Docker Desktop Testcontainers gotcha**: if DB/integration tests fail immediately with
`Could not find a valid Docker environment` / a 400 response with an all-empty `Info` body, it's a
protocol mismatch between Docker Desktop's npipe proxy and the docker-java version Testcontainers bundles —
the plain `docker` CLI and `docker compose` are unaffected, so don't be misled into thinking the daemon
itself is unreachable. Fix (needed once per machine): create `~/.testcontainers.properties` with
`docker.host=npipe:////./pipe/dockerDesktopLinuxEngine` (match whatever `docker context ls` shows as the
current context's endpoint) and make sure `backend/pom.xml`'s `testcontainers.version` is current (bumping
1.19.8 → 1.21.4 fixed it here). Both changes are required together.

## Architecture

### Backend package layout (`backend/src/main/java/com/demo/mall/`)

Packages are organized **by technical layer**, not by feature — this is a hard requirement (see
`GUARDRAILS.md` rule 6 and the self-check greps below), not a stylistic choice:

- `controller/` — HTTP binding, `@Valid` request validation, status codes. No business logic.
- `service/` — business rules, transaction boundaries, SQLState→exception translation. `OrderService`
  and `ProductService`.
- `repository/` — one method per stored procedure call via `JdbcTemplate`/`CallableStatement`. **Never**
  build SQL strings here — only `{call sp_xxx(...)}` invocations. `ProductRecord` is an internal
  repository-to-service row type (not a DTO).
- `dto/request/`, `dto/response/` — Java records with Bean Validation annotations. Request DTOs never
  carry money fields (price/total) — the server always computes and owns amounts.
- `common/` — `ApiResponse<T>` (uniform `{code, message, data}` envelope), `ErrorCode` (maps to HTTP
  status), `BusinessException`, `GlobalExceptionHandler` (`@RestControllerAdvice`; generic 500 message only,
  never leaks stack traces or SQL), `SqlStateUtils` (walks the cause chain to pull `SQLState` out of a
  `DataAccessException`), `TransactionConfig` (exposes a `TransactionTemplate` bean).

### Database access pattern

All six stored procedures live in `DB/02_stored_procedures.sql` (fixed set, don't add a 7th — Phase 1's DoD
checks `SHOW PROCEDURE STATUS` returns exactly 6). Repositories call them via `{call sp_xxx(?,...)}` using
`JdbcTemplate`. For procedures with `OUT` parameters, `JdbcTemplate.call(CallableStatementCreator, List<SqlParameter>)`
requires the **full positional parameter list including IN params** (not just the OUT ones) — the extraction
logic indexes by position in that list, so a partial list silently reads the wrong column and throws
`Parameter number N is not an OUT parameter` only on the success path (error paths SIGNAL before extraction
runs, so this bug hides behind passing error-case tests). See `ProductRepository.deductStock` /
`OrderRepository.nextOrderId` for the correct pattern.

SQLState → business meaning: `45001` product not found, `45002` insufficient stock, `45003` duplicate
product. Services catch `DataAccessException`, extract the SQLState via `SqlStateUtils.extract`, and throw
the matching `BusinessException`.

### Order creation flow (`OrderService.createOrder`)

1. Validate request (dup `productId` in one order → 400) **before** opening a transaction.
2. Pre-fetch available products (`sp_get_available_products`) once, outside the transaction, purely to get
   `productName` for the response — product names aren't returned by `sp_deduct_stock`, and this lookup is
   safe because any item that successfully deducts must have had stock > 0 when fetched.
3. Sort items by `productId` before touching stock — fixed lock order prevents deadlocks across concurrent
   multi-item orders (ADR-002).
4. Inside one `TransactionTemplate.execute` block: deduct stock per item (throws on insufficient/not-found,
   rolling back everything including earlier successful deductions in the same order), compute `itemPrice`/
   `total` in `BigDecimal`, generate the order ID as the **last** step (`sp_next_order_id`, called with a
   `yyyyMMdd` date computed in Java using `Asia/Taipei` — not DB time, see ADR-003), then insert order +
   order details.
5. Transactions are controlled only by Spring's `TransactionTemplate`, never inside a stored procedure
   (ADR-001) — `@Transactional` is deliberately avoided due to self-invocation pitfalls in this call shape.

### Hard constraints (full list in `GUARDRAILS.md`)

These are correctness/security invariants, not style preferences — violating any one is a delivery blocker:

- Money fields are always `BigDecimal`/`DECIMAL`, never `double`/`float`.
- Stock deduction is a single conditional `UPDATE ... WHERE quantity >= ?`; never `SELECT` then `UPDATE`.
- Request DTOs never contain price/total fields — server always computes amounts.
- Repositories contain no SQL string literals (`SELECT`/`INSERT INTO`/`UPDATE ... SET`/`DELETE FROM`) and
  stored procedures contain no `START TRANSACTION`/`COMMIT`/`ROLLBACK`/dynamic SQL — enforced by the grep
  self-checks in `GUARDRAILS.md`.
- 500 responses never include stack traces or SQL text (see `GlobalExceptionHandler`).
- `.env` never gets committed; only `.env.example` ships.
