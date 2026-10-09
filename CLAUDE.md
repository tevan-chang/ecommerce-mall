# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview
An e-commerce mall interview assignment (2-day delivery scope): product creation, in-stock product listing, and order creation with stock deduction. Three-tier architecture — Vue 3 + TypeScript frontend, Spring Boot 3 (Java 17) backend, MySQL 8 — deployed via Docker Compose (Nginx / app / db).

Governing documents, in order of authority for anything not covered below:

- `GUARDRAILS.md` — hard constraints, violation of any one blocks delivery. Tracked in git.
- `docs/ADR.md` — architecture decisions with alternatives/consequences. Tracked in git.
- `spec-v3.1.md` — full product spec (requirements traceability, API contracts, DB schema, SP signatures,
  validation rules, test case catalogue). **Gitignored** (contains the original assignment text) but present
  locally — read it for anything spec-related.
- `roadmap.md` — phase-by-phase task/DoD checklist with checkboxes kept up to date as work completes.
  **Gitignored**, local planning doc only.

Current status: Phase 1~5 complete (DB layer, backend API, integration/concurrency tests, frontend, containerization + README). Clean-clone acceptance (`git clone` → `cp .env.example .env` → `docker compose up -d --build`) has passed — from here on, bug fixes only, no new features (see `roadmap.md` 緩衝 phase).

## Commands
Backend commands run from `backend/`; frontend commands run from `frontend/`.

```bash
# Full stack (from repo root)
docker compose up -d --build     # db + app + web; fresh-volume cold start can take ~2-3min (db DDL+seed) before app/web report healthy — not stuck
docker compose down -v           # full reset incl. volume; re-runs DB/*.sql seed on next up
# DB only (for local dev running backend/frontend outside Docker)
docker compose up -d db          # from repo root
# Backend tests
mvn clean package                                                     # all layers
mvn test -Dtest='com.demo.mall.db.*Test'                              # SP behavior (Testcontainers MySQL)
mvn test -Dtest='com.demo.mall.service.*Test,com.demo.mall.controller.*Test'  # Mockito + MockMvc
# Frontend
npm ci
npm run dev       # Vite dev server on :5173, proxies /api to :8080
npm run build     # vue-tsc -b && vite build
```

**前端慣例**：API 呼叫一律集中在 `src/api/`；禁用 `v-html`、`innerHTML`、`eval`；金額一律由後端計算，前端不送 `price`/`total` 欄位，畫面金額僅供試算顯示。

**Testcontainers（換機器時的一次性設定）**：DB 層測試立即失敗通常是 Docker Desktop npipe 代理相容性問題，非 daemon 不可達，建 `~/.testcontainers.properties`（`docker.host=npipe:////./pipe/dockerDesktopLinuxEngine`，依 `docker context ls` 調整）；`testcontainers.version` 已固定 `1.21.4`，此問題已解決。換新機器第一次執行 `mvn test`/`mvn clean package` 時，Testcontainers 需 pull MySQL image 並冷啟動 InnoDB，實測可能耗時數分鐘（非卡住），之後的執行會快很多。

## Architecture
### Backend package layout (`backend/src/main/java/com/demo/mall/`)
Organized **by technical layer**, not by feature — hard requirement, see `GUARDRAILS.md` rule 6:

- `controller/` — HTTP binding, `@Valid` validation, status codes. No business logic.
- `service/` — business rules, transaction boundaries, SQLState→exception translation.
- `repository/` — one method per SP call via `JdbcTemplate`/`CallableStatement`; never build SQL strings.
- `dto/request|response/` — Java records with Bean Validation; request DTOs never carry money fields.
- `common/` — `ApiResponse<T>`, `ErrorCode`, `BusinessException`, `GlobalExceptionHandler` (generic 500, no stack trace/SQL leak), `SqlStateUtils`, `TransactionConfig` (`TransactionTemplate` bean).

### Database access pattern
Six fixed stored procedures in `DB/02_stored_procedures.sql` — don't add a 7th (Phase 1 DoD checks `SHOW PROCEDURE STATUS` = 6; `sp_insert_product` now generates `product_id` via `product_seq` instead of taking it as an IN param, ADR-004). Repositories call them via `{call sp_xxx(?,...)}`. **OUT-param gotcha**: `JdbcTemplate.call(CallableStatementCreator, List<SqlParameter>)` needs the full positional parameter list including IN params — extraction indexes by position, so a partial list silently reads the wrong column and only throws on the success path (error paths SIGNAL before extraction runs). See `ProductRepository.insertProduct`/`deductStock` or `OrderRepository.nextOrderId`.

SQLState → business meaning: `45001` not found, `45002` insufficient stock, `45003` duplicate. Services extract it via `SqlStateUtils.extract` and throw the matching `BusinessException`.

### Order creation flow (`OrderService.createOrder`) — see ADR-001/002/003 for full rationale
1. Validate request (dup `productId` → 400) before opening a transaction.
2. Pre-fetch all products (`ProductRepository.findAllProducts`) once, purely for `productName` lookup.
3. Sort items by `productId` before touching stock — fixed lock order avoids deadlocks (ADR-002).
4. Inside one `TransactionTemplate.execute`: deduct stock per item, compute `itemPrice`/`total` in `BigDecimal`, generate order ID last (`sp_next_order_id`, Java `Asia/Taipei` date, ADR-003), insert.
5. Transactions only via Spring's `TransactionTemplate`, never inside a SP (ADR-001) — `@Transactional` deliberately avoided due to self-invocation pitfalls in this call shape.

## 工作流程
Phase 5 只做容器化、README 與交付驗證，不再新增功能；題目規格未要求的功能一律不做。交付前以乾淨 `git clone` 跑一次 `docker compose up -d --build` 驗收整站可一鍵啟動。

## Guardrail 自檢
完整 9 條規則見 `GUARDRAILS.md`，違反任一條即不可交付。底部附的自動檢查：

```bash
grep -rniE "start transaction|commit|rollback|prepare |execute " DB/02_stored_procedures.sql && echo "FAIL: 規則 2/4"
grep -rniE "select |insert into|update .* set|delete from" backend/src/main/java/com/demo/mall/repository && echo "FAIL: 規則 2/6"
grep -rnE "\b(double|float)\b" backend/src/main && echo "FAIL: 規則 5"
grep -rniE "v-html|innerHTML|eval\(" frontend/src && echo "FAIL: 規則 7"
```

規則 1、3 由測試案例覆蓋；規則 8 由 `git ls-files | grep -E "(^|/)\.env$"` 無輸出確認；規則 9 由測試案例覆蓋。
