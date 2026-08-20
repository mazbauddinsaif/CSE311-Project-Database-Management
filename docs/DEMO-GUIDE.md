# Demonstration Guide

Everything needed to set up, run and present the project. Follow it top to bottom.

---

## Part 1 — Setup on the demonstration machine

Do this **before** the session starts, not in front of the examiner.

### 1. Start MySQL
Open the XAMPP control panel, click **Start** next to MySQL. Wait for the green
highlight.

### 2. Import the database

Open `http://localhost/phpmyadmin`, then import these three files **in this order**:

| Order | File | What it does |
|---|---|---|
| 1 | `db/schema.sql` | Creates the database and all 27 tables |
| 2 | `db/seed.sql` | Loads the sample data |
| 3 | `db/views.sql` | Creates the six views |

In phpMyAdmin: **Import** tab → **Choose File** → select the file → **Go**. Repeat for
each. There is no need to create the database first; `schema.sql` creates it.

Command line alternative:

```bash
cd db
mysql -u root < schema.sql
mysql -u root < seed.sql
mysql -u root < views.sql
```

### 3. Confirm it worked

In phpMyAdmin, click `marketplace_db` in the left panel. You should see 27 tables and 6
views. Click `customer_order` → **Browse** → about 1,190 rows.

### 4. Start the application

```bash
cd app
run.bat
```

The login window should appear. If it does not, MySQL is not running.

### 5. Have these open and ready

- phpMyAdmin on `marketplace_db`
- The application login window
- `docs/er-diagram.png` and `docs/schema-diagram.png`
- The report, on the Table of Contents page

> **If something breaks:** `schema.sql` drops and recreates everything, so re-importing
> the three files in order returns you to a known good state in about ten seconds.

---

## Part 2 — The demonstration, in order

Roughly ten minutes. The order builds an argument: here is the problem, here is the
design that solves it, here is proof it works.

### Step 1 — State the problem first (30 seconds)

Before opening anything, say what the project is about:

> "This is a multi-vendor marketplace, like Daraz. The hard part is that one customer
> basket can contain products from several different sellers. The customer pays once,
> but each seller has to pack and ship their own part separately, and must not see the
> other sellers' orders. Our whole design is built around solving that."

This one sentence tells the examiner you understood the problem rather than just
building tables.

### Step 2 — Show the design that solves it (1 minute)

Open `docs/er-diagram.png`. Point at **Checkout** and **Order**:

> "Checkout is the act of paying — one row, one buyer, one address, one payment.
> Order is one seller's share of that basket, and it carries the store, its own status
> and its own subtotal. One checkout splits into many orders. That is the relationship
> that makes the whole thing work."

Then point at the **ISA triangle** under User:

> "Vendors, buyers and support staff all log in and share email, password and name, but
> each has its own extra fields. Instead of one table full of nulls, we used a
> generalisation hierarchy."

### Step 3 — Show the schema (1 minute)

Open `docs/schema-diagram.png`, then switch to phpMyAdmin so she sees it is real.

> "27 tables, 41 foreign keys, 17 check constraints. Colours group them: users,
> catalogue, inventory, orders, fulfilment."

Click one table in phpMyAdmin — `order_item` is a good choice — and show the **Structure**
tab so the columns and keys are visible in the live database.

### Step 4 — Run the split, live (3 minutes) — **this is the important part**

1. Log in as the buyer: `nusrat.jahan@mail.com` / `pass123` — click the row on the login
   screen and it fills itself in.
2. **Browse**. Pick any row, note its **Store**, click **Add to cart**.
3. Scroll or change the category filter, then pick a row from a **different store** and
   add that too.
4. **My Cart**. Point at the Store column and the line under the table — it names how many
   sellers are in the basket.
5. Click **Place order**, confirm.
6. The app jumps straight to **My Orders**. Point at the panel on the right:

> "One checkout on the left, and the lines fan out into one order per seller. They share a
> single checkout reference and a single payment, but each has its own status, so each
> vendor packs and ships independently. That is the requirement met."

7. Switch to phpMyAdmin, browse `customer_order`, sort by `checkout_id` descending. The
   new rows share a `checkout_id` but have different `store_id`.

Then sign out from the rail at the bottom left and log in as the vendor:
`karim.hossain@shop.bd` / `pass123`

> "The vendor sees only their own storefronts. The other seller's half of that same basket
> is invisible here."

**Overview** shows revenue, orders, units and restock count as live figures.
**Low Stock** is worth showing too — stock came down when the order was placed.

### Step 5 — Show the SQL (3 minutes)

Close the dashboard, go back to the login window, click **Reporting console**. It shows
the SQL and the result together. Walk through four:

| Report | What it proves |
|---|---|
| Checkouts split across several vendors | `GROUP BY` + `HAVING`, and the core requirement in one query |
| Category tree (self-join) | Self join on the recursive relationship |
| Variants nobody has ever ordered | Multi-row subquery with `NOT IN` |
| Vendor revenue leaderboard | A view, aggregation, commission computed not stored |

If she asks for something specific, `db/queries.sql` has the full set labelled by lab
week, so you can open it and run any query in phpMyAdmin.

### Step 6 — Be honest about limits (30 seconds)

Volunteering a limitation before being asked reads as confidence, not weakness.

> "Two judgment calls worth mentioning. Reviews should only be allowed after a
> confirmed purchase — that still needs checking three other tables at insert time, so
> we kept it in Java rather than a trigger. We did add one trigger, on order_item, to
> block overselling stock — that rule is simple enough to belong in the database. And
> MariaDB does not support FULL OUTER JOIN even though it was in the lab, so we used the
> UNION workaround. All three are written up in the Limitations section."

