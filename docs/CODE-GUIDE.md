# Code Guide

How the project is built, what every file does, and the order to read them in so it
actually makes sense. Written for the team first, and for explaining the work to the
course instructor second.

---

## Part 1 — The technology stack

### The four layers

```
   YOU click a button
        │
   ┌────▼──────────────────────────────────────┐
   │  Java Swing            the windows,        │   what the user sees
   │                        tables, buttons     │
   ├───────────────────────────────────────────┤
   │  JDBC                  carries SQL down,   │   the bridge
   │  (Connector/J)         rows back up        │
   ├───────────────────────────────────────────┤
   │  MariaDB               runs the SQL,       │   the database
   │  (inside XAMPP)        enforces the rules  │
   ├───────────────────────────────────────────┤
   │  InnoDB                stores the rows,    │   the storage engine
   │                        enforces keys       │
   └───────────────────────────────────────────┘
```

### What each piece is and why it was chosen

| Layer | Technology | Why this one |
|---|---|---|
| Database server | **MariaDB 10.4** | Ships inside XAMPP, so it runs on any lab machine with no separate install. Its SQL is nearly identical to the Oracle syntax taught in the lab. |
| Storage engine | **InnoDB** | The only common MySQL engine that actually enforces foreign keys and supports transactions. MyISAM would accept our `FOREIGN KEY` lines and then silently ignore them. |
| Admin tool | **phpMyAdmin** | Already used in the lab sessions. Good for importing the scripts and browsing tables during the demonstration. |
| Character set | **utf8mb4** | Stores Bangla text and any symbol correctly. The older `utf8` in MySQL is only three bytes and breaks on some characters. |
| Application language | **Java SE 8, Swing** | Reuses the object-oriented Java from earlier courses. Swing is part of the JDK, so the application needs no web server, no browser and no framework. |
| Database connectivity | **JDBC, MySQL Connector/J 8.0.33** | The standard Java database API. One jar, works against MariaDB. |
| Look and feel | **FlatLaf 3.4.1** | Swing's built-in appearance is dated. FlatLaf replaces it with flat modern controls. It is presentation only and touches no logic. |

### How a single click travels through the stack

Take "Add to cart". This is the shortest complete path and worth tracing once.

```
1. User selects a row and clicks the button
      BuyerFrame.addToCart()
2. Read the selected variant id out of the table
      TableUtil.intAt(catalogTable, row, 0)
3. Open a connection
      Db.open()               ->  DriverManager  ->  Connector/J
4. Send parameterised SQL
      INSERT INTO cart_item (buyer_id, variant_id, quantity) VALUES (?,?,?)
      ON DUPLICATE KEY UPDATE quantity = quantity + ?
5. MariaDB checks the constraints
      FK buyer_id  -> buyer.user_id      exists?
      FK variant_id-> product_variant    exists?
      CHECK quantity > 0                 satisfied?
      PK (buyer_id, variant_id)          duplicate -> run the UPDATE branch
6. Row written, connection closed
7. Screen refreshed
      Ui.fill(cartTable, ...)  ->  new SELECT  ->  rows  ->  JTable
```

The important idea: **the database enforces correctness, the Java only asks**. If the
Java layer were replaced tomorrow by a website, every rule would still hold, because the
rules live in `schema.sql`, not in the application.

### Two decisions worth being able to defend

**Prepared statements, never string concatenation.** Every query uses `?` placeholders
and `ps.setInt(...)`. Besides blocking SQL injection, it keeps date and decimal
formatting out of our hands.

```java
// what we do
ps = c.prepareStatement("SELECT ... WHERE buyer_id = ?");
ps.setInt(1, buyerId);

// what we never do
"SELECT ... WHERE buyer_id = " + buyerId
```

**One transaction for checkout.** Checkout writes to five tables. Either all of it
happens or none of it does. That is `setAutoCommit(false)` … `commit()` … `rollback()`
in `CheckoutService`.

---

## Part 2 — Project structure

```
project/
│
├── db/                     THE DATABASE
│   ├── schema.sql              27 tables + every constraint      (499 lines)
│   ├── seed.sql                sample data, 23,704 rows
│   ├── views.sql               6 views                           (186 lines)
│   └── queries.sql             query set by lab week             (475 lines)
│
├── app/                    THE APPLICATION
│   ├── src/marketplace/        13 Java classes                 (3,665 lines)
│   ├── lib/                    JDBC driver, FlatLaf
│   ├── manifest.txt            names the main class
│   ├── build.bat               compile + package
│   └── run.bat                 launch
│
├── docs/                   THE PAPERWORK
│   ├── CSE311-Project-Report.docx
│   ├── er-diagram.png
│   ├── schema-diagram.png
│   ├── DEMO-GUIDE.md           how to present it
│   ├── CODE-GUIDE.md           this file
│   └── screenshots/
│
├── README.md
└── .gitignore
```

