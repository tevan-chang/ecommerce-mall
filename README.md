# 電商購物中心系統（MVP）

商品建立、庫存清單查詢、建立訂單（含扣庫存與 Transaction）。Vue 3 + TypeScript 前端、Spring Boot 3（Java 17）後端、MySQL 8，以 Docker Compose（Nginx / app / db）部署。

## 架構

```
Browser (Vue 3)
   │ HTTP
Nginx ── 靜態資源 + /api reverse proxy + security headers
   │
Spring Boot (Java 17)
   │ JDBC（僅呼叫 Stored Procedure）
MySQL 8
```

| 服務 | 說明 | Port |
|---|---|---|
| `web` | Nginx：Vue build 後的靜態檔 + `/api` reverse proxy 到 `app` | `80` |
| `app` | Spring Boot API | `8080` |
| `db` | MySQL 8，`DB/` 掛載為 `initdb.d` 初始化 | `3307`（對外，容器內仍為 `3306`） |

分層（後端 `backend/src/main/java/com/demo/mall/`，依技術層分套件，不依功能分）：

| 分層 | 職責 |
|---|---|
| `controller/` | HTTP 綁定、`@Valid` 驗證、狀態碼，不含業務邏輯 |
| `service/` | 業務規則、Transaction 邊界、SQLState → 例外轉換 |
| `repository/` | 一個 method 對應一個 Stored Procedure，不寫 SQL 字串 |
| `dto/` | Request / Response record；Request 不含金額欄位 |
| `common/` | `ApiResponse`、`ErrorCode`、`BusinessException`、`GlobalExceptionHandler` |

## 啟動步驟

```bash
git clone <repo-url>
cd <repo>
cp .env.example .env   # 依需要調整 DB_ROOT_PASSWORD / DB_USER / DB_PASSWORD
docker compose up -d --build
```

啟動後：

- 前端：http://localhost
- 後端健康檢查：http://localhost:8080/actuator/health
- 完整走一次：開啟 http://localhost → 「新增商品」建立一筆商品 → 回到建立訂單頁勾選商品 → 送出 → 確認庫存更新

重置（含清空資料庫 volume，下次啟動會重跑 `DB/` 內的 seed）：

```bash
docker compose down -v
```

## API 範例

Base path：`/api/v1`（前端與 Nginx 皆以此為前綴）。

### 新增商品 `POST /products`

商品編號由後端自動產生（`sp_insert_product`，見 `docs/ADR.md` ADR-004），請求不帶 `productId`。

```bash
curl -s -X POST http://localhost/api/v1/products -H "Content-Type: application/json" \
  -d '{"productName":"測試商品","price":1500,"quantity":10}'
```

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": { "productId": "P004", "productName": "測試商品", "price": 1500, "quantity": 10 }
}
```

### 查詢有庫存商品 `GET /products?inStock=true`

```bash
curl -s "http://localhost/api/v1/products?inStock=true"
```

### 建立訂單 `POST /orders`

價格與總額一律由後端計算，請求不帶金額欄位。

```bash
curl -s -X POST http://localhost/api/v1/orders -H "Content-Type: application/json" \
  -d '{"memberId":"458","items":[{"productId":"P002","quantity":2}]}'
```

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "orderId": "Ms20261009000001",
    "memberId": "458",
    "totalPrice": 2400,
    "payStatus": 0,
    "items": [
      { "productId": "P002", "productName": "...", "quantity": 2, "standPrice": 1200, "itemPrice": 2400 }
    ]
  }
}
```

常見錯誤回應：

| 情境 | HTTP | `code` |
|---|---|---|
| 商品不存在 | 404 | `PRODUCT_NOT_FOUND` |
| 庫存不足 | 409 | `INSUFFICIENT_STOCK` |
| 商品編號重複 | 409 | `DUPLICATE_PRODUCT` |
| 請求內 `productId` 重複 | 400 | `VALIDATION_ERROR` |
| 商品名稱含 `<script>` 等不允許字元 | 400 | `VALIDATION_ERROR` |

所有回應皆為 `{ code, message, data }`；5xx 不含 stack trace 或 SQL（見 `GUARDRAILS.md` 規則 9）。

> 上方 curl 範例走 Nginx（`http://localhost`）；也可直接打後端（`http://localhost:8080/api/v1/...`），兩者路徑相同，差異僅在是否經過 Nginx 代理。

## 假設（A1~A9）

