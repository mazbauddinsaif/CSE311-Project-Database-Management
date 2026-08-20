-- =====================================================================
-- CSE 311: Database Management System
-- Project: Multi-Vendor E-Commerce Marketplace
-- File   : queries.sql   (run AFTER schema.sql, seed.sql and views.sql)
--
-- Every query in THIS FILE is restricted to the SQL taught in CSE 311L
-- Weeks 1-7. No triggers, no stored procedures, no window functions and
-- no common table expressions appear here. A trigger and a stored
-- procedure ARE used elsewhere in the project -- see db/routines.sql,
-- which covers the Week 9 material and runs before this file.
--
--   Section A - Week 1-2 : SELECT, aliases, arithmetic, concatenation
--   Section B - Week 3   : WHERE, BETWEEN, IN, LIKE, NULL, ORDER BY
--   Section C - Week 4   : joins (equi, self, three-way, outer)
--   Section D - Week 5   : group functions, GROUP BY, HAVING
--   Section E - Week 6   : subqueries (single-row and multi-row)
--   Section F - Week 7   : views
-- =====================================================================

USE marketplace_db;


-- =====================================================================
-- SECTION A - WEEK 1 AND 2
-- Basic SELECT, column aliases, arithmetic expressions, concatenation,
-- eliminating duplicate rows.
-- =====================================================================

-- A1. Full product variant price list with a column alias.
SELECT  barcode          AS "Barcode",
        description      AS "Variant",
        price            AS "Unit Price"
FROM    product_variant;

-- A2. Arithmetic expression: price including 5% VAT, and the price of a
--     three-piece bundle. Arithmetic happens in the SELECT list.
SELECT  description             AS "Variant",
        price                   AS "Base Price",
        ROUND(price * 1.05, 2)  AS "Price With 5% VAT",
        price * 3               AS "Bundle Of Three"
FROM    product_variant;

-- A3. Concatenation operator building a single display string.
--     MySQL/MariaDB uses CONCAT() where Oracle uses ||.
SELECT  CONCAT(first_name, ' ', last_name, ' <', email, '>') AS "Customer Contact"
FROM    `user`;

-- A4. Eliminating duplicate rows: which brands does the marketplace carry?
SELECT DISTINCT brand AS "Brand"
FROM    product;

-- A5. Payment methods actually used by customers, no duplicates.
SELECT DISTINCT payment_method AS "Payment Method Used"
FROM    payment;

