

USE marketplace_db;

DROP VIEW IF EXISTS vw_product_catalog;
DROP VIEW IF EXISTS vw_vendor_sales;
DROP VIEW IF EXISTS vw_order_summary;
DROP VIEW IF EXISTS vw_low_stock;
DROP VIEW IF EXISTS vw_product_rating;
DROP VIEW IF EXISTS vw_buyer_profile;


CREATE VIEW vw_product_catalog AS
SELECT  pv.variant_id,
        p.product_id,
        p.title                              AS product_title,
        pv.description                       AS variant_description,
        p.brand,
        c.name                               AS category_name,
        s.store_id,
        s.name                               AS store_name,
        v.business_name                      AS vendor_name,
        pv.price,
        IFNULL(SUM(i.quantity), 0)           AS total_stock
FROM        product_variant pv
JOIN        product   p ON p.product_id  = pv.product_id
JOIN        store     s ON s.store_id    = p.store_id
JOIN        vendor    v ON v.user_id     = s.vendor_id
LEFT JOIN   category  c ON c.category_id = p.category_id
LEFT JOIN   inventory i ON i.variant_id  = pv.variant_id
WHERE   s.is_active = 1
GROUP BY pv.variant_id, p.product_id, p.title, pv.description, p.brand,
         c.name, s.store_id, s.name, v.business_name, pv.price;


CREATE VIEW vw_vendor_sales AS
SELECT  v.user_id                                       AS vendor_id,
        v.business_name,
        v.commission_rate,
        COUNT(DISTINCT o.order_id)                      AS orders_count,
        SUM(oi.quantity)                                AS units_sold,
        SUM(oi.quantity * oi.unit_price)                AS gross_revenue,
        ROUND(SUM(oi.quantity * oi.unit_price)
              * v.commission_rate / 100, 2)             AS platform_commission,
        ROUND(SUM(oi.quantity * oi.unit_price)
              * (100 - v.commission_rate) / 100, 2)     AS vendor_earning
FROM        vendor         v
JOIN        store          s  ON s.vendor_id = v.user_id
JOIN        customer_order o  ON o.store_id  = s.store_id
JOIN        order_item     oi ON oi.order_id = o.order_id
WHERE   o.status <> 'CANCELLED'
GROUP BY v.user_id, v.business_name, v.commission_rate;

CREATE VIEW vw_order_summary AS
SELECT  o.order_id,
        o.checkout_id,
        c.checkout_date,
        o.buyer_display_id                             AS buyer_id,
        o.buyer_name,
        s.store_id,
        s.vendor_id,
        s.name                                         AS store_name,
        o.status                                       AS order_status,
        o.item_lines,
        o.units,
        o.gross_amount,
        o.total_amount                                 AS net_amount,
        o.shipment_status
FROM (
        SELECT  o.order_id,
                o.checkout_id,
                o.store_id,
                o.status,
                o.total_amount,
                ck.buyer_id                              AS buyer_display_id,
                CONCAT(u.first_name,' ',u.last_name)     AS buyer_name,
                COUNT(oi.order_item_id)                  AS item_lines,
                SUM(oi.quantity)                         AS units,
                SUM(oi.quantity * oi.unit_price)         AS gross_amount,
                MAX(sh.status)                           AS shipment_status
        FROM        customer_order o
        JOIN        checkout    ck ON ck.checkout_id = o.checkout_id
        JOIN        `user`      u  ON u.user_id      = ck.buyer_id
        JOIN        order_item  oi ON oi.order_id    = o.order_id
        LEFT JOIN   shipment    sh ON sh.order_id    = o.order_id
        GROUP BY o.order_id, o.checkout_id, o.store_id, o.status,
                 o.total_amount, ck.buyer_id, u.first_name, u.last_name
     ) o
JOIN checkout c ON c.checkout_id = o.checkout_id
JOIN store    s ON s.store_id    = o.store_id;


CREATE VIEW vw_low_stock AS
SELECT  pv.variant_id,
        p.title                     AS product_title,
        pv.description              AS variant_description,
        pv.barcode,
        s.store_id,
        v.user_id                   AS vendor_id,
        s.name                      AS store_name,
        v.business_name             AS vendor_name,
        pv.low_stock_threshold,
        IFNULL(SUM(i.quantity), 0)  AS stock_on_hand
FROM        product_variant pv
JOIN        product   p ON p.product_id = pv.product_id
JOIN        store     s ON s.store_id   = p.store_id
JOIN        vendor    v ON v.user_id    = s.vendor_id
LEFT JOIN   inventory i ON i.variant_id = pv.variant_id
GROUP BY pv.variant_id, p.title, pv.description, pv.barcode, s.store_id,
         v.user_id, s.name, v.business_name, pv.low_stock_threshold
HAVING IFNULL(SUM(i.quantity), 0) <= pv.low_stock_threshold;


CREATE VIEW vw_product_rating AS
SELECT  pv.variant_id,
        p.title                          AS product_title,
        pv.description                   AS variant_description,
        COUNT(r.stars)                   AS review_count,
        ROUND(AVG(r.stars), 2)           AS avg_stars,
        MIN(r.stars)                     AS lowest_star,
        MAX(r.stars)                     AS highest_star
FROM        product_variant pv
JOIN        product p ON p.product_id = pv.product_id
LEFT JOIN   review  r ON r.variant_id = pv.variant_id
GROUP BY pv.variant_id, p.title, pv.description;


CREATE VIEW vw_buyer_profile AS
SELECT  b.user_id                                        AS buyer_id,
        CONCAT(u.first_name,' ',u.last_name)             AS buyer_name,
        u.email,
        b.dob,
        TIMESTAMPDIFF(YEAR, b.dob, CURDATE())            AS age,
        b.loyalty_points,
        COUNT(DISTINCT ck.checkout_id)                   AS total_checkouts,
        IFNULL(SUM(ck.total_amount), 0)                  AS lifetime_spend
FROM        buyer    b
JOIN        `user`   u  ON u.user_id  = b.user_id
LEFT JOIN   checkout ck ON ck.buyer_id = b.user_id
GROUP BY b.user_id, u.first_name, u.last_name, u.email, b.dob, b.loyalty_points;