### The 13 Java classes, grouped by job

Do not think of these as thirteen separate things. They are **four groups**.

**Group 1 — Plumbing (2 classes).** Talking to the database.

| File | Lines | Job |
|---|---|---|
| `Db.java` | 34 | Opens a connection. The only place the URL, username and password appear. |
| `TableUtil.java` | 91 | Turns a `ResultSet` into something a `JTable` can display. Error and message popups. |

**Group 2 — Business logic (1 class).** The actual thinking.

| File | Lines | Job |
|---|---|---|
| `CheckoutService.java` | 324 | **The vendor split.** Reads the cart, groups it by store, writes one checkout and one order per store, takes stock, records payment, empties the cart. One transaction. |

**Group 3 — Appearance (4 classes).** No logic, only how things look.

| File | Lines | Job |
|---|---|---|
| `Theme.java` | 176 | Colours, fonts, money formatting, status colours. Installs FlatLaf. |
| `Ui.java` | 547 | Factory for styled widgets: cards, stat tiles, buttons, tables, cell renderers. |
| `Glyph.java` | 160 | Draws the icons as vector paths, so there are no image files to ship. |
| `Shell.java` | 323 | The window frame every dashboard shares: navigation rail, title bar, page stack. |

**Group 4 — Screens (6 classes).** One per thing the user does.

| File | Lines | Job |
|---|---|---|
| `App.java` | 365 | Login. Resolves the ISA hierarchy and opens the right dashboard. Has `main()`. |
| `BuyerFrame.java` | 575 | Browse, cart, orders, wishlist, profile. |
| `VendorFrame.java` | 287 | Sales overview, orders to fulfil, catalogue, low stock, promotions. |
| `SupportFrame.java` | 246 | Ticket queue and team workload. |
| `ReportsFrame.java` | 285 | Runs the showcase queries and shows the SQL next to the result. |
| `SplitRibbon.java` | 252 | Draws one checkout fanning out into its vendor orders. |

> If someone asks "which file is the project really about", the answer is
> **`CheckoutService.java`**. Everything else is catalogue, screens and decoration.

---

## Part 3 — The order to read the code in

Do not open the files alphabetically. Follow this path. Each step assumes the one before.

### Step 1 — `db/schema.sql` (start here, always)

Read the whole thing before touching any Java. It is the vocabulary for everything else.
It is written in 12 sections with comments explaining each decision.

Read for these five things:

1. **The ISA hierarchy** (section 1). `user` holds what everyone shares. `vendor`,
   `buyer` and `support` each take `user_id` as *both* their primary key *and* a foreign
   key back to `user`. That is how SQL fakes inheritance.
2. **The multivalued attributes** (`user_phone`, `variant_image`). A person can have two
   phone numbers. You cannot put two values in one column without breaking 1NF, so they
   become their own tables.
3. **The recursive relationship** (`category.parent_category_id`). A foreign key pointing
   at its own table. This is what makes the self-join possible.
4. **The split** (section 7). `checkout` → `customer_order` → `order_item`. Read the
   comment block above it; it explains the whole project in ten lines.
5. **What is deliberately missing.** There is no `age` column and no `sub_total` column.
   Both are derived and are computed when queried.

**Checkpoint:** you should be able to answer "why is there a `checkout` table separate
from `customer_order`" without looking.

### Step 2 — `db/views.sql`

Six views. Each is one big join hidden behind a short name. Read `vw_order_summary`
carefully — it is the one the application leans on hardest, and it is where `store_id`
and `vendor_id` are exposed so a vendor can be shown only their own orders.

**Checkpoint:** you should be able to say what a view is — a stored SELECT, holding no
data of its own.

### Step 3 — `db/queries.sql`

Organised A through F by lab week. Do not read it top to bottom; skim the section
headers, then read section E (subqueries) and query **E11**, which expresses the entire
order-splitting requirement in one statement.

### Step 4 — `Db.java` (34 lines, five minutes)

The smallest file. Read it whole. It tells you where the connection string lives, which
is the first thing to change if the database has a password.

### Step 5 — `TableUtil.java` (91 lines)

Read `toModel()`. It walks a `ResultSet` and copies it into a `DefaultTableModel`. Once
you understand this one method, every table in the application is demystified — they all
go through it.

### Step 6 — `CheckoutService.java` (the important one)