-- A6. Displaying table structure (the lab's DESCRIBE equivalent).
DESCRIBE customer_order;


-- =====================================================================
-- SECTION B - WEEK 3
-- Restricting and sorting data.
-- =====================================================================

-- B1. Comparison condition: every variant priced above 20,000 taka.
SELECT  barcode, description, price
FROM    product_variant
WHERE   price > 20000;

-- B2. BETWEEN: mid-range products, sorted cheapest first.
SELECT  barcode, description, price
FROM    product_variant
WHERE   price BETWEEN 1000 AND 5000
ORDER BY price;

-- B3. IN: orders currently in any pre-delivery state.
SELECT  order_id, store_id, status, total_amount
FROM    customer_order
WHERE   status IN ('PLACED','PACKED','SHIPPED');

-- B4. LIKE with the % wildcard: every product whose title mentions a book.
SELECT  product_id, title, brand
FROM    product
WHERE   title LIKE '%Book%';

-- B5. LIKE with the _ wildcard: brands whose second letter is 'a'.
SELECT DISTINCT brand
FROM    product
WHERE   brand LIKE '_a%';

-- B6. IS NULL: tickets that no support agent has picked up yet.
SELECT  ticket_id, buyer_id, description, status
FROM    ticket
WHERE   support_id IS NULL;

-- B7. IS NOT NULL: orders that had a vendor promotion applied.
SELECT  order_id, store_id, promotion_id, total_amount
FROM    customer_order
WHERE   promotion_id IS NOT NULL;

-- B8. Logical conditions AND / OR.
SELECT  barcode, description, price
FROM    product_variant
WHERE   price >= 20000
  AND   (description LIKE '%256GB%' OR description LIKE '%512GB%');

-- B9. NOT operator: every order that is neither delivered nor cancelled.
SELECT  order_id, status, total_amount
FROM    customer_order
WHERE   status NOT IN ('DELIVERED','CANCELLED');

-- B10. Sorting by multiple columns, one ascending and one descending.
SELECT  store_id, order_id, status, total_amount
FROM    customer_order
ORDER BY store_id ASC, total_amount DESC;

-- B11. Date range restriction: everything bought in the first quarter.
SELECT  checkout_id, buyer_id, checkout_date, total_amount
FROM    checkout
WHERE   checkout_date BETWEEN '2026-01-01' AND '2026-03-31 23:59:59'
ORDER BY checkout_date;


-- =====================================================================
-- SECTION C - WEEK 4
-- Displaying data from multiple tables.
-- =====================================================================

-- C1. Equijoin: which vendor owns which storefront.
SELECT  v.business_name, s.store_id, s.name AS store_name, s.reputation_score
FROM    vendor v, store s
WHERE   v.user_id = s.vendor_id;

-- C2. The same join written with the ON clause (preferred modern form).
SELECT  v.business_name, s.name AS store_name, s.is_active
FROM    vendor v
JOIN    store  s ON s.vendor_id = v.user_id;

-- C3. SELF-JOIN on the recursive category relationship: every
--     sub-category shown beside its parent category.
SELECT  child.category_id   AS "Sub Category ID",
        child.name          AS "Sub Category",
        parent.name         AS "Parent Category"
FROM    category child
JOIN    category parent ON child.parent_category_id = parent.category_id
ORDER BY parent.name, child.name;

-- C4. THREE-WAY JOIN: product, its variant and the category it sits in.
SELECT  p.title, pv.description AS variant, c.name AS category, pv.price
FROM        product         p
JOIN        product_variant pv ON pv.product_id  = p.product_id
JOIN        category        c  ON c.category_id  = p.category_id
ORDER BY c.name, p.title;

-- C5. FIVE-TABLE JOIN: the complete line-item story of every order.
SELECT  o.order_id,
        CONCAT(u.first_name,' ',u.last_name) AS buyer,
        s.name                               AS store,
        p.title                              AS product,
        oi.quantity,
        oi.unit_price,
        oi.quantity * oi.unit_price          AS sub_total
FROM        customer_order  o
JOIN        checkout        ck ON ck.checkout_id = o.checkout_id
JOIN        `user`          u  ON u.user_id      = ck.buyer_id
JOIN        store           s  ON s.store_id     = o.store_id
JOIN        order_item      oi ON oi.order_id    = o.order_id
JOIN        product_variant pv ON pv.variant_id  = oi.variant_id
JOIN        product         p  ON p.product_id   = pv.product_id
ORDER BY o.order_id;

-- C6. LEFT OUTER JOIN: every category, including ones nobody has
--     listed a product under yet.
SELECT  c.name AS category, p.title AS product
FROM        category c
LEFT OUTER JOIN product p ON p.category_id = c.category_id
ORDER BY c.name;

-- C7. LEFT OUTER JOIN finding gaps: checkouts with no payment.
SELECT  ck.checkout_id, ck.buyer_id, ck.checkout_date, ck.total_amount,
        pay.payment_id
FROM        checkout ck
LEFT OUTER JOIN payment pay ON pay.checkout_id = ck.checkout_id
WHERE   pay.payment_id IS NULL;

-- C8. RIGHT OUTER JOIN: every courier company, even those never used.
SELECT  sc.courier_name, sh.shipment_id, sh.status
FROM        shipment sh
RIGHT OUTER JOIN shipment_company sc ON sc.company_id = sh.company_id
ORDER BY sc.courier_name;

-- C9. FULL OUTER JOIN is taught in the lab but is NOT supported by
--     MySQL / MariaDB. The standard workaround is a UNION of a LEFT
--     and a RIGHT outer join. This is discussed in the Limitations
--     section of the report.
SELECT  c.name AS category, p.title AS product
FROM        category c
LEFT OUTER JOIN product p ON p.category_id = c.category_id
UNION
SELECT  c.name AS category, p.title AS product
FROM        category c
RIGHT OUTER JOIN product p ON p.category_id = c.category_id;

-- C10. Join with an additional condition in the ON clause.
SELECT  o.order_id, s.name AS store, o.status, o.total_amount
FROM    customer_order o
JOIN    store          s ON s.store_id = o.store_id
                        AND o.total_amount > 10000;


-- =====================================================================
-- SECTION D - WEEK 5
-- Aggregating data using group functions.
-- =====================================================================

-- D1. AVG, MAX, MIN, SUM and COUNT over the whole catalogue.
SELECT  COUNT(*)            AS "Total Variants",
        ROUND(AVG(price),2) AS "Average Price",
        MAX(price)          AS "Most Expensive",
        MIN(price)          AS "Cheapest",
        SUM(price)          AS "Sum Of All Prices"
FROM    product_variant;

-- D2. COUNT with DISTINCT: how many different categories are in use.
SELECT  COUNT(DISTINCT category_id) AS "Categories In Use"
FROM    product;

-- D3. MIN and MAX on dates: the trading period covered by the data.
SELECT  MIN(checkout_date) AS "First Order",
        MAX(checkout_date) AS "Latest Order"
FROM    checkout;

-- D4. GROUP BY: number of products offered by each store.
SELECT  s.name AS store, COUNT(p.product_id) AS products_listed
FROM    store   s
JOIN    product p ON p.store_id = s.store_id
GROUP BY s.name
ORDER BY products_listed DESC;

-- D5. GROUP BY on multiple columns: units sold per store per status.
SELECT  s.name AS store, o.status, SUM(oi.quantity) AS units
FROM        store          s
JOIN        customer_order o  ON o.store_id  = s.store_id
JOIN        order_item     oi ON oi.order_id = o.order_id
GROUP BY s.name, o.status
ORDER BY s.name, o.status;

-- D6. HAVING: only the stores that have earned more than 20,000 taka.
SELECT  s.name AS store,
        SUM(oi.quantity * oi.unit_price) AS revenue
FROM        store          s
JOIN        customer_order o  ON o.store_id  = s.store_id
JOIN        order_item     oi ON oi.order_id = o.order_id
WHERE   o.status <> 'CANCELLED'
GROUP BY s.name
HAVING  SUM(oi.quantity * oi.unit_price) > 20000
ORDER BY revenue DESC;

-- D7. WHERE and HAVING together. WHERE filters rows before grouping,
--     HAVING filters the groups afterwards.
SELECT  c.name AS category, COUNT(*) AS variant_count,
        ROUND(AVG(pv.price),2) AS avg_price
FROM        category        c
JOIN        product         p  ON p.category_id = c.category_id
JOIN        product_variant pv ON pv.product_id = p.product_id
WHERE   pv.price > 500
GROUP BY c.name
HAVING  COUNT(*) >= 2
ORDER BY avg_price DESC;

-- D8. Average rating per product, worst first, for quality review.
SELECT  p.title, COUNT(r.stars) AS reviews, ROUND(AVG(r.stars),2) AS avg_stars
FROM        product         p
JOIN        product_variant pv ON pv.product_id = p.product_id
JOIN        review          r  ON r.variant_id  = pv.variant_id
GROUP BY p.title
ORDER BY avg_stars ASC;

-- D9. Nesting group functions: the single highest store revenue.
SELECT MAX(store_revenue) AS "Best Store Revenue"
FROM (
    SELECT SUM(oi.quantity * oi.unit_price) AS store_revenue
    FROM        customer_order o
    JOIN        order_item     oi ON oi.order_id = o.order_id
    GROUP BY o.store_id
) AS store_totals;

-- D10. Stock held per warehouse.
SELECT  w.name AS warehouse, COUNT(i.variant_id) AS variant_lines,
        SUM(i.quantity) AS total_units
FROM    warehouse w
JOIN    inventory i ON i.warehouse_id = w.warehouse_id
GROUP BY w.name
ORDER BY total_units DESC;

-- D11. Support workload: tickets handled per agent.
SELECT  CONCAT(u.first_name,' ',u.last_name) AS agent,
        sp.response_time_min,
        COUNT(t.ticket_id) AS tickets_handled
FROM        support sp
JOIN        `user`  u ON u.user_id = sp.user_id
LEFT JOIN   ticket  t ON t.support_id = sp.user_id
GROUP BY u.first_name, u.last_name, sp.response_time_min
ORDER BY tickets_handled DESC;


-- =====================================================================
-- SECTION E - WEEK 6
-- Subqueries.
-- =====================================================================

-- E1. SINGLE-ROW SUBQUERY with a group function: every variant priced
--     above the catalogue average.
SELECT  barcode, description, price
FROM    product_variant
WHERE   price > (SELECT AVG(price) FROM product_variant)
ORDER BY price DESC;

-- E2. SINGLE-ROW SUBQUERY: the most expensive variant in the shop.
SELECT  barcode, description, price
FROM    product_variant
WHERE   price = (SELECT MAX(price) FROM product_variant);

-- E3. MULTI-ROW SUBQUERY with IN: everyone who has ever bought a phone.
SELECT DISTINCT CONCAT(u.first_name,' ',u.last_name) AS buyer
FROM        `user`   u
JOIN        checkout ck ON ck.buyer_id = u.user_id
JOIN        customer_order o ON o.checkout_id = ck.checkout_id
JOIN        order_item oi ON oi.order_id = o.order_id
WHERE   oi.variant_id IN (
            SELECT pv.variant_id
            FROM   product_variant pv
            JOIN   product p ON p.product_id = pv.product_id
            WHERE  p.category_id = (SELECT category_id FROM category
                                    WHERE name = 'Mobile Phones')
        );

-- E4. MULTI-ROW SUBQUERY with NOT IN: variants nobody has ever ordered.
SELECT  pv.variant_id, pv.barcode, pv.description, pv.price
FROM    product_variant pv
WHERE   pv.variant_id NOT IN (SELECT variant_id FROM order_item)
ORDER BY pv.price DESC;

-- E5. MULTI-ROW SUBQUERY with ANY: variants cheaper than at least one
--     laptop variant.
SELECT  barcode, description, price
FROM    product_variant
WHERE   price < ANY (
            SELECT pv.price
            FROM   product_variant pv
            JOIN   product p ON p.product_id = pv.product_id
            WHERE  p.category_id = (SELECT category_id FROM category
                                    WHERE name = 'Laptops')
        )
ORDER BY price DESC;

-- E6. MULTI-ROW SUBQUERY with ALL: variants more expensive than every
--     single book in the shop.
SELECT  barcode, description, price
FROM    product_variant
WHERE   price > ALL (
            SELECT pv.price
            FROM   product_variant pv
            JOIN   product  p ON p.product_id  = pv.product_id
            JOIN   category c ON c.category_id = p.category_id
            WHERE  c.parent_category_id = (SELECT category_id FROM category
                                           WHERE name = 'Books')
        )
ORDER BY price;

-- E7. Group function inside a subquery, compared against a group
--     function outside: stores whose revenue beats the store average.
SELECT  s.name AS store, SUM(oi.quantity * oi.unit_price) AS revenue
FROM        store          s
JOIN        customer_order o  ON o.store_id  = s.store_id
JOIN        order_item     oi ON oi.order_id = o.order_id
GROUP BY s.name
HAVING  SUM(oi.quantity * oi.unit_price) > (
            SELECT AVG(store_total)
            FROM (
                SELECT SUM(oi2.quantity * oi2.unit_price) AS store_total
                FROM   customer_order o2
                JOIN   order_item     oi2 ON oi2.order_id = o2.order_id
                GROUP BY o2.store_id
            ) AS t
        )
ORDER BY revenue DESC;

-- E8. Correlated-style subquery using EXISTS: buyers who have written
--     at least one review.
SELECT  CONCAT(u.first_name,' ',u.last_name) AS buyer
FROM    buyer b
JOIN    `user` u ON u.user_id = b.user_id
WHERE   EXISTS (SELECT 1 FROM review r WHERE r.buyer_id = b.user_id);

-- E9. NOT EXISTS: registered buyers who have never placed an order.
SELECT  CONCAT(u.first_name,' ',u.last_name) AS buyer, u.email
FROM    buyer b
JOIN    `user` u ON u.user_id = b.user_id
WHERE   NOT EXISTS (SELECT 1 FROM checkout ck WHERE ck.buyer_id = b.user_id);

-- E10. Subquery in the FROM clause: the top spending customer.
SELECT  buyer_name, lifetime_spend
FROM (
        SELECT  CONCAT(u.first_name,' ',u.last_name) AS buyer_name,
                SUM(ck.total_amount)                 AS lifetime_spend
        FROM    checkout ck
        JOIN    `user`   u ON u.user_id = ck.buyer_id
        GROUP BY u.first_name, u.last_name
     ) AS spend
WHERE   lifetime_spend = (
            SELECT MAX(total_spend) FROM (
                SELECT SUM(total_amount) AS total_spend
                FROM   checkout GROUP BY buyer_id
            ) AS t
        );

-- E11. Checkouts that fanned out into more than one vendor order,
--      that is, the order-splitting requirement demonstrated.
SELECT  ck.checkout_id,
        CONCAT(u.first_name,' ',u.last_name) AS buyer,
        COUNT(o.order_id)                    AS vendor_orders,
        GROUP_CONCAT(s.name ORDER BY s.name SEPARATOR ' | ') AS stores,
        ck.total_amount
FROM        checkout       ck
JOIN        `user`         u ON u.user_id  = ck.buyer_id
JOIN        customer_order o ON o.checkout_id = ck.checkout_id
JOIN        store          s ON s.store_id = o.store_id
GROUP BY ck.checkout_id, u.first_name, u.last_name, ck.total_amount
HAVING  COUNT(o.order_id) > 1
ORDER BY vendor_orders DESC, ck.checkout_id;


-- =====================================================================
-- SECTION F - WEEK 7
-- Using the views created in views.sql.
-- =====================================================================

-- F1. The shop browse screen, filtered and sorted, from one view.
SELECT  product_title, variant_description, category_name, store_name, price, total_stock
FROM    vw_product_catalog
WHERE   category_name = 'Mobile Phones'
ORDER BY price;

-- F2. Vendor revenue leaderboard straight out of a view.
SELECT  business_name, orders_count, units_sold, gross_revenue, vendor_earning
FROM    vw_vendor_sales
ORDER BY gross_revenue DESC;

-- F3. Low-stock restock alert.
SELECT  store_name, product_title, variant_description, stock_on_hand, low_stock_threshold
FROM    vw_low_stock
ORDER BY stock_on_hand;

-- F4. Best rated products, ignoring anything with fewer than two reviews.
SELECT  product_title, variant_description, review_count, avg_stars
FROM    vw_product_rating
WHERE   review_count >= 2
ORDER BY avg_stars DESC, review_count DESC;

-- F5. A view queried with a subquery on top of it: customers who have
--     spent more than the average customer.
SELECT  buyer_name, age, total_checkouts, lifetime_spend
FROM    vw_buyer_profile
WHERE   lifetime_spend > (SELECT AVG(lifetime_spend) FROM vw_buyer_profile)
ORDER BY lifetime_spend DESC;

-- F6. Orders still awaiting delivery, from the order summary view.
SELECT  order_id, buyer_name, store_name, order_status, shipment_status, net_amount
FROM    vw_order_summary
WHERE   order_status NOT IN ('DELIVERED','CANCELLED')
ORDER BY order_id;


-- =====================================================================
-- END OF QUERIES
-- =====================================================================
