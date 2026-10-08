# Architecture Decision Records

電商購物中心系統的重大設計決策。格式：背景 → 決策 → 替代方案 → 後果。
禁止事項請見 `GUARDRAILS.md`，需求與流程請見 `spec-v3.1.md`。

| ADR | 標題 | 狀態 |
|---|---|---|
| 001 | Transaction 只由 Spring 控制，SP 只做單一操作 | Accepted |
| 002 | 以條件式 UPDATE 防止超賣 | Accepted |
| 003 | 以 `order_seq` 表產生 OrderID，日期由應用層傳入 | Accepted |

---

## ADR-001：Transaction 只由 Spring 控制，SP 只做單一操作

- **狀態**：Accepted（2026-10-09）
- **背景**：題目要求以 Stored Procedure 存取資料庫，同時要求「業務層」與「多表異動需 Transaction」。若 SP 內自行開關 Transaction，會與 Spring 管理的 Transaction 衝突（MySQL 的隱式 commit 會使外層失效）。
- **決策**：SP 僅做單一資料操作（扣庫存、寫主檔、寫明細），不含 `START TRANSACTION` / `COMMIT` / `ROLLBACK`。業務流程與 Transaction 邊界位於 Service 層，以 `TransactionTemplate` 控制。Repository 使用 `JdbcTemplate` 呼叫 SP，不使用 JPA。
- **替代方案**：整包邏輯放進 `sp_create_order`（業務層被架空，Service 變成貧血層）；使用 `@Transactional`（同類別 self-invocation 會失效）。
- **後果**：+ 邊界單一、可用 Mockito 測 Service；+ 業務規則可讀。− 一次下單需多次 DB 往返，MVP 規模可接受。
- **相關規則**：GUARDRAILS 4、6。

## ADR-002：以條件式 UPDATE 防止超賣

- **狀態**：Accepted（2026-10-09）
- **背景**：多人同時購買同一商品時，需保證庫存不為負、不超賣。
- **決策**：`UPDATE product SET quantity = quantity - ? WHERE product_id = ? AND quantity >= ?`，以 `ROW_COUNT()` 判斷是否扣成功；多商品依 `productId` 排序後處理，避免 deadlock。
- **替代方案**：`SELECT ... FOR UPDATE` 後再更新（鎖持有較久，且需先讀後寫）；樂觀鎖 `version` 欄位（衝突時需重試，MVP 複雜度較高）。
- **後果**：+ 單一語句即原子；+ 無需額外欄位。− 熱門商品的列鎖會序列化，極高併發需另行設計（如分段庫存、佇列）。
- **相關規則**：GUARDRAILS 3。

## ADR-003：以 `order_seq` 表產生 OrderID，日期由應用層傳入

- **狀態**：Accepted（2026-10-09）
- **背景**：OrderID 格式為 `Ms + yyyyMMdd + 6 碼流水號`，併發下不得重複，且日期應為使用者的當地日期。MySQL 沒有 SEQUENCE，且容器預設時區為 UTC，台北 00:00~08:00 下的單若用 DB 時間，日期會落在前一天。
- **決策**：`order_seq(seq_date, last_seq)` 搭配 `INSERT ... ON DUPLICATE KEY UPDATE` 遞增，於 Transaction 最後一步呼叫以縮短列鎖時間。日期由 Java 以固定的 `Asia/Taipei` 計算後，作為參數傳入 `sp_next_order_id`。
- **替代方案**：`MAX(order_id)+1`（race condition）；隨機 6 碼（有碰撞機率需重試）；UUID（不符題目格式）；全環境設為 `Asia/Taipei` 或 SP 內 `CONVERT_TZ`（依賴部署環境設定）。
- **後果**：+ 無重複、每日自動歸零、不依賴 DB 時區設定。− 同日訂單在 `order_seq` 該列序列化，高 TPS 需改用分段或外部序號服務；時區固定為 `Asia/Taipei`，多時區需另行設計。
