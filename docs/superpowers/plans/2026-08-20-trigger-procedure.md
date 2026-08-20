# Trigger + Stored Procedure Extension — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add one trigger and one stored procedure to the marketplace DB, wire the procedure into the running Java app, and correct every place in the project's own docs/report that currently (and now falsely) claims triggers/procedures are never used.

**Architecture:** Pure MySQL/MariaDB DDL in a new `db/routines.sql`, loaded after `views.sql`. One `VendorFrame.java` line swaps a raw `SELECT` for a `CALL` to the new procedure. Everything else is prose edits to keep README/CODE-GUIDE/DEMO-GUIDE/report consistent with the code.

**Tech Stack:** MariaDB 10.4 (via XAMPP), Java 8 + JDBC (mysql-connector-j 8.0.33), no test framework in this repo — verification is running the SQL against the live `marketplace_db` and building/running the Swing app.

## Global Constraints

- This project is **not a git repo** (confirmed at session start) — no commit steps; ignore the "Commit" step pattern from the skill template.
- Live DB access confirmed: `/c/xampp/mysql/bin/mysql.exe -u root marketplace_db` works, seeded (`vendor`=38 rows, `order_item`=2338 rows), zero existing triggers/procedures.
- The stock-deduct logic in `CheckoutService.java` (`deductStock()`, `app/src/marketplace/CheckoutService.java:259-301`) must NOT be duplicated by the new trigger — the trigger only validates, never mutates `inventory`.
- Every doc edit must stay factually true after the code change — no task is "done" if it leaves a stale "no triggers" claim anywhere in the project.
- Spec: `docs/superpowers/specs/2026-08-20-trigger-procedure-design.md`.

---

### Task 1: `db/routines.sql` — trigger + procedure

**Files:**
- Create: `db/routines.sql`
- Modify: `db/schema.sql:7` (run-order comment)
- Modify: `db/queries.sql:6-8` (drop the now-false blanket claim)

**Interfaces:**
- Produces: MySQL trigger `trg_order_item_stock_guard` (fires `BEFORE INSERT ON order_item`), MySQL procedure `sp_vendor_sales_report(IN p_vendor_id INT)` returning columns in this exact order: `gross_revenue, orders_count, units_sold, platform_commission, vendor_earning, business_name, commission_rate`. Task 2 depends on this column order.

- [ ] **Step 1: Create `db/routines.sql`**

```sql
-- =====================================================================
-- CSE 311: Database Management System
-- Project: Multi-Vendor E-Commerce Marketplace
-- File   : routines.sql   (run AFTER views.sql, BEFORE queries.sql)
--
-- Week 8/9 material: CREATE TRIGGER, CREATE PROCEDURE.
-- Deliberately scoped to two objects instead of moving every Table 7.1
-- rule into the database -- see the project report, Section 7.1, for
-- which rules stay in Java and why.
-- =====================================================================

USE marketplace_db;

DROP TRIGGER IF EXISTS trg_order_item_stock_guard;
DROP PROCEDURE IF EXISTS sp_vendor_sales_report;


-- ---------------------------------------------------------------------
-- TRIGGER: trg_order_item_stock_guard
--
-- Backstops the "stock must never be oversold" rule (report Table 7.1)
-- at the database level. CheckoutService.java already checks stock
-- before inserting order_item rows and deducts inventory afterwards in
-- the same transaction; this trigger does NOT touch inventory itself
-- -- if it did, stock would be deducted twice. It only refuses an
-- order_item insert that asks for more units than total_stock has, so
-- it is a pure safety net, not a replacement for the Java check.
-- ---------------------------------------------------------------------
DELIMITER $$

CREATE TRIGGER trg_order_item_stock_guard
BEFORE INSERT ON order_item
FOR EACH ROW
BEGIN
    DECLARE available INT;

    SELECT IFNULL(SUM(quantity), 0) INTO available
    FROM   inventory
    WHERE  variant_id = NEW.variant_id;

    IF available < NEW.quantity THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Insufficient stock for this variant';
    END IF;
END$$

DELIMITER ;


-- ---------------------------------------------------------------------
-- PROCEDURE: sp_vendor_sales_report
--
-- Parameterised wrapper around vw_vendor_sales (views.sql). Column
-- order matches what VendorFrame.java's overview screen reads:
-- gross_revenue, orders_count, units_sold first (see Task 2).
-- ---------------------------------------------------------------------
DELIMITER $$

CREATE PROCEDURE sp_vendor_sales_report(IN p_vendor_id INT)
BEGIN
    SELECT gross_revenue, orders_count, units_sold,
           platform_commission, vendor_earning, business_name, commission_rate
    FROM   vw_vendor_sales
    WHERE  vendor_id = p_vendor_id;
END$$

DELIMITER ;


-- =====================================================================
-- DEMO: run by hand (or via ReportsFrame) to see both objects fire, in
-- the same style as the Week 9 lab's Bank_Account and show_dept demos.
-- =====================================================================

-- Demo 1: the trigger accepts an in-stock line, then rejects one asking
-- for more units than exist. Pick a real variant first:
--   SELECT variant_id, total_stock FROM vw_product_catalog LIMIT 5;
-- then, with a real order_id from `SELECT order_id FROM customer_order LIMIT 1`:
--
-- INSERT INTO order_item (order_id, variant_id, quantity, unit_price)
-- VALUES (<order_id>, <variant_id>, 1, 10.00);          -- succeeds
--
-- INSERT INTO order_item (order_id, variant_id, quantity, unit_price)
-- VALUES (<order_id>, <variant_id>, 999999, 10.00);     -- rejected:
--   ERROR 1644 (45000): Insufficient stock for this variant

-- Demo 2: the procedure, called the way the Week 9 lab calls show_dept(70).
-- CALL sp_vendor_sales_report(<any user_id from SELECT user_id FROM vendor LIMIT 5>);

-- =====================================================================
-- END OF ROUTINES
-- =====================================================================
```

