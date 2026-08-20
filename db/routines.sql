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
-- gross_revenue, orders_count, units_sold first.
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