Read `placeOrder()` from top to bottom. It is written in the order it executes:

```
open connection, turn off auto-commit
  readCart()          cart lines joined to the store that owns each
  verifyStock()       every line checked against total stock
  group by store      LinkedHashMap<storeId, lines>
  insertCheckout()    one row: the act of paying
  for each store:
      insertOrder()   one row per vendor
      insertItems()   the lines beneath it, unit_price copied as a snapshot
  updateCheckoutTotal()
  insertPayment()
  deductStock()
  clearCart()
commit
```

The `catch` block calls `rollback()`. That is what makes an oversell leave nothing
behind.

**Checkpoint:** you should be able to explain why the grouping happens in Java rather
than in SQL. (Because we are *writing* rows, not reading them. SQL groups rows you have;
here we decide how many rows to create.)

### Step 7 — `App.java`, method `attemptLogin()`

Skip the layout code at the top; go to `attemptLogin()`. Two steps: match the email and
password against `user`, then probe `vendor`, `buyer`, `support` in turn to find out
which kind of account it is. That is the ISA hierarchy being resolved at runtime.

### Step 8 — `Shell.java`

Read `page()` and `showPage()`. Every dashboard registers its pages with `page(...)` and
the rail switches between them with a `CardLayout`. Once you see this, `BuyerFrame`,
`VendorFrame` and `SupportFrame` are all the same shape.

### Step 9 — `BuyerFrame.java`

Now the biggest file is easy, because it is repetitive by design. Each page is a pair:

```java
private JPanel  xxxPage()      // builds the widgets, once
private void    refreshXxx()   // runs the SQL, fills the table
```

Read `browsePage()` + `refreshCatalog()`, then skip to `placeOrder()`. The rest is the
same pattern with different SQL.

### Step 10 — everything else, only if you need it

`Theme`, `Ui`, `Glyph`, `SplitRibbon`, `VendorFrame`, `SupportFrame`, `ReportsFrame`.
None of them contain logic you have not already seen. `SplitRibbon` is worth a look if
you like graphics: it is one `paintComponent()` method drawing boxes and curves.

---

## Part 4 — Three flows traced through the real code

Being able to walk one of these end to end is worth more than memorising the file list.

### Flow A — Logging in

```
App.main()
  └─ Theme.install()                    FlatLaf + fonts
  └─ new App()                          the split login screen
        user clicks Sign in
  └─ attemptLogin()
        SELECT user_id, first_name, last_name
        FROM `user` WHERE email = ? AND password = ?
        │
        ├─ existsIn(c, "vendor",  id) ?  ->  new VendorFrame(...)
        ├─ existsIn(c, "buyer",   id) ?  ->  new BuyerFrame(...)
        └─ existsIn(c, "support", id) ?  ->  new SupportFrame(...)
```

### Flow B — Browsing the catalogue

```
BuyerFrame.refreshCatalog()
  └─ Ui.fill(catalogTable, "SELECT ... FROM vw_product_catalog WHERE ...")
        └─ Db.open()
        └─ PreparedStatement, bound parameters
        └─ TableUtil.toModel(rs)          ResultSet  ->  table model
        └─ Ui.style(table)                money columns, status pills
        └─ Ui.sizeColumns(table)          widths to fit the card
```

Note that the screen queries a **view**, not five tables. That is the point of views.

### Flow C — Checkout, the one that matters

```
BuyerFrame.placeOrder()
  └─ confirm dialog
  └─ CheckoutService.placeOrder(buyerId, addressId, method)
        │
        │   BEGIN
        ├─ SELECT cart joined to product and store
        ├─ stock check on every line          ── fails ──> ROLLBACK, nothing written
        ├─ group lines by store_id
        ├─ INSERT INTO checkout                one row
        ├─ for each store:
        │      INSERT INTO customer_order      one row per vendor
        │      INSERT INTO order_item          the lines
        ├─ UPDATE checkout SET total_amount
        ├─ INSERT INTO payment
        ├─ UPDATE inventory                    take the stock
        ├─ DELETE FROM cart_item
        │   COMMIT
        │
  └─ back in BuyerFrame: jump to My Orders, load SplitRibbon
```

---

## Part 5 — Explaining it to the instructor

### The 60-second version

> "There are three parts. `db` holds the database — one file creates the tables, one
> loads the data, one creates the views, one holds our queries. `app` is a Java Swing
> program that talks to that database through JDBC. `docs` has the report and the
> diagrams.
>
> The design problem is that one customer basket can hold products from several sellers.
> The customer pays once, but each seller ships separately. We solved it with a
> `checkout` table above `customer_order`: checkout is the act of paying, and each
> customer_order is one seller's share of it. The splitting itself happens in
> `CheckoutService.java`, inside a single transaction."

