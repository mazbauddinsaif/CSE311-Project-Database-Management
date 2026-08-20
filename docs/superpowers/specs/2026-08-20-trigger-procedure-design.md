# Add a trigger and a stored procedure (Week 8/9 extension)

## Context

The project currently stops deliberately at Week 7 material (views). This is
recorded in four places as a rehearsed defense position, not just a code
comment:

- `README.md` ("No triggers, stored procedures... used anywhere")
- `docs/CODE-GUIDE.md` — scripted viva Q&A citing "Table 6.1"
- `docs/DEMO-GUIDE.md` — the same scripted Q&A, twice
- `docs/CSE311-Project-Report.docx`/`.pdf` — Table 6.1 (syllabus coverage
  map), Table 7.1 (rules enforced only in Java because "the course syllabus
  does not cover triggers or stored procedures"), and the Conclusion, which
  names triggers as a stated "next step."

The user wants a real trigger and procedure added, wired into the running
Java app, matching the exact syntax style taught in `Week 8, 9.pdf`
(`DELIMITER $$ ... END$$`, `FOR EACH ROW`, `NEW.col`/`OLD.col`,
`CREATE PROCEDURE name(IN p TYPE) BEGIN SELECT ... END$$`, `CALL`). Chosen
scope: **full consistency** — code, README, CODE-GUIDE, DEMO-GUIDE,
queries.sql header, and the report doc all updated together so nothing
contradicts the demo.

## What gets built

### 1. Trigger — `trg_order_item_stock_guard`

```sql
DELIMITER $$
CREATE TRIGGER trg_order_item_stock_guard
BEFORE INSERT ON order_item
FOR EACH ROW
BEGIN
    DECLARE available INT;
    SELECT IFNULL(SUM(quantity), 0) INTO available
    FROM inventory WHERE variant_id = NEW.variant_id;
    IF available < NEW.quantity THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Insufficient stock for this variant';
    END IF;
END$$
DELIMITER ;
```

Rejected earlier design: a trigger that *deducts* inventory on insert. That
would double-deduct — `CheckoutService.deductStock()` already deducts stock,
with multi-warehouse spillover, inside the same transaction. The guard
version only validates, never mutates, so it cannot conflict with existing
Java logic. It directly answers Table 7.1's "Stock must never be oversold"
row, which already names a trigger as the fix.

Not a full concurrency fix (still needs `SELECT ... FOR UPDATE` for two
simultaneous buyers) — the report's Table 7.1 keeps a trimmed note saying so.

### 2. Procedure — `sp_vendor_sales_report`

```sql
DELIMITER $$
CREATE PROCEDURE sp_vendor_sales_report(IN p_vendor_id INT)
BEGIN
    SELECT gross_revenue, orders_count, units_sold,
           platform_commission, vendor_earning, business_name, commission_rate
    FROM   vw_vendor_sales
    WHERE  vendor_id = p_vendor_id;
END$$
DELIMITER ;
```

Column order matches what `VendorFrame.row()` already expects (revenue,
orders, units first) so the Java-side change is a one-line SQL swap, not a
rewrite. Wraps the existing `vw_vendor_sales` view rather than re-joining —
consistent with the project's own "views hide joins" convention.

### 3. New file: `db/routines.sql`

Runs after `views.sql`. Contains both objects above plus a short demo block
in the course's own style (insert/call, then `SELECT` to prove it fired) —
mirrors the `Bank_Account`/`Account_Audit` and `show_dept` examples from
`Week 8, 9.pdf`. Run order becomes:
`schema.sql -> seed.sql -> views.sql -> routines.sql -> queries.sql`.

### 4. Java wiring

`VendorFrame.refreshOverview()` — swap the `row(c, "SELECT ... FROM
vw_vendor_sales WHERE vendor_id = ?", vendorId, 3)` call for
`row(c, "{call sp_vendor_sales_report(?)}", vendorId, 3)`. No other Java
file changes; `row()`'s existing plumbing (`PreparedStatement` +
positional `ResultSet` read) already handles a `CALL` that returns one
result set.

### 5. Documentation updates (keep everything consistent)

- `db/schema.sql` header run-order comment: add `routines.sql`.
- `db/queries.sql` header: replace the blanket "no triggers, no stored
  procedures... anywhere" with a note that this *file* stays Week 1-7 only,
  and the two Week 9 objects live separately in `routines.sql`.
- `README.md:175-176`: same correction.
- `docs/CODE-GUIDE.md` Q&A (~line 400): update the rehearsed answer to
  mention the Week 9 extension and point at the updated Table 6.1/7.1.
- `docs/DEMO-GUIDE.md` Q&A (both copies, ~151 and ~260): same correction.
- `docs/CSE311-Project-Report.docx` (source of truth; re-export the PDF
  after editing):
  - Table 6.1: add a Week 8/9 row pointing at `routines.sql` and the
    `VendorFrame` wiring.
  - Section 6.1 opening sentence: drop "deliberately uses nothing beyond
    them," replace with a one-line note about the deliberate Week 9
    extension.
  - Table 7.1, "Stock must never be oversold" row: update "Current
    enforcement" to mention `trg_order_item_stock_guard`, keep the
    remaining concurrency caveat.
  - Section 7.1 intro sentence: drop "the course syllabus does not cover
    triggers or stored procedures" (it does — Week 9 — the project chose
    not to use it, which is a different claim).
  - Conclusion: soften "Triggers would move the rules... back into the
    database" since one now has been.

## Out of scope

- The other three Table 7.1 rules (review-after-purchase, order-total
  consistency, promotion-vendor match) stay Java-side. Not part of this
  request.
- No change to `CheckoutService.java`'s own stock check — it stays as the
  primary check; the trigger is a backstop, not a replacement.
- Lab1,2 / Lab3 / Lab4,5 / Week6 PDFs confirmed irrelevant (basic
  SELECT/joins/GROUP BY topics, no trigger/procedure content).
