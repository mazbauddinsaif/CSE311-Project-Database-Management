

DROP DATABASE IF EXISTS marketplace_db;
CREATE DATABASE marketplace_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;
USE marketplace_db;

SET FOREIGN_KEY_CHECKS = 1;


CREATE TABLE `user` (
    user_id       INT             NOT NULL AUTO_INCREMENT,
    email         VARCHAR(100)    NOT NULL,
    password      VARCHAR(255)    NOT NULL,
    first_name    VARCHAR(50)     NOT NULL,
    last_name     VARCHAR(50)     NOT NULL,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_user        PRIMARY KEY (user_id),
    CONSTRAINT uq_user_email  UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE user_phone (
    user_id       INT             NOT NULL,
    phone         VARCHAR(20)     NOT NULL,
    CONSTRAINT pk_user_phone PRIMARY KEY (user_id, phone),
    CONSTRAINT fk_phone_user FOREIGN KEY (user_id)
        REFERENCES `user` (user_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE vendor (
    user_id         INT           NOT NULL,
    business_name   VARCHAR(100)  NOT NULL,
    tax_id          VARCHAR(30)   NOT NULL,
    commission_rate DECIMAL(5,2)  NOT NULL DEFAULT 5.00,
    CONSTRAINT pk_vendor         PRIMARY KEY (user_id),
    CONSTRAINT fk_vendor_user    FOREIGN KEY (user_id)
        REFERENCES `user` (user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT uq_vendor_tax     UNIQUE (tax_id),
    CONSTRAINT ck_vendor_comm    CHECK (commission_rate >= 0 AND commission_rate <= 100)
) ENGINE=InnoDB;

CREATE TABLE buyer (
    user_id        INT            NOT NULL,
    dob            DATE           NULL,
    loyalty_points INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_buyer       PRIMARY KEY (user_id),
    CONSTRAINT fk_buyer_user  FOREIGN KEY (user_id)
        REFERENCES `user` (user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_buyer_pts   CHECK (loyalty_points >= 0)
) ENGINE=InnoDB;

CREATE TABLE support (
    user_id           INT         NOT NULL,
    response_time_min INT         NOT NULL DEFAULT 60,   -- average response, minutes
    CONSTRAINT pk_support      PRIMARY KEY (user_id),
    CONSTRAINT fk_support_user FOREIGN KEY (user_id)
        REFERENCES `user` (user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_support_rt   CHECK (response_time_min > 0)
) ENGINE=InnoDB;



CREATE TABLE address (
    address_id    INT           NOT NULL AUTO_INCREMENT,
    owner_user_id INT           NULL,
    house         VARCHAR(50)   NULL,
    block         VARCHAR(50)   NULL,
    street        VARCHAR(100)  NOT NULL,
    city          VARCHAR(50)   NOT NULL,
    postal_code   VARCHAR(15)   NULL,
    country       VARCHAR(50)   NOT NULL DEFAULT 'Bangladesh',
    CONSTRAINT pk_address       PRIMARY KEY (address_id),
    CONSTRAINT fk_address_owner FOREIGN KEY (owner_user_id)
        REFERENCES `user` (user_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;


-- =====================================================================
-- SECTION 3: STORE AND PRODUCT CATALOG
-- =====================================================================

-- Vendor 1 : N Store  (a vendor may operate more than one storefront)
CREATE TABLE store (
    store_id         INT           NOT NULL AUTO_INCREMENT,
    vendor_id        INT           NOT NULL,
    name             VARCHAR(100)  NOT NULL,
    reputation_score DECIMAL(3,2)  NOT NULL DEFAULT 0.00,
    is_active        TINYINT(1)    NOT NULL DEFAULT 1,
    CONSTRAINT pk_store        PRIMARY KEY (store_id),
    CONSTRAINT fk_store_vendor FOREIGN KEY (vendor_id)
        REFERENCES vendor (user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_store_rep    CHECK (reputation_score >= 0 AND reputation_score <= 5)
) ENGINE=InnoDB;

-- Recursive relationship: parent_category_id is a foreign key onto
-- this same table, giving a category tree.
CREATE TABLE category (
    category_id        INT          NOT NULL AUTO_INCREMENT,
    name               VARCHAR(60)  NOT NULL,
    parent_category_id INT          NULL,
    CONSTRAINT pk_category        PRIMARY KEY (category_id),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_category_id)
        REFERENCES category (category_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE product (
    product_id  INT           NOT NULL AUTO_INCREMENT,
    store_id    INT           NOT NULL,
    category_id INT           NULL,
    title       VARCHAR(150)  NOT NULL,
    description TEXT          NULL,
    brand       VARCHAR(60)   NULL,
    CONSTRAINT pk_product          PRIMARY KEY (product_id),
    CONSTRAINT fk_product_store    FOREIGN KEY (store_id)
        REFERENCES store (store_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id)
        REFERENCES category (category_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

-- One product has many variants (size / colour / material).
-- Price sits on the variant, not the product, since variants of the
-- same product differ in price.
CREATE TABLE product_variant (
    variant_id          INT           NOT NULL AUTO_INCREMENT,
    product_id          INT           NOT NULL,
    barcode             VARCHAR(40)   NOT NULL,
    description         VARCHAR(150)  NULL,
    price               DECIMAL(10,2) NOT NULL,
    low_stock_threshold INT           NOT NULL DEFAULT 10,
    CONSTRAINT pk_variant         PRIMARY KEY (variant_id),
    CONSTRAINT fk_variant_product FOREIGN KEY (product_id)
        REFERENCES product (product_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT uq_variant_barcode UNIQUE (barcode),
    CONSTRAINT ck_variant_price   CHECK (price > 0)
) ENGINE=InnoDB;

-- Not present in the original ER. Holds the option values that
-- distinguish variants, e.g. ('Size','L'), ('Color','Red').
CREATE TABLE variant_option (
    variant_id   INT          NOT NULL,
    option_name  VARCHAR(30)  NOT NULL,
    option_value VARCHAR(50)  NOT NULL,
    CONSTRAINT pk_variant_option PRIMARY KEY (variant_id, option_name),
    CONSTRAINT fk_option_variant FOREIGN KEY (variant_id)
        REFERENCES product_variant (variant_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- `Image` is a MULTIVALUED attribute of Product Variant (double oval).
CREATE TABLE variant_image (
    image_id   INT           NOT NULL AUTO_INCREMENT,
    variant_id INT           NOT NULL,
    image_url  VARCHAR(255)  NOT NULL,
    CONSTRAINT pk_variant_image  PRIMARY KEY (image_id),
    CONSTRAINT fk_image_variant  FOREIGN KEY (variant_id)
        REFERENCES product_variant (variant_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;


-- =====================================================================
-- SECTION 4: INVENTORY
--
-- "stored in" is M:N and carries a descriptive attribute (quantity),
-- so it becomes its own table with a composite primary key.
-- =====================================================================

CREATE TABLE warehouse (
    warehouse_id INT          NOT NULL AUTO_INCREMENT,
    name         VARCHAR(80)  NOT NULL,
    address_id   INT          NULL,
    CONSTRAINT pk_warehouse      PRIMARY KEY (warehouse_id),
    CONSTRAINT fk_warehouse_addr FOREIGN KEY (address_id)
        REFERENCES address (address_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE inventory (
    variant_id   INT NOT NULL,
    warehouse_id INT NOT NULL,
    quantity     INT NOT NULL DEFAULT 0,
    CONSTRAINT pk_inventory      PRIMARY KEY (variant_id, warehouse_id),
    CONSTRAINT fk_inv_variant    FOREIGN KEY (variant_id)
        REFERENCES product_variant (variant_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_inv_warehouse  FOREIGN KEY (warehouse_id)
        REFERENCES warehouse (warehouse_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_inv_qty        CHECK (quantity >= 0)
) ENGINE=InnoDB;


-- =====================================================================
-- SECTION 5: PROMOTIONS
--
-- Vendor 1:N Promotion, Promotion M:N Product Variant.
-- discount_type covers both percentage and fixed amount offers.
-- =====================================================================

CREATE TABLE promotion (
    promotion_id     INT            NOT NULL AUTO_INCREMENT,
    vendor_id        INT            NOT NULL,
    code             VARCHAR(25)    NOT NULL,
    reason           VARCHAR(120)   NULL,
    discount_type    ENUM('PERCENT','FIXED') NOT NULL DEFAULT 'PERCENT',
    amount           DECIMAL(10,2)  NOT NULL,
    min_order_amount DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    start_date       DATE           NOT NULL,
    end_date         DATE           NOT NULL,
    CONSTRAINT pk_promotion     PRIMARY KEY (promotion_id),
    CONSTRAINT fk_promo_vendor  FOREIGN KEY (vendor_id)
        REFERENCES vendor (user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT uq_promo_code    UNIQUE (code),
    CONSTRAINT ck_promo_amount  CHECK (amount > 0),
    CONSTRAINT ck_promo_dates   CHECK (end_date >= start_date)
) ENGINE=InnoDB;

CREATE TABLE promotion_variant (
    promotion_id INT NOT NULL,
    variant_id   INT NOT NULL,
    CONSTRAINT pk_promo_variant  PRIMARY KEY (promotion_id, variant_id),
    CONSTRAINT fk_pv_promotion   FOREIGN KEY (promotion_id)
        REFERENCES promotion (promotion_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_pv_variant     FOREIGN KEY (variant_id)
        REFERENCES product_variant (variant_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;


-- =====================================================================
-- SECTION 6: CART AND WISHLIST
--
-- Both are M:N relationships between Buyer and Product Variant.
-- Cart carries Quantity; wishlist does not need one.
-- =====================================================================

CREATE TABLE cart_item (
    buyer_id   INT      NOT NULL,
    variant_id INT      NOT NULL,
    quantity   INT      NOT NULL DEFAULT 1,
    added_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_cart_item   PRIMARY KEY (buyer_id, variant_id),
    CONSTRAINT fk_cart_buyer  FOREIGN KEY (buyer_id)
        REFERENCES buyer (user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_cart_variant FOREIGN KEY (variant_id)
        REFERENCES product_variant (variant_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_cart_qty    CHECK (quantity > 0)
) ENGINE=InnoDB;

CREATE TABLE wishlist_item (
    buyer_id   INT      NOT NULL,
    variant_id INT      NOT NULL,
    added_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wishlist_item   PRIMARY KEY (buyer_id, variant_id),
    CONSTRAINT fk_wish_buyer      FOREIGN KEY (buyer_id)
        REFERENCES buyer (user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_wish_variant    FOREIGN KEY (variant_id)
        REFERENCES product_variant (variant_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;


-- =====================================================================
-- SECTION 7: CHECKOUT -> ORDER SPLITTING -> ORDER ITEMS
--
-- Requirement: "the system must split a single customer cart into
-- separate vendor orders for individual fulfillment."
--
-- One CHECKOUT (the single act of paying) fans out into one
-- CUSTOMER_ORDER per store, so each vendor fulfills only its own rows:
--
--        checkout  1 ----- N  customer_order  1 ----- N  order_item
--           |                       |
--        payment                 shipment
--
-- `order` is a reserved word in SQL, so the table is `customer_order`.
-- =====================================================================

CREATE TABLE checkout (
    checkout_id         INT           NOT NULL AUTO_INCREMENT,
    buyer_id            INT           NOT NULL,
    shipping_address_id INT           NOT NULL,
    checkout_date       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount        DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT pk_checkout        PRIMARY KEY (checkout_id),
    CONSTRAINT fk_checkout_buyer  FOREIGN KEY (buyer_id)
        REFERENCES buyer (user_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_checkout_addr   FOREIGN KEY (shipping_address_id)
        REFERENCES address (address_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_checkout_total  CHECK (total_amount >= 0)
) ENGINE=InnoDB;

CREATE TABLE customer_order (
    order_id     INT           NOT NULL AUTO_INCREMENT,
    checkout_id  INT           NOT NULL,
    store_id     INT           NOT NULL,   -- <-- the vendor split
    promotion_id INT           NULL,
    order_date   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status       ENUM('PLACED','PACKED','SHIPPED','DELIVERED','CANCELLED')
                 NOT NULL DEFAULT 'PLACED',
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT pk_order            PRIMARY KEY (order_id),
    CONSTRAINT fk_order_checkout   FOREIGN KEY (checkout_id)
        REFERENCES checkout (checkout_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_order_store      FOREIGN KEY (store_id)
        REFERENCES store (store_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_order_promotion  FOREIGN KEY (promotion_id)
        REFERENCES promotion (promotion_id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT uq_order_per_store  UNIQUE (checkout_id, store_id),
    CONSTRAINT ck_order_total      CHECK (total_amount >= 0)
) ENGINE=InnoDB;

-- unit_price is a deliberate snapshot of product_variant.price at the
-- time of purchase, so a later price change does not rewrite past
-- invoices. Sub total is derived (quantity * unit_price), not stored.
CREATE TABLE order_item (
    order_item_id INT           NOT NULL AUTO_INCREMENT,
    order_id      INT           NOT NULL,
    variant_id    INT           NOT NULL,
    quantity      INT           NOT NULL,
    unit_price    DECIMAL(10,2) NOT NULL,
    CONSTRAINT pk_order_item      PRIMARY KEY (order_item_id),
    CONSTRAINT fk_oi_order        FOREIGN KEY (order_id)
        REFERENCES customer_order (order_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_oi_variant      FOREIGN KEY (variant_id)
        REFERENCES product_variant (variant_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT uq_oi_order_variant UNIQUE (order_id, variant_id),
    CONSTRAINT ck_oi_qty          CHECK (quantity > 0),
    CONSTRAINT ck_oi_price        CHECK (unit_price >= 0)
) ENGINE=InnoDB;


-- =====================================================================
-- SECTION 8: PAYMENT
--
-- Payment attaches to the checkout, not to each split order, since the
-- buyer pays once for the whole cart. paid_by_user_id implements the
-- "*not by buyer" note: another user may settle a checkout.
-- =====================================================================

CREATE TABLE payment (
    payment_id     INT           NOT NULL AUTO_INCREMENT,
    checkout_id    INT           NOT NULL,
    paid_by_user_id INT          NOT NULL,
    payment_method ENUM('BKASH','NAGAD','ROCKET','UPAY','CARD','BANK','COD') NOT NULL,
    paid_amount    DECIMAL(12,2) NOT NULL,
    paid_date      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reference      VARCHAR(60)   NOT NULL,
    CONSTRAINT pk_payment        PRIMARY KEY (payment_id),
    CONSTRAINT fk_pay_checkout   FOREIGN KEY (checkout_id)
        REFERENCES checkout (checkout_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_pay_payer      FOREIGN KEY (paid_by_user_id)
        REFERENCES `user` (user_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT uq_pay_checkout   UNIQUE (checkout_id),
    CONSTRAINT uq_pay_reference  UNIQUE (reference),
    CONSTRAINT ck_pay_amount     CHECK (paid_amount >= 0)
) ENGINE=InnoDB;


-- =====================================================================
-- SECTION 9: SHIPMENT
--
-- A shipment belongs to one split order, therefore to one vendor.
-- shipment_item references order_item rather than product_variant so
-- partial shipments are representable and each shipped row traces back
-- to its order. Tracking resolves through
-- shipment -> customer_order -> checkout -> buyer.
-- =====================================================================

CREATE TABLE shipment_company (
    company_id   INT          NOT NULL AUTO_INCREMENT,
    courier_name VARCHAR(80)  NOT NULL,
    phone_number VARCHAR(20)  NULL,
    is_active    TINYINT(1)   NOT NULL DEFAULT 1,
    CONSTRAINT pk_ship_company PRIMARY KEY (company_id),
    CONSTRAINT uq_courier_name UNIQUE (courier_name)
) ENGINE=InnoDB;

CREATE TABLE shipment (
    shipment_id            INT       NOT NULL AUTO_INCREMENT,
    order_id               INT       NOT NULL,
    company_id             INT       NOT NULL,
    address_id             INT       NOT NULL,
    dispatch_date          DATE      NULL,
    estimated_delivery_date DATE     NULL,
    status                 ENUM('PENDING','IN_TRANSIT','DELIVERED','RETURNED')
                           NOT NULL DEFAULT 'PENDING',
    CONSTRAINT pk_shipment       PRIMARY KEY (shipment_id),
    CONSTRAINT fk_ship_order     FOREIGN KEY (order_id)
        REFERENCES customer_order (order_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ship_company   FOREIGN KEY (company_id)
        REFERENCES shipment_company (company_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_ship_address   FOREIGN KEY (address_id)
        REFERENCES address (address_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_ship_dates     CHECK (estimated_delivery_date IS NULL
                                     OR dispatch_date IS NULL
                                     OR estimated_delivery_date >= dispatch_date)
) ENGINE=InnoDB;

CREATE TABLE shipment_item (
    shipment_id   INT NOT NULL,
    order_item_id INT NOT NULL,
    quantity      INT NOT NULL,
    CONSTRAINT pk_shipment_item PRIMARY KEY (shipment_id, order_item_id),
    CONSTRAINT fk_si_shipment   FOREIGN KEY (shipment_id)
        REFERENCES shipment (shipment_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_si_order_item FOREIGN KEY (order_item_id)
        REFERENCES order_item (order_item_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_si_qty        CHECK (quantity > 0)
) ENGINE=InnoDB;


-- =====================================================================
-- SECTION 10: REVIEW  (weak entity)
--
-- Drawn as a double rectangle in the ER. A review has no identifier of
-- its own; it is identified by its buyer plus the variant reviewed.
-- The composite key also limits a buyer to one review per variant.
-- =====================================================================

CREATE TABLE review (
    buyer_id    INT          NOT NULL,
    variant_id  INT          NOT NULL,
    stars       TINYINT      NOT NULL,
    review_text TEXT         NULL,
    review_date DATE         NOT NULL DEFAULT (CURRENT_DATE),
    CONSTRAINT pk_review        PRIMARY KEY (buyer_id, variant_id),
    CONSTRAINT fk_review_buyer  FOREIGN KEY (buyer_id)
        REFERENCES buyer (user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_review_variant FOREIGN KEY (variant_id)
        REFERENCES product_variant (variant_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_review_stars  CHECK (stars BETWEEN 1 AND 5)
) ENGINE=InnoDB;


-- =====================================================================
-- SECTION 11: SUPPORT TICKET
--
-- Buyer submits a ticket, a support agent manages it. order_id is
-- nullable because a complaint need not concern a specific order.
-- =====================================================================

CREATE TABLE ticket (
    ticket_id       INT          NOT NULL AUTO_INCREMENT,
    buyer_id        INT          NOT NULL,
    support_id      INT          NULL,      -- unassigned until picked up
    order_id        INT          NULL,      -- "concerning" an order
    description     TEXT         NOT NULL,
    status          ENUM('OPEN','IN_PROGRESS','RESOLVED','CLOSED')
                    NOT NULL DEFAULT 'OPEN',
    resolution_text TEXT         NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_ticket         PRIMARY KEY (ticket_id),
    CONSTRAINT fk_ticket_buyer   FOREIGN KEY (buyer_id)
        REFERENCES buyer (user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ticket_support FOREIGN KEY (support_id)
        REFERENCES support (user_id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_ticket_order   FOREIGN KEY (order_id)
        REFERENCES customer_order (order_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;


-- =====================================================================
-- SECTION 12: SECONDARY INDEXES
-- Foreign keys are indexed automatically by InnoDB. These extra indexes
-- support the search and reporting queries in queries.sql.
-- =====================================================================

CREATE INDEX idx_product_title    ON product (title);
CREATE INDEX idx_variant_price    ON product_variant (price);
CREATE INDEX idx_order_date       ON customer_order (order_date);
CREATE INDEX idx_checkout_date    ON checkout (checkout_date);
CREATE INDEX idx_shipment_status  ON shipment (status);

-- =====================================================================
-- END OF SCHEMA  -- 27 tables
-- =====================================================================