- [ ] **Step 2: Update the run-order comment in `db/schema.sql`**

Read `db/schema.sql:1-10` first to confirm the line is still `-- Run order: schema.sql -> seed.sql -> views.sql -> queries.sql`, then:

```
OLD: -- Run order: schema.sql -> seed.sql -> views.sql -> queries.sql
NEW: -- Run order: schema.sql -> seed.sql -> views.sql -> routines.sql -> queries.sql
```

- [ ] **Step 3: Fix the false blanket claim in `db/queries.sql`**

```
OLD:
-- Every query below is restricted to the SQL taught in CSE 311L
-- Weeks 1-7. No triggers, no stored procedures, no window functions
-- and no common table expressions are used anywhere in this project.

NEW:
-- Every query in THIS FILE is restricted to the SQL taught in CSE 311L
-- Weeks 1-7. No triggers, no stored procedures, no window functions and
-- no common table expressions appear here. A trigger and a stored
-- procedure ARE used elsewhere in the project -- see db/routines.sql,
-- which covers the Week 9 material and runs before this file.
```

- [ ] **Step 4: Load it into the live database and verify**

Run:
```bash
/c/xampp/mysql/bin/mysql.exe -u root marketplace_db < "db/routines.sql"
```
Expected: no error output.

Verify both objects registered:
```bash
/c/xampp/mysql/bin/mysql.exe -u root -e "SHOW TRIGGERS FROM marketplace_db LIKE 'order_item'; SHOW PROCEDURE STATUS WHERE Db='marketplace_db';"
```
Expected: one row naming `trg_order_item_stock_guard`, one row naming `sp_vendor_sales_report`.

- [ ] **Step 5: Verify the trigger actually blocks an oversell**

```bash
/c/xampp/mysql/bin/mysql.exe -u root marketplace_db -e "
SELECT pv.variant_id, IFNULL(SUM(i.quantity),0) AS stock, oi.order_id
FROM product_variant pv
LEFT JOIN inventory i ON i.variant_id = pv.variant_id
JOIN order_item oi ON 1=1
GROUP BY pv.variant_id, oi.order_id
LIMIT 1;"
```
Note the `variant_id`, `stock` and an `order_id` from the output, then:
```bash
/c/xampp/mysql/bin/mysql.exe -u root marketplace_db -e "
INSERT INTO order_item (order_id, variant_id, quantity, unit_price)
VALUES (<order_id>, <variant_id>, 999999, 10.00);"
```
Expected: `ERROR 1644 (45000) at line 2: Insufficient stock for this variant`. If it instead succeeds or errors with a different message, stop — the trigger is wrong, do not proceed to Task 2.

- [ ] **Step 6: Verify the procedure returns the same numbers as the view**