| # | 假設 |
|---|---|
| A1 | 幣別為 TWD 整數，`DECIMAL(12,0)`，不處理稅與折扣 |
| A2 | `MemberID` 由前端欄位輸入，後端只驗格式，不驗是否存在 |
| A3 | Seed 的 `quantity` 為目前庫存，不回推歷史訂單 |
| A4 | 新訂單 `PayStatus = 0`，付款流程不實作 |
| A5 | 價格與總額一律由後端計算，前端數字僅供顯示 |
| A6 | 請求內 `productId` 重複 → 回 400（不靜默合併） |
| A7 | `OrderID` 的 `yyyyMMdd` 固定使用 `Asia/Taipei`，由 Java 計算後傳入 SP |
| A8 | 「管理人員」新增商品為商品管理頁內的 Modal（非獨立頁面），不做認證授權（題目未要求） |
| A9 | 重複送單以前端「送出中按鈕 disabled」防連點，不保證網路層重送的冪等 |

## 測試

| # | 層級 | 工具 | 案例 | 執行指令 |
|---|---|---|---|---|
| 1~3 | DB / SP | Testcontainers (MySQL) | 庫存不足 `45002`、商品不存在 `45001`、`sp_next_order_id` 不重複 | `mvn test -Dtest='com.demo.mall.db.*Test'` |
| 4~5 | 整合 | `@SpringBootTest` + Testcontainers | 10 thread 搶最後 1 件恰好 1 成功；兩品項第二項不足時第一項不被扣 | `mvn test -Dtest='com.demo.mall.integration.*Test'` |
| 6~7 | Service | Mockito | 重複 `productId` → 400；依 `productId` 排序；`BigDecimal` 總額正確 | `mvn test -Dtest='com.demo.mall.service.*Test'` |
| 8~9 | Controller | **MockMvc 切片測試** | 驗證失敗 → 400；500 不洩漏 stack trace / SQL | `mvn test -Dtest='com.demo.mall.controller.*Test'` |

全部一次執行：

```bash
cd backend
mvn clean package
```

不做：前端單元測試、E2E、覆蓋率門檻；以上方「啟動步驟」手動驗證取代 E2E。

## 架構決策

重大設計決策（含替代方案與後果）記錄於 [`docs/ADR.md`](docs/ADR.md)：

- ADR-001：Transaction 只由 Spring 控制，SP 只做單一操作
- ADR-002：以條件式 UPDATE 防止超賣
- ADR-003：以 `order_seq` 表產生 OrderID，日期由應用層傳入
- ADR-004：以 `product_seq` 表產生 ProductID，伺服器端自動配號
- ADR-005：`memberId` 僅做格式驗證，不建立會員資料表

## 目前限制與未來擴充

以下皆為題目未要求、MVP 不實作的項目，附擴充設計草稿：

| 項目 | 目前狀態 | 擴充設計草稿 |
|---|---|---|
| 冪等 | 僅前端防連點（A9） | `Idempotency-Key` header + `orders.idempotency_key` UNIQUE；重送回傳原訂單（HTTP 200） |
| 授權 | 新增商品頁無認證（A8） | Spring Security，`ADMIN` role 保護 `POST /products` |
| 訂單查詢 | 無 | `GET /orders/{id}`，需驗證 `memberId` 避免序號被列舉 |
| 會員驗證 | 僅格式驗證，無 `member` 資料表（A2，見 `docs/ADR.md` ADR-005） | 建立 `member` 表（含唯一性約束）+ FK；`POST /orders` 於寫入前驗證會員存在；為未來 `GET /orders/{id}` 等端點提供可信的身份依據，而非僅格式比對 |
| 時區標準化 | OrderID 日期固定 `Asia/Taipei` | DB 與 JVM 統一 UTC，注入 `Clock` 並測試跨午夜 |
| DB 最小權限 | 應用帳號為 `MYSQL_USER` 預設權限 | 應用帳號僅授與 `EXECUTE`，SP 以 `DEFINER` 執行 |
| 資料庫遷移 | `initdb.d` 初始化 | 導入 Flyway / Liquibase |
| 商品清單 | 無分頁 | `page` / `size` 參數 |
| 可觀測性 | 預設 log | 結構化 log + `traceId`（MDC）、Actuator 指標 |
| 稽核 | 無 | Audit Log 資料表（誰、何時、做了什麼） |
| 濫用防護 | 無 | Rate Limit；單筆訂單品項數上限 |
| 庫存釋放 | 無 | 未付款逾時自動釋放庫存 |
| API 文件 | 本 README 範例 | springdoc OpenAPI |
| 前端 | composable + 簡單 CSS | Pinia、TanStack Query、Tailwind、Vitest |
| 測試 | 見上方「測試」章節 | Playwright E2E、覆蓋率門檻 |

## Guardrails

硬性禁止事項（金額計算、SP 存取、Transaction 邊界等 9 條）見 [`GUARDRAILS.md`](GUARDRAILS.md)。
