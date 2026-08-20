# Multi-Vendor E-Commerce Marketplace Database

CSE 311 — Database Management System project. North South University, Department of
Electrical and Computer Engineering.

A marketplace database in the style of Daraz: many independent vendors sell through one
storefront, the customer pays once, and the system splits that single basket into a
separate order for each vendor to fulfil.

Built on **MariaDB / XAMPP**, with a **Java Swing + JDBC** desktop application on top.

The data set is modelled on the Bangladeshi retail market throughout: local names and
districts, local brands such as Aarong, Walton, Kiam and Pran, local couriers such as
Pathao and RedX, and bKash, Nagad, Rocket and Upay as payment methods.

---

## What is in here

| Path | Contents |
|---|---|
| `db/schema.sql` | Data definition: 27 tables, 41 foreign keys, 17 check constraints |
| `db/seed.sql` | Sample data, 23,704 rows of Bangladeshi retail data |
| `db/views.sql` | Six views used by the application and the reports |
| `db/queries.sql` | Query set organised by lab week |
| `app/src/marketplace/` | Java source for the desktop application |
| `app/lib/` | JDBC driver and the FlatLaf look and feel |
| `docs/er-diagram.png` | Entity relationship diagram with cardinality |
| `docs/schema-diagram.png` | Relational schema diagram |
| `docs/CSE311-Project-Report.docx` | Project report |
| `docs/DEMO-GUIDE.md` | Setup, demonstration walkthrough and expected questions |
| `docs/CODE-GUIDE.md` | Tech stack, file-by-file structure, and the order to read the code in |
| `docs/screenshots/` | Screens of the running application |

---

## The design problem

A customer's basket can hold items from several vendors at once. The customer sees one
purchase and pays once. Each vendor sees only its own share, which it prices, packs and
ships independently.

Modelling a purchase as a single order makes the vendor's view expensive, because there
is nowhere to hang a fulfilment status or a shipment for one vendor's portion. Modelling
it as several independent orders loses the fact that the customer paid once.

The schema resolves this with a `checkout` entity sitting above `customer_order`:

```
checkout ──1:N──> customer_order ──1:N──> order_item
   │                     │
 payment              shipment
```

- `checkout` — the act of paying. One row, one buyer, one address, one payment.
- `customer_order` — one vendor's share. Carries `store_id`, its own status and subtotal.
- `UNIQUE (checkout_id, store_id)` — guarantees exactly one order per vendor per basket.

A basket spanning three vendors produces one `checkout` row, three `customer_order` rows
and one `payment` row.

---

## Setup

**Requirements:** XAMPP (MariaDB 10.4+), and a JRE 8+ to run the app. Building from
source additionally needs a JDK.

### 1. Start the database

Open the XAMPP control panel and start **MySQL**.

### 2. Import the SQL scripts, in this order

Through phpMyAdmin (`http://localhost/phpmyadmin` → Import), or on the command line:

```bash
cd db
mysql -u root < schema.sql
mysql -u root < seed.sql
mysql -u root < views.sql
```

`schema.sql` drops and recreates the `marketplace_db` database, so re-running it resets
everything to a clean state.

### 3. Run the application

```bash
cd app
run.bat
```

To rebuild from source first:

```bash
cd app
build.bat
```

If the database uses a password for `root`, set it in
`app/src/marketplace/Db.java` and rebuild.

### Sample accounts

All accounts use the password `pass123`.

| Role | Email |
|---|---|
| Buyer | `nusrat.jahan@mail.com` |
| Vendor | `karim.hossain@shop.bd` |
| Support | `zareef.mirza@help.bd` |

The login screen lists these and fills them in when clicked.

It also opens a **Reporting console**, which runs the showcase queries and
displays both the SQL and its result. No login is required for it.

---

## Trying the vendor split

1. Log in as the buyer.
2. On **Browse**, add a product from one storefront to the cart.
3. Add a second product from a *different* storefront.
4. Open **My Cart** and place the order.
5. **My Orders** now shows one row per vendor, all sharing a single checkout reference,
   and the panel on the right draws the split: one payment fanning out into the
   separate orders each seller has to fulfil.

Or check it in SQL:

```sql
SELECT ck.checkout_id, COUNT(o.order_id) AS vendor_orders, ck.total_amount
FROM   checkout       ck
JOIN   customer_order o ON o.checkout_id = ck.checkout_id
GROUP BY ck.checkout_id, ck.total_amount
HAVING COUNT(o.order_id) > 1;
```

---

## Schema notes

**ISA hierarchy.** `user` is specialised into `vendor`, `buyer` and `support` using the
shared primary key strategy: each subclass takes `user_id` as both its own primary key
and a foreign key to `user`.

**Derived attributes are not stored.** A buyer's age is computed from `dob`; an order
line's sub total is computed as `quantity * unit_price`.

**Multivalued attributes became tables.** Phone numbers and variant images live in
`user_phone` and `variant_image` rather than as repeating columns, which would break 1NF.

**`order_item.unit_price` is duplicated on purpose.** It snapshots the variant price at
the moment of purchase so that a later price change does not rewrite historical invoices.

**`customer_order`, not `order`.** `ORDER` is a reserved word in SQL.

---

## SQL scope

The project uses only what the CSE 311 lab covered in weeks 1–7:

| Week | Topics | Where |
|---|---|---|
| 1–2 | `CREATE TABLE`, `INSERT`, `SELECT`, aliases, arithmetic, `CONCAT`, `DISTINCT` | `schema.sql`, `seed.sql`, queries A1–A6 |
| 3 | `WHERE`, `BETWEEN`, `IN`, `LIKE`, `IS NULL`, `AND`/`OR`/`NOT`, `ORDER BY` | B1–B11 |
| 4 | equijoin, `ON`, self join, three-way join, `LEFT`/`RIGHT`/`FULL OUTER JOIN` | C1–C10 |
| 5 | `COUNT`, `SUM`, `AVG`, `MIN`, `MAX`, `GROUP BY`, `HAVING` | D1–D11 |
| 6 | single-row and multi-row subqueries, `IN`, `ANY`, `ALL`, `EXISTS` | E1–E11 |
| 7 | `CREATE VIEW` | `views.sql`, F1–F6 |

No window functions or common table expressions are used anywhere. `db/routines.sql`
adds one trigger and one stored procedure (Week 9 material); every other query in the
project stays inside Weeks 1-7.

Note that MariaDB does not support `FULL OUTER JOIN`. Query C9 shows the standard
workaround, a `UNION` of a left and a right outer join.

---

## Known limitations

- Reviews can only be restricted to confirmed purchases in the application layer, since
  enforcing it in the database would need a trigger.
- Passwords are stored in plain text so the sample accounts work during the demo.
- Payment is recorded but not processed; there is no gateway integration and no refunds.
- Cancelling an order changes its status but does not restore stock.
- Search is a `LIKE` match on title and brand, with no ranking.

Section 7 of the report covers these in full.
