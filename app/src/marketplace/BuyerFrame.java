package marketplace;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Buyer dashboard: browse, cart, checkout, orders and profile. */
public class BuyerFrame extends Shell {

    private final int    buyerId;
    private final String buyerName;

    private final JTable catalogTable = Ui.table();
    private final JTable cartTable    = Ui.table();
    private final JTable ordersTable  = Ui.table();
    private final JTable wishTable    = Ui.table();
    private final JTable profileTable = Ui.table();

    private final JTextField        searchField = Ui.search("Search products or brands", 22);
    private final JComboBox<String> categoryBox = new JComboBox<String>();
    private final JSpinner          qty = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));

    private final List<Integer>     addressIds = new ArrayList<Integer>();
    private final JComboBox<String> addressBox = new JComboBox<String>();
    private final JComboBox<String> methodBox  = new JComboBox<String>(
        new String[] { "BKASH", "NAGAD", "ROCKET", "UPAY", "CARD", "BANK", "COD" });

    private final JLabel cartTotal = new JLabel(" ");
    private final SplitRibbon ribbon = new SplitRibbon();

    private Ui.Card statOrders, statSpend, statCart, statPoints;

    public BuyerFrame(int buyerId, String buyerName) {
        super("Marketplace", buyerName, "Buyer");
        this.buyerId   = buyerId;
        this.buyerName = buyerName;

        page("browse", "Browse", "Browse the catalogue",
             "Products from every active storefront on the platform.", "browse", browsePage());
        page("cart", "My Cart", "My cart",
             "Items from different sellers will be split at checkout.", "cart", cartPage());
        page("orders", "My Orders", "My orders",
             "One row per vendor order. Select one to see how its basket was split.",
             "split", ordersPage());
        page("wishlist", "Wishlist", "Wishlist",
             "Items saved for later.", "heart", wishPage());
        page("profile", "Profile", "My account",
             "Summary of your activity on the marketplace.", "user", profilePage());

        loadCategories();
        loadAddresses();
        showPage("browse");
    }

    @Override
    protected void onPageShown(String key) {
        if ("browse".equals(key))        refreshCatalog();
        else if ("cart".equals(key))     refreshCart();
        else if ("orders".equals(key))   refreshOrders();
        else if ("wishlist".equals(key)) refreshWishlist();
        else if ("profile".equals(key))  refreshProfile();
    }

    // ------------------------------------------------------------- browse

    private JPanel browsePage() {
        JButton addCart = Ui.primary("Add to cart");
        JButton addWish = Ui.ghost("Save to wishlist");
        JButton search  = Ui.ghost("Search");

        search.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { refreshCatalog(); }
        });
        searchField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { refreshCatalog(); }
        });
        categoryBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { refreshCatalog(); }
        });
        addCart.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addToCart(((Integer) qty.getValue()).intValue());
            }
        });
        addWish.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { addToWishlist(); }
        });

        categoryBox.setPreferredSize(new Dimension(190, 34));
        qty.setPreferredSize(new Dimension(62, 34));

        JPanel bar = Ui.row(10);
        bar.add(searchField);
        bar.add(categoryBox);
        bar.add(search);
        bar.add(new JLabel(" "));
        bar.add(Ui.eyebrow("Qty"));
        bar.add(qty);
        bar.add(addCart);
        bar.add(addWish);

        Ui.Card card = Ui.card();
        card.add(bar, BorderLayout.NORTH);
        JPanel body = Ui.transparent(new BorderLayout());
        body.setBorder(Ui.pad(14, 0, 0, 0));
        body.add(Ui.scroll(catalogTable), BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private void loadCategories() {
        categoryBox.addItem("All categories");
        try (Connection c = Db.open();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT name FROM category WHERE parent_category_id IS NOT NULL "
               + "ORDER BY name");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                categoryBox.addItem(rs.getString("name"));
            }
        } catch (SQLException e) {
            TableUtil.error(this, e);
        }
    }

    private void refreshCatalog() {
        String keyword  = "%" + searchField.getText().trim() + "%";
        Object selected = categoryBox.getSelectedItem();
        boolean all = selected == null || "All categories".equals(selected);

        // Price and stock come before category and store so the columns a shopper
        // actually decides on are visible without scrolling sideways.
        String sql =
            "SELECT variant_id, product_title AS product, variant_description AS variant, "
          + "       brand, price, total_stock AS stock, "
          + "       category_name AS category, store_name AS store "
          + "FROM   vw_product_catalog "
          + "WHERE  (product_title LIKE ? OR brand LIKE ?) ";

        if (all) {
            Ui.fill(catalogTable, sql + "ORDER BY product_title LIMIT 400", keyword, keyword);
        } else {
            Ui.fill(catalogTable, sql + "AND category_name = ? ORDER BY product_title LIMIT 400",
                    keyword, keyword, selected);
        }
    }

    private void addToCart(int quantity) {
        int row = catalogTable.getSelectedRow();
        if (row < 0) {
            TableUtil.info(this, "Select a product first.");
            return;
        }
        int variantId = TableUtil.intAt(catalogTable, row, 0);

        // (buyer_id, variant_id) is the primary key, so an existing line is topped up.
        String sql = "INSERT INTO cart_item (buyer_id, variant_id, quantity) VALUES (?, ?, ?) "
                   + "ON DUPLICATE KEY UPDATE quantity = quantity + ?";
        try (Connection c = Db.open();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, buyerId);
            ps.setInt(2, variantId);
            ps.setInt(3, quantity);
            ps.setInt(4, quantity);
            ps.executeUpdate();
            TableUtil.info(this, "Added to cart.");
        } catch (SQLException e) {
            TableUtil.error(this, e);
        }
    }

    private void addToWishlist() {
        int row = catalogTable.getSelectedRow();
        if (row < 0) {
            TableUtil.info(this, "Select a product first.");
            return;
        }
        int variantId = TableUtil.intAt(catalogTable, row, 0);
        try (Connection c = Db.open();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT IGNORE INTO wishlist_item (buyer_id, variant_id) VALUES (?, ?)")) {
            ps.setInt(1, buyerId);
            ps.setInt(2, variantId);
            ps.executeUpdate();
            TableUtil.info(this, "Saved to wishlist.");
        } catch (SQLException e) {
            TableUtil.error(this, e);
        }
    }

    // --------------------------------------------------------------- cart

    private JPanel cartPage() {
        JButton remove   = Ui.ghost("Remove selected");
        JButton checkout = Ui.accent("Place order");

        remove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { removeFromCart(); }
        });
        checkout.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { placeOrder(); }
        });

        addressBox.setPreferredSize(new Dimension(280, 34));
        methodBox.setPreferredSize(new Dimension(110, 34));

        JPanel bar = Ui.row(10);
        bar.add(Ui.eyebrow("Deliver to"));
        bar.add(addressBox);
        bar.add(Ui.eyebrow("Pay with"));
        bar.add(methodBox);
        bar.add(remove);
        bar.add(checkout);

        cartTotal.setFont(Theme.display(18));
        cartTotal.setForeground(Theme.INK);

        JPanel foot = Ui.transparent(new BorderLayout());
        foot.setBorder(Ui.pad(12, 2, 0, 2));
        foot.add(cartTotal, BorderLayout.WEST);

        Ui.Card card = Ui.card();
        card.add(bar, BorderLayout.NORTH);
        JPanel body = Ui.transparent(new BorderLayout());
        body.setBorder(Ui.pad(14, 0, 0, 0));
        body.add(Ui.scroll(cartTable), BorderLayout.CENTER);
        body.add(foot, BorderLayout.SOUTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private void loadAddresses() {
        addressIds.clear();
        addressBox.removeAllItems();
        try (Connection c = Db.open();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT address_id, house, street, city FROM address "
               + "WHERE owner_user_id = ? ORDER BY address_id")) {
            ps.setInt(1, buyerId);
            ResultSet rs = ps.executeQuery();
            try {
                while (rs.next()) {
                    addressIds.add(Integer.valueOf(rs.getInt("address_id")));
                    addressBox.addItem(rs.getString("house") + ", "
                        + rs.getString("street") + ", " + rs.getString("city"));
                }
            } finally {
                rs.close();
            }
        } catch (SQLException e) {
            TableUtil.error(this, e);
        }
    }

    private void refreshCart() {
        // Sub total is derived here, exactly as it is in the SQL layer.
        String sql =
            "SELECT ci.variant_id, p.title AS product, pv.description AS variant, "
          + "       s.name AS store, ci.quantity, pv.price, "
          + "       ci.quantity * pv.price AS sub_total "
          + "FROM   cart_item       ci "
          + "JOIN   product_variant pv ON pv.variant_id = ci.variant_id "
          + "JOIN   product         p  ON p.product_id  = pv.product_id "
          + "JOIN   store           s  ON s.store_id    = p.store_id "
          + "WHERE  ci.buyer_id = ? "
          + "ORDER BY s.name, p.title";
        Ui.fill(cartTable, sql, Integer.valueOf(buyerId));

        double total = 0;
        int col = cartTable.getColumnCount() - 1;
        java.util.Set<String> shops = new java.util.HashSet<String>();
        for (int row = 0; row < cartTable.getRowCount(); row++) {
            Object v = cartTable.getValueAt(row, col);
            if (v != null) {
                total += Double.parseDouble(v.toString());
            }
            Object st = cartTable.getValueAt(row, 3);
            if (st != null) {
                shops.add(st.toString());
            }
        }
        cartTotal.setText(Theme.money(Double.valueOf(total)) + "   "
            + (cartTable.getRowCount() == 0 ? "" :
               "· " + cartTable.getRowCount() + " line" + (cartTable.getRowCount() == 1 ? "" : "s")
               + " from " + shops.size() + " seller" + (shops.size() == 1 ? "" : "s")));
    }

    private void removeFromCart() {
        int row = cartTable.getSelectedRow();
        if (row < 0) {
            TableUtil.info(this, "Select a cart line first.");
            return;
        }
        int variantId = TableUtil.intAt(cartTable, row, 0);
        try (Connection c = Db.open();
             PreparedStatement ps = c.prepareStatement(
                 "DELETE FROM cart_item WHERE buyer_id = ? AND variant_id = ?")) {
            ps.setInt(1, buyerId);
            ps.setInt(2, variantId);
            ps.executeUpdate();
            refreshCart();
        } catch (SQLException e) {
            TableUtil.error(this, e);
        }
    }

    private void placeOrder() {
        if (cartTable.getRowCount() == 0) {
            TableUtil.info(this, "Your cart is empty.");
            return;
        }
        int idx = addressBox.getSelectedIndex();
        if (idx < 0) {
            TableUtil.info(this, "No delivery address is saved on this account.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Place this order?\nItems from different sellers become separate orders.",
            "Confirm order", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            int checkoutId = CheckoutService.placeOrder(
                buyerId, addressIds.get(idx).intValue(), (String) methodBox.getSelectedItem());
            refreshCart();
            showPage("orders");
            refreshOrders();
            selectCheckout(checkoutId);
            ribbon.load(checkoutId);
        } catch (SQLException e) {
            TableUtil.error(this, e);
        }
    }

    // ------------------------------------------------------------- orders

    private JPanel ordersPage() {
        JButton refresh = Ui.ghost("Refresh");
        refresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { refreshOrders(); }
        });

        ordersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ordersTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int row = ordersTable.getSelectedRow();
                    if (row >= 0) {
                        ribbon.load(TableUtil.intAt(ordersTable, row, 1));
                    }
                }
            }
        });

        JPanel bar = Ui.row(10);
        bar.add(refresh);

        Ui.Card list = Ui.card();
        list.add(bar, BorderLayout.NORTH);
        JPanel lb = Ui.transparent(new BorderLayout());
        lb.setBorder(Ui.pad(14, 0, 0, 0));
        lb.add(Ui.scroll(ordersTable), BorderLayout.CENTER);
        list.add(lb, BorderLayout.CENTER);

        Ui.Card split = Ui.card("How this basket was split", ribbon);
        split.setPreferredSize(new Dimension(500, 10));

        JPanel p = Ui.transparent(new BorderLayout(16, 0));
        p.add(list, BorderLayout.CENTER);
        p.add(split, BorderLayout.EAST);
        return p;
    }

    private void refreshOrders() {
        String sql =
            "SELECT order_id, checkout_id, store_name AS store, order_status AS status, "
          + "       net_amount, checkout_date AS placed "
          + "FROM   vw_order_summary WHERE buyer_id = ? "
          + "ORDER BY checkout_id DESC, order_id";
        Ui.fill(ordersTable, sql, Integer.valueOf(buyerId));
        if (ordersTable.getRowCount() == 0) {
            ribbon.clear("No orders yet. Add something to your cart and check out.");
            return;
        }
        // Prefer a basket that actually split, since that is the case worth showing.
        int pick = 0;
        for (int r = 0; r + 1 < ordersTable.getRowCount(); r++) {
            if (TableUtil.intAt(ordersTable, r, 1) == TableUtil.intAt(ordersTable, r + 1, 1)) {
                pick = r;
                break;
            }
        }
        ordersTable.setRowSelectionInterval(pick, pick);
    }

    private void selectCheckout(int checkoutId) {
        for (int r = 0; r < ordersTable.getRowCount(); r++) {
            if (TableUtil.intAt(ordersTable, r, 1) == checkoutId) {
                ordersTable.setRowSelectionInterval(r, r);
                return;
            }
        }
    }

    // ----------------------------------------------------------- wishlist

    private JPanel wishPage() {
        JButton toCart = Ui.primary("Move to cart");
        JButton remove = Ui.ghost("Remove");
        toCart.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { wishlistToCart(); }
        });
        remove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { removeWish(); }
        });

        JPanel bar = Ui.row(10);
        bar.add(toCart);
        bar.add(remove);

        Ui.Card card = Ui.card();
        card.add(bar, BorderLayout.NORTH);
        JPanel body = Ui.transparent(new BorderLayout());
        body.setBorder(Ui.pad(14, 0, 0, 0));
        body.add(Ui.scroll(wishTable), BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private void refreshWishlist() {
        Ui.fill(wishTable,
            "SELECT w.variant_id, p.title AS product, pv.description AS variant, "
          + "       s.name AS store, pv.price, w.added_at AS saved "
          + "FROM   wishlist_item   w "
          + "JOIN   product_variant pv ON pv.variant_id = w.variant_id "
          + "JOIN   product         p  ON p.product_id  = pv.product_id "
          + "JOIN   store           s  ON s.store_id    = p.store_id "
          + "WHERE  w.buyer_id = ? ORDER BY w.added_at DESC",
            Integer.valueOf(buyerId));
    }

    private void wishlistToCart() {
        int row = wishTable.getSelectedRow();
        if (row < 0) {
            TableUtil.info(this, "Select a wishlist item first.");
            return;
        }
        int variantId = TableUtil.intAt(wishTable, row, 0);
        try (Connection c = Db.open()) {
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO cart_item (buyer_id, variant_id, quantity) VALUES (?, ?, 1) "
              + "ON DUPLICATE KEY UPDATE quantity = quantity + 1");
            ps.setInt(1, buyerId);
            ps.setInt(2, variantId);
            ps.executeUpdate();
            ps.close();

            ps = c.prepareStatement(
                "DELETE FROM wishlist_item WHERE buyer_id = ? AND variant_id = ?");
            ps.setInt(1, buyerId);
            ps.setInt(2, variantId);
            ps.executeUpdate();
            ps.close();

            refreshWishlist();
            TableUtil.info(this, "Moved to cart.");
        } catch (SQLException e) {
            TableUtil.error(this, e);
        }
    }

    private void removeWish() {
        int row = wishTable.getSelectedRow();
        if (row < 0) {
            TableUtil.info(this, "Select a wishlist item first.");
            return;
        }
        try (Connection c = Db.open();
             PreparedStatement ps = c.prepareStatement(
                 "DELETE FROM wishlist_item WHERE buyer_id = ? AND variant_id = ?")) {
            ps.setInt(1, buyerId);
            ps.setInt(2, TableUtil.intAt(wishTable, row, 0));
            ps.executeUpdate();
            refreshWishlist();
        } catch (SQLException e) {
            TableUtil.error(this, e);
        }
    }

    // ------------------------------------------------------------ profile

    private JPanel profilePage() {
        statOrders = Ui.stat("Vendor orders", "0", "across all baskets", Theme.DEEP, "orders");
        statSpend  = Ui.stat("Lifetime spend", "0", "paid to date", Theme.OK, "chart");
        statCart   = Ui.stat("In cart", "0", "waiting to be ordered", Theme.WARN, "cart");
        statPoints = Ui.stat("Loyalty points", "0", "earned on the platform",
                             Theme.MARIGOLD_DK, "heart");

        JPanel tiles = Ui.statRow(statOrders, statSpend, statCart, statPoints);

        Ui.Card detail = Ui.card("Account detail", Ui.bare(profileTable));
        JPanel p = Ui.transparent(new BorderLayout(0, 16));
        p.add(tiles, BorderLayout.NORTH);
        p.add(detail, BorderLayout.CENTER);
        return p;
    }

    private void refreshProfile() {
        Ui.fill(profileTable,
            "SELECT buyer_name, email, dob, age, loyalty_points, total_checkouts, "
          + "       lifetime_spend FROM vw_buyer_profile WHERE buyer_id = ?",
            Integer.valueOf(buyerId));

        try (Connection c = Db.open()) {
            setStat(statSpend, one(c,
                "SELECT IFNULL(SUM(total_amount),0) FROM checkout WHERE buyer_id = ?", buyerId),
                true);
            setStat(statOrders, one(c,
                "SELECT COUNT(*) FROM customer_order o JOIN checkout ck "
              + "ON ck.checkout_id = o.checkout_id WHERE ck.buyer_id = ?", buyerId), false);
            setStat(statCart, one(c,
                "SELECT IFNULL(SUM(quantity),0) FROM cart_item WHERE buyer_id = ?", buyerId),
                false);
            setStat(statPoints, one(c,
                "SELECT loyalty_points FROM buyer WHERE user_id = ?", buyerId), false);
        } catch (SQLException e) {
            TableUtil.error(this, e);
        }
    }

    private static String one(Connection c, String sql, int id) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        try {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            try {
                return rs.next() ? String.valueOf(rs.getObject(1)) : "0";
            } finally {
                rs.close();
            }
        } finally {
            ps.close();
        }
    }

    private static void setStat(Ui.Card card, String value, boolean money) {
        JPanel body = (JPanel) ((BorderLayout) card.getLayout())
                        .getLayoutComponent(BorderLayout.CENTER);
        JLabel v = (JLabel) ((BorderLayout) body.getLayout())
                        .getLayoutComponent(BorderLayout.NORTH);
        v.setText(money ? Theme.moneyShort(value) : value);
    }
}