---

## Part 3 — Explaining the folder structure

If asked "how is your project organised", use this. Four folders, one purpose each.

```
project/
│
├── db/          THE DATABASE — run these in order
│   ├── schema.sql     CREATE TABLE for all 27 tables, with every constraint
│   ├── seed.sql       Sample data, 23,704 rows
│   ├── views.sql      The 6 views
│   └── queries.sql    Query set, organised by lab week
│
├── app/         THE APPLICATION — Java Swing + JDBC
│   ├── src/marketplace/   12 Java classes
│   ├── lib/               JDBC driver, FlatLaf look and feel
│   ├── build.bat          Compile
│   └── run.bat            Launch
│
├── docs/        THE DOCUMENTS
│   ├── CSE311-Project-Report.docx
│   ├── er-diagram.png
│   └── schema-diagram.png
│
└── README.md    Setup instructions
```

The one-line version:

> "`db` is the database itself, `app` is the program that uses it, `docs` is the report
> and the diagrams. The SQL files run in the order schema, seed, views, queries."

### The Java classes, if she asks

| Class | Responsibility |
|---|---|
| `Db` | Opens the JDBC connection. One place to change the password |
| `App` | Login screen; decides which dashboard to open |
| `BuyerFrame` | Browse, cart, checkout, my orders, profile |
| `VendorFrame` | My products, orders to fulfil, low stock, sales |
| `SupportFrame` | Ticket queue and team workload |
| `ReportsFrame` | Runs the showcase queries |
| `CheckoutService` | **The vendor splitting logic.** One transaction |
| `SplitRibbon` | Draws one checkout fanning out into its vendor orders |
| `Shell` | Window frame: navigation rail, title bar, page stack |
| `Theme` / `Ui` / `Glyph` | Colours, fonts, styled widgets, drawn icons |
| `TableUtil` | Puts a ResultSet into a Swing table |

If she wants to see one file, show `CheckoutService.java`. It is where the design
decision actually becomes code.

---

## Part 4 — Questions to expect

**"Why did you add a Checkout table? Why not just one order table?"**
> Because a basket can hold items from several sellers. If it were one order, there would
> be nowhere to put a per-seller status or a per-seller shipment, and vendor revenue
> would need filtering on every query. Checkout holds what happens once — paying. Order
> holds what happens per seller — fulfilment.

**"Is this in third normal form?"**
> Yes. No repeating groups, so 1NF. In `order_item` the key is order plus variant and
> both quantity and unit price depend on the whole key, so 2NF. We removed a city column
> from store because city depended on address, not on store — that transitive dependency
> would have broken 3NF. Section 2.5 of the report works through it.

**"`order_item.unit_price` duplicates `product_variant.price`. Isn't that redundancy?"**
> It looks like it, but they answer different questions. `product_variant.price` is what
> the shop is asking today. `order_item.unit_price` is what the customer actually agreed
> to pay on the day. If a seller raises the price next month, old invoices must not
> change. It is a deliberate snapshot of a time-dependent fact.

**"Where is the derived attribute?"**
> Two. A buyer's age is not stored — it is computed from date of birth with
> `TIMESTAMPDIFF`. And an order line's sub total is not stored — it is
> `quantity * unit_price`. Both were dashed ovals in the ER diagram.

**"How did you handle the multivalued attribute?"**
> A user can have several phone numbers. Putting them in one column as a comma-separated
> list would break first normal form, so `user_phone` is its own table with the composite
> key (user_id, phone). Same for variant images.

**"Show me a self join."**
> Category has a recursive relationship — every category can name a parent. Open the
> category tree report in the console; it opens `category` twice under two aliases,
> `child` and `parent`, and matches one against the other.

**"What if two customers buy the last item at the same time?"**
> Our checkout runs as a single transaction: it checks stock, writes the checkout, the
> orders, the lines and the payment, then deducts stock, and commits. If any line fails
> the stock check the whole thing rolls back and nothing is written. Under true
> simultaneous access we would need row-level locking; that is stated in the Limitations
> section.

**"Why MariaDB and not Oracle, which we used in the lab?"**
> XAMPP ships MariaDB, so it runs on any machine without a separate install. The SQL is
> nearly identical. Two differences we hit: MariaDB uses `CONCAT()` where Oracle uses the
> `||` operator, and MariaDB has no `FULL OUTER JOIN`, so we used a UNION of a left and
> a right outer join.

**"Did you use anything we did not teach?"**
> Mostly weeks 1 to 7 — DDL, INSERT, SELECT, WHERE, ORDER BY, all the join forms,
> aggregate functions with GROUP BY and HAVING, subqueries, and views — plus one Week 9
> trigger and one stored procedure, added deliberately and covered in Table 6.1. No
> window functions or common table expressions anywhere.

**"Who did what?"**
> Appendix B of the report. Designer did requirements and the ER model, DBA did the
> mapping, DDL, data and queries, developer did the Java application and testing.

---

## Part 5 — Quick reference card

**Accounts** — password `pass123` for all

| Role | Email |
|---|---|
| Buyer | `nusrat.jahan@mail.com` |
| Vendor | `karim.hossain@shop.bd` |
| Support | `zareef.mirza@help.bd` |

**Numbers worth remembering**

| | |
|---|---|
| Tables | 27 |
| Foreign keys | 41 |
| Check constraints | 17 |
| Views | 6 |
| Sample rows | 23,704 |
| Baskets that split across vendors | 355 |
| Entities in the ER | 23 |
| Relationships | 32 |

**Reset the database at any time**

```bash
cd db
mysql -u root < schema.sql
mysql -u root < seed.sql
mysql -u root < views.sql
```
