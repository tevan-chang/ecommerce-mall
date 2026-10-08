# GUARDRAILS

電商購物中心系統的硬性禁止事項。每條皆為「違反即不可交付」，不分等級。
需求與流程請見 `spec-v3.1.md`，設計理由請見 `docs/ADR.md`。

1. **金額一律由後端計算。** Request DTO 不得包含價格、小計、總額欄位。
2. **資料庫只能透過 Stored Procedure + 參數化存取。** 禁止字串拼接 SQL；SP 內禁止動態 SQL（`PREPARE` / `EXECUTE`）。
3. **扣庫存必須是單一條件式 `UPDATE ... WHERE quantity >= ?`。** 禁止先 `SELECT` 再 `UPDATE`。
4. **Transaction 只由 Spring `TransactionTemplate` 控制。** SP 內禁止 `START TRANSACTION` / `COMMIT` / `ROLLBACK`。
5. **金額禁用 `float` / `double`。** 一律使用 `BigDecimal` / `DECIMAL`。
6. **分層不得跨越。** Controller 不含業務邏輯、Repository 不含 SQL 字串、前端不得直連資料庫（含 BaaS）。
7. **前端禁用 `v-html`、`innerHTML`、`eval`。**
8. **密碼不得進入 Git。** 只走 `.env`，並提供 `.env.example`。
9. **錯誤回應不得洩漏 stack trace 或 SQL。**

---

## 自動檢查（選用，4 行）

```bash
grep -rniE "start transaction|commit|rollback|prepare |execute " DB/02_stored_procedures.sql && echo "FAIL: 規則 2/4"
grep -rniE "select |insert into|update .* set|delete from" backend/src/main/java/com/demo/mall/repository && echo "FAIL: 規則 2/6"
grep -rnE "\b(double|float)\b" backend/src/main && echo "FAIL: 規則 5"
grep -rniE "v-html|innerHTML|eval\(" frontend/src && echo "FAIL: 規則 7"
```

規則 1、3 由測試案例 4、5、7 覆蓋；規則 8 由 `git ls-files | grep -E "(^|/)\.env$"` 無輸出確認；規則 9 由測試案例 9 覆蓋。

「不實作範圍外功能」與「交付前於乾淨環境驗證」不屬於 Guardrail，分別列於 `spec-v3.1.md` §2 / §13 與 `roadmap.md` 的 DoD。