### If she asks to see code, show these three, in this order

**1. `schema.sql`, the checkout section.** The design decision, in the schema.

```sql
CREATE TABLE customer_order (
    order_id     INT NOT NULL AUTO_INCREMENT,
    checkout_id  INT NOT NULL,
    store_id     INT NOT NULL,   -- <-- the vendor split
    ...
    CONSTRAINT uq_order_per_store UNIQUE (checkout_id, store_id)
);
```

Say: *"the unique constraint guarantees exactly one order per seller per basket. The
database will not let us get it wrong."*

**2. `CheckoutService.placeOrder()`, the grouping loop.** The decision becoming code.

Say: *"we read the cart with the store joined on, group by store in Java, then write one
order row per group."*

**3. Query E11 in `queries.sql`.** The decision proved in one statement.

Say: *"any basket that appears here was paid for once but fulfilled by more than one
vendor. 355 of them in our data."*

### Questions about the stack, with answers

**"Why Swing and not a website?"**
> Swing is part of the JDK, so the application is one jar and needs no web server. The
> course is about the database, and a desktop client keeps the JDBC layer visible instead
> of hiding it behind a framework.

**"Why MariaDB when the lab used Oracle?"**
> XAMPP ships MariaDB, so it runs on any machine without a separate install. The SQL is
> nearly identical. Two differences we hit: MariaDB uses `CONCAT()` where Oracle uses
> `||`, and MariaDB has no `FULL OUTER JOIN`, so query C9 uses the standard `UNION`
> workaround.

**"What is JDBC?"**
> The standard Java API for databases. Our code writes plain JDBC calls; the
> Connector/J jar translates them into the MySQL wire protocol. Swapping to a different
> database would mean swapping the driver, not rewriting the queries.

**"Where is the SQL in your Java?"**
> In the screen classes, as prepared statements with `?` placeholders. Nothing is built
> by concatenating strings, so the values are always sent as parameters.

**"Did you use anything beyond the syllabus?"**
> Mostly Weeks 1 to 7 — DDL, INSERT, SELECT, WHERE, ORDER BY, all the join forms,
> aggregates with GROUP BY and HAVING, subqueries, and views. We also added one Week 9
> feature deliberately: a trigger that blocks an order line if stock is insufficient,
> plus a stored procedure behind the vendor revenue report. No window functions or
> common table expressions anywhere. Table 6.1 in the report maps every lab week to
> where it appears.

**"How do you stop a vendor seeing another vendor's orders?"**
> Every vendor query is filtered by `vendor_id`, and the status update joins through
> `store` so the `WHERE` clause carries `s.vendor_id = ?`. If the order is not theirs the
> update affects zero rows and the application says so.

### Dividing the explanation between three people

| Member | Explains | Files to know cold |
|---|---|---|
| Designer | The problem, the ER model, cardinality, why Checkout exists | `er-diagram.png`, report sections 1, 3, 4 |
| DBA | The mapping to tables, constraints, normalisation, the queries | `schema.sql`, `views.sql`, `queries.sql` |
| Developer | How the app reaches the database, the transaction, the split | `Db.java`, `CheckoutService.java`, `BuyerFrame.java` |

Each person should still be able to give the 60-second version above, in case the
question lands on the wrong one.

---

## Part 6 — Practical notes

**Where to change things**

| To change | Edit |
|---|---|
| Database password | `app/src/marketplace/Db.java`, the `PASSWORD` field, then rebuild |
| Colours or fonts | `app/src/marketplace/Theme.java` |
| A report in the console | The `REPORTS` array at the top of `ReportsFrame.java` |
| Sample data | `db/seed.sql`, then re-import all three scripts |

**Rebuilding after any Java change**

```bash
cd app
build.bat
run.bat
```

`build.bat` needs a JDK. `run.bat` only needs a JRE. The classes are compiled for
Java 8 so the jar runs on almost any machine.

**Resetting the database**

```bash
cd db
mysql -u root < schema.sql
mysql -u root < seed.sql
mysql -u root < views.sql
```

`schema.sql` drops and recreates everything, so this always returns a clean state.

**If the application will not start**

| Symptom | Cause |
|---|---|
| Window never appears | MySQL not running in XAMPP |
| "Cannot reach the database" on login | Same, or the password in `Db.java` is wrong |
| `UnsupportedClassVersionError` | Jar was built for a newer Java than the JRE running it. Rebuild with `build.bat`. |
| Tables empty but the app opens | `seed.sql` was not imported, or was imported before `schema.sql` |