```bash
/c/xampp/mysql/bin/mysql.exe -u root marketplace_db -e "
SELECT user_id FROM vendor LIMIT 1;"
```
Take that `user_id`, then compare:
```bash
/c/xampp/mysql/bin/mysql.exe -u root marketplace_db -e "
SELECT gross_revenue, orders_count, units_sold FROM vw_vendor_sales WHERE vendor_id = <user_id>;
CALL sp_vendor_sales_report(<user_id>);"
```
Expected: both queries print the same `gross_revenue`/`orders_count`/`units_sold` values.

No commit step (not a git repo).

---

### Task 2: Wire the procedure into `VendorFrame.java`

**Files:**
- Modify: `app/src/marketplace/VendorFrame.java:95-98`

**Interfaces:**
- Consumes: `sp_vendor_sales_report(IN p_vendor_id INT)` from Task 1, returning `gross_revenue, orders_count, units_sold, ...` in that column order.
- Consumes: the existing `row(Connection c, String sql, int id, int cols)` helper already defined at `VendorFrame.java:258-278` — unchanged, just called with a different SQL string.

- [ ] **Step 1: Read the current method to confirm line numbers**

Read `app/src/marketplace/VendorFrame.java:81-109` (the `refreshOverview()` method) and confirm it still contains:

```java
        try (Connection c = Db.open()) {
            String[] r = row(c,
                "SELECT IFNULL(gross_revenue,0), IFNULL(orders_count,0), IFNULL(units_sold,0) "
              + "FROM vw_vendor_sales WHERE vendor_id = ?", vendorId, 3);
```

- [ ] **Step 2: Swap the raw view query for the procedure call**

```
OLD:
        try (Connection c = Db.open()) {
            String[] r = row(c,
                "SELECT IFNULL(gross_revenue,0), IFNULL(orders_count,0), IFNULL(units_sold,0) "
              + "FROM vw_vendor_sales WHERE vendor_id = ?", vendorId, 3);

NEW:
        try (Connection c = Db.open()) {
            // sp_vendor_sales_report (db/routines.sql) wraps vw_vendor_sales; column
            // order there is gross_revenue, orders_count, units_sold first, matching
            // what row() reads below.
            String[] r = row(c, "{call sp_vendor_sales_report(?)}", vendorId, 3);
```

Leave everything else in `refreshOverview()` (the `setStat(...)` calls, the `low` stock query) untouched.

- [ ] **Step 3: Build**

```bash
cd "app" && cmd /c build.bat
```
Expected: `Build complete. Start the application with run.bat` with no `Compilation failed.` line. If it fails, read the compiler error — a common cause here would be a stray character from the edit; re-check Step 2's exact text against the file.

- [ ] **Step 4: Run and manually verify**

```bash
cd "app" && cmd /c run.bat
```
Log in as any vendor account (see `docs/DEMO-GUIDE.md` for seeded credentials). Open **Overview**. Expected: the "Gross revenue", "Orders" and "Units sold" tiles show non-zero numbers — same numbers as before this change, since `sp_vendor_sales_report` returns exactly what the old inline query returned. If the tiles show blank/zero for a vendor that Task 1 Step 6 showed has revenue, stop — the `{call ...}` wiring is broken, do not proceed to Task 3.

No commit step (not a git repo).

---

### Task 3: Documentation consistency — README, CODE-GUIDE, DEMO-GUIDE

**Files:**
- Modify: `README.md:175-176`
- Modify: `docs/CODE-GUIDE.md:400-404`
- Modify: `docs/DEMO-GUIDE.md:152-155`
- Modify: `docs/DEMO-GUIDE.md:261-264`

**Interfaces:** None — prose only, no code dependency. Can run in parallel with Task 1/2 if using subagent-driven-development, since it touches none of the same files.

- [ ] **Step 1: `README.md`**

Read `README.md:165-180` first to confirm exact current text, then:

```
OLD:
No triggers, stored procedures, window functions or common table expressions are used
anywhere.

NEW:
No window functions or common table expressions are used anywhere. `db/routines.sql`
adds one trigger and one stored procedure (Week 9 material); every other query in the
project stays inside Weeks 1-7.
```

- [ ] **Step 2: `docs/CODE-GUIDE.md`**

Read `docs/CODE-GUIDE.md:395-406` first to confirm exact current text, then:

```
OLD:
**"Did you use anything beyond the syllabus?"**
> No. Weeks 1 to 7 only — DDL, INSERT, SELECT, WHERE, ORDER BY, all the join forms,
> aggregates with GROUP BY and HAVING, subqueries, and views. No triggers, no stored
> procedures, no window functions. Table 6.1 in the report maps every lab week to where
> it appears.

NEW:
**"Did you use anything beyond the syllabus?"**
> Mostly Weeks 1 to 7 — DDL, INSERT, SELECT, WHERE, ORDER BY, all the join forms,
> aggregates with GROUP BY and HAVING, subqueries, and views. We also added one Week 9
> feature deliberately: a trigger that blocks an order line if stock is insufficient,
> plus a stored procedure behind the vendor revenue report. No window functions or
> common table expressions anywhere. Table 6.1 in the report maps every lab week to
> where it appears.
```

- [ ] **Step 3: `docs/DEMO-GUIDE.md`, first occurrence (~line 152)**

Read `docs/DEMO-GUIDE.md:145-156` first to confirm exact current text, then:

```
OLD:
> "Two things we could not do inside the syllabus. Reviews should only be allowed after
> a confirmed purchase — that needs a trigger, so we enforce it in Java instead. And
> MariaDB does not support FULL OUTER JOIN even though it was in the lab, so we used the
> UNION workaround. Both are written up in the Limitations section."

NEW:
> "Two judgment calls worth mentioning. Reviews should only be allowed after a
> confirmed purchase — that still needs checking three other tables at insert time, so
> we kept it in Java rather than a trigger. We did add one trigger, on order_item, to
> block overselling stock — that rule is simple enough to belong in the database. And
> MariaDB does not support FULL OUTER JOIN even though it was in the lab, so we used the
> UNION workaround. All three are written up in the Limitations section."
```

- [ ] **Step 4: `docs/DEMO-GUIDE.md`, second occurrence (~line 261)**

Read `docs/DEMO-GUIDE.md:255-266` first to confirm exact current text, then:

```
OLD:
**"Did you use anything we did not teach?"**
> No. Only weeks 1 to 7 — DDL, INSERT, SELECT, WHERE, ORDER BY, all the join forms,
> aggregate functions with GROUP BY and HAVING, subqueries, and views. No triggers, no
> stored procedures, no window functions. Table 6.1 in the report maps every lab week to
> where it appears.

NEW:
**"Did you use anything we did not teach?"**
> Mostly weeks 1 to 7 — DDL, INSERT, SELECT, WHERE, ORDER BY, all the join forms,
> aggregate functions with GROUP BY and HAVING, subqueries, and views — plus one Week 9
> trigger and one stored procedure, added deliberately and covered in Table 6.1. No
> window functions or common table expressions anywhere.
```

- [ ] **Step 5: Verify no stale claim survives**

```bash
grep -rniE "no triggers|no stored procedures" "README.md" "docs/CODE-GUIDE.md" "docs/DEMO-GUIDE.md" "db/queries.sql"
```
Expected: either no output, or only the intentionally-scoped `db/queries.sql` line from Task 1 Step 3 ("Every query in THIS FILE..."), which is true and should stay. Any other hit means a spot was missed — fix it before moving on.

No commit step (not a git repo).

---

### Task 4: Update the project report (docx, then re-export PDF)

**Files:**
- Modify: `docs/CSE311-Project-Report.docx`
- Regenerate: `docs/CSE311-Project-Report.pdf`

**Interfaces:** None — this task only needs Task 1 to exist (to describe truthfully) and can run any time after Task 1. Requires the `anthropic-skills:docx` skill for the `.docx` edits (Word XML, not plain text — do not attempt raw string replacement on the `.docx` binary).

- [ ] **Step 1: Load the docx skill and open the report**

Invoke the `anthropic-skills:docx` skill on `docs/CSE311-Project-Report.docx`. Locate the five spots below by their identifying text (paragraph text won't have the PDF's mid-sentence line-wrap newlines — match on the phrase, not the exact line breaks).

- [ ] **Step 2: Table 6.1 — add a Week 8/9 row**

Find the table with caption "Table 6.1 Mapping of laboratory topics onto the delivered project" (Section 6.1). It has one row per week/topic group (Weeks 1-2, 3, 4, 5, 6, 7). Append a new row:

| Week | Topic | Where it appears in the project |
|---|---|---|
| 8 and 9 | `CREATE TRIGGER`, `CREATE PROCEDURE` | `db/routines.sql`; the procedure is called from the vendor Overview screen (`VendorFrame.java`) |

- [ ] **Step 3: Section 6.1 opening sentence**

```
OLD (paragraph text, ignore PDF line wraps):
The query set was written to exercise every SQL feature covered in the laboratory sessions,
and deliberately uses nothing beyond them.

NEW:
The query set was written to exercise every SQL feature covered in the laboratory sessions.
One deliberate extension goes beyond Weeks 1-7: a trigger and a stored procedure from Week 9,
added to close a limitation noted in Section 7 and listed as the last row of Table 6.1.
```

- [ ] **Step 4: Table 7.1 — update the stock-oversell row**

Find the table with caption "Table 7.1 Rules enforced outside the schema" (Section 7.1). In the row for "Stock must never be oversold under concurrent checkouts", replace the "Current enforcement" cell:

```
OLD:
A single transaction with a stock check; row level locking would be needed for real
concurrency

NEW:
A single transaction with an application stock check, backed by a BEFORE INSERT trigger
(trg_order_item_stock_guard) that rejects the insert outright if stock is insufficient.
Row level locking (SELECT ... FOR UPDATE) would still be needed for true concurrency
between two simultaneous buyers.
```

- [ ] **Step 5: Section 7.1 intro paragraph**

```
OLD:
Some business rules are beyond what declarative constraints can express, and the course
syllabus does not cover triggers or stored procedures. These rules are enforced in the
Java layer instead, which means they can be bypassed by writing directly to the database
through phpMyAdmin.

NEW:
Some business rules are beyond what declarative constraints can express. The course
syllabus does cover triggers and stored procedures (Week 9); the stock-oversell rule
below now uses one, closing the ordinary-use case, though not the fully concurrent one.
The remaining rules are enforced in the Java layer instead, which means they can be
bypassed by writing directly to the database through phpMyAdmin.
```

- [ ] **Step 6: Conclusion paragraph**

```
OLD:
The limitations recorded in section 7 point to the natural next steps. Triggers would
move the rules currently enforced in Java back into the database where they belong. Row
level locking would make concurrent checkout safe. A returns and refunds subsystem would
complete the order lifecycle. None of these change the core design; each extends it,
which suggests the foundation is sound.

NEW:
The limitations recorded in section 7 point to the natural next steps. One of them has
already been taken: a trigger now backstops the stock-oversell rule directly in the
database. The remaining Java-enforced rules are natural candidates for the same
treatment. Row level locking would make concurrent checkout fully safe. A returns and
refunds subsystem would complete the order lifecycle. None of these change the core
design; each extends it, which suggests the foundation is sound.
```

- [ ] **Step 7: Save the docx and re-export the PDF**

Save `docs/CSE311-Project-Report.docx`. Export/print it to PDF, overwriting `docs/CSE311-Project-Report.pdf` (via the docx skill's export path, or LibreOffice `soffice --headless --convert-to pdf` if available, or hand off to the user to re-export from Word if neither tool is available in this environment).

- [ ] **Step 8: Verify**

```bash
pdftotext -layout "docs/CSE311-Project-Report.pdf" - | grep -n -i "trg_order_item_stock_guard\|Week 9\|8 and 9"
```
Expected: at least 3 hits (Table 6.1 row, Table 7.1 row, Section 7.1/Conclusion mentions). If `pdftotext` shows the old text instead, the PDF was not actually regenerated from the edited docx.

No commit step (not a git repo).

---

## Self-Review Notes

- **Spec coverage:** trigger (Task 1) ✓, procedure (Task 1) ✓, `routines.sql` file + run order (Task 1) ✓, Java wiring (Task 2) ✓, README/CODE-GUIDE/DEMO-GUIDE/queries.sql (Task 3) ✓, report docx five edits + PDF re-export (Task 4) ✓. Out-of-scope items from the spec (other Table 7.1 rules, `CheckoutService.java` changes) have no task — intentional.
- **Placeholder scan:** no TBD/TODO; every step has literal SQL/Java/prose text, not descriptions of text.
- **Type/column consistency:** `sp_vendor_sales_report`'s column order (Task 1 Step 1) matches the `row(c, sql, vendorId, 3)` call in Task 2 Step 2 — both read `gross_revenue, orders_count, units_sold` first, in that order.
