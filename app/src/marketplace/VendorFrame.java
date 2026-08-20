package marketplace;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Vendor dashboard: sales overview, fulfilment queue, catalogue and restock alerts. */
public class VendorFrame extends Shell {

    private final int    vendorId;
    private final String vendorName;

    private final JTable ordersTable    = Ui.table();
    private final JTable productsTable  = Ui.table();
    private final JTable lowStockTable  = Ui.table();
    private final JTable breakdownTable = Ui.table();
    private final JTable promoTable     = Ui.table();

    private final JComboBox<String> statusBox = new JComboBox<String>(
        new String[] { "PLACED", "PACKED", "SHIPPED", "DELIVERED", "CANCELLED" });

    private Ui.Card tRevenue, tOrders, tUnits, tLow;

    public VendorFrame(int vendorId, String vendorName) {
        super("Marketplace", vendorName, "Vendor");
        this.vendorId   = vendorId;
        this.vendorName = vendorName;

        page("overview", "Overview", "Sales overview",
             "Revenue after platform commission, excluding cancelled orders.",
             "chart", overviewPage());
        page("fulfil", "Orders", "Orders to fulfil",
             "Only orders belonging to your storefronts appear here.", "orders", ordersPage());
        page("catalogue", "My Products", "My catalogue",
             "Every variant listed under your storefronts.", "store", productsPage());
        page("stock", "Low Stock", "Restock alerts",
             "Variants at or below their own restock threshold.", "alert", stockPage());
        page("promos", "Promotions", "My promotions",
             "Discounts you created and the variants they cover.", "split", promoPage());

        showPage("overview");
    }

    @Override
    protected void onPageShown(String key) {
        if ("overview".equals(key))       refreshOverview();
        else if ("fulfil".equals(key))    refreshOrders();
        else if ("catalogue".equals(key)) refreshProducts();
        else if ("stock".equals(key))     refreshLowStock();
        else if ("promos".equals(key))    refreshPromos();
    }

    // ----------------------------------------------------------- overview

    private JPanel overviewPage() {
        tRevenue = Ui.stat("Gross revenue", "0", "delivered and in flight", Theme.DEEP, "chart");
        tOrders  = Ui.stat("Orders", "0", "excluding cancelled", Theme.INFO, "orders");
        tUnits   = Ui.stat("Units sold", "0", "across all storefronts", Theme.OK, "box");
        tLow     = Ui.stat("Needs restock", "0", "variants below threshold",
                           Theme.DANGER, "alert");

        Ui.Card table = Ui.card("Revenue per product", Ui.bare(breakdownTable));

        JPanel p = Ui.transparent(new BorderLayout(0, 16));
        p.add(Ui.statRow(tRevenue, tOrders, tUnits, tLow), BorderLayout.NORTH);
        p.add(table, BorderLayout.CENTER);
        return p;
    }

    private void refreshOverview() {
        String sql =
            "SELECT p.title AS product, SUM(oi.quantity) AS units, "
          + "       SUM(oi.quantity * oi.unit_price) AS revenue, "
          + "       COUNT(DISTINCT o.order_id) AS orders "
          + "FROM   store           s "
          + "JOIN   customer_order  o  ON o.store_id    = s.store_id "
          + "JOIN   order_item      oi ON oi.order_id   = o.order_id "
          + "JOIN   product_variant pv ON pv.variant_id = oi.variant_id "
          + "JOIN   product         p  ON p.product_id  = pv.product_id "
          + "WHERE  s.vendor_id = ? AND o.status <> 'CANCELLED' "
          + "GROUP BY p.title ORDER BY revenue DESC";
        Ui.fill(breakdownTable, sql, Integer.valueOf(vendorId));

        try (Connection c = Db.open()) {
            // sp_vendor_sales_report (db/routines.sql) wraps vw_vendor_sales; column
            // order there is gross_revenue, orders_count, units_sold first, matching
            // what row() reads below.
            String[] r = row(c, "{call sp_vendor_sales_report(?)}", vendorId, 3);
            setStat(tRevenue, r == null ? "0" : Theme.moneyShort(r[0]));
            setStat(tOrders,  r == null ? "0" : r[1]);
            setStat(tUnits,   r == null ? "0" : r[2]);

            String[] low = row(c,
                "SELECT COUNT(*) FROM vw_low_stock WHERE vendor_id = ?", vendorId, 1);
            setStat(tLow, low == null ? "0" : low[0]);
        } catch (SQLException e) {
            TableUtil.error(this, e);
        }
    }

    // ------------------------------------------------------------- orders

    private JPanel ordersPage() {
        JButton update = Ui.primary("Update status");
        update.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { updateStatus(); }
        });
        statusBox.setPreferredSize(new Dimension(150, 34));

        JPanel bar = Ui.row(10);
        bar.add(Ui.eyebrow("Set selected order to"));
        bar.add(statusBox);
        bar.add(update);

        Ui.Card card = Ui.card();
        card.add(bar, BorderLayout.NORTH);
        JPanel body = Ui.transparent(new BorderLayout());
        body.setBorder(Ui.pad(14, 0, 0, 0));
        body.add(Ui.scroll(ordersTable), BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private void refreshOrders() {
        Ui.fill(ordersTable,
            "SELECT order_id, checkout_id, buyer_name AS buyer, store_name AS store, "
          + "       order_status AS status, shipment_status AS delivery, units, net_amount "
          + "FROM   vw_order_summary WHERE vendor_id = ? ORDER BY order_id DESC LIMIT 500",
            Integer.valueOf(vendorId));
    }

    private void updateStatus() {
        int row = ordersTable.getSelectedRow();
        if (row < 0) {
            TableUtil.info(this, "Select an order first.");
            return;
        }
        int    orderId = TableUtil.intAt(ordersTable, row, 0);
        String status  = (String) statusBox.getSelectedItem();

        // The join guards against a vendor updating another vendor's order.
        String sql =
            "UPDATE customer_order o "
          + "JOIN   store          s ON s.store_id = o.store_id "
          + "SET    o.status = ? "
          + "WHERE  o.order_id = ? AND s.vendor_id = ?";
        try (Connection c = Db.open();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, orderId);
            ps.setInt(3, vendorId);
            if (ps.executeUpdate() == 0) {
                TableUtil.info(this, "That order does not belong to your storefronts.");
            } else {
                refreshOrders();
            }
        } catch (SQLException e) {
            TableUtil.error(this, e);
        }
    }

    // ---------------------------------------------------------- catalogue

    private JPanel productsPage() {
        JButton refresh = Ui.ghost("Refresh");
        refresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { refreshProducts(); }
        });
        JPanel bar = Ui.row(10);
        bar.add(refresh);

        Ui.Card card = Ui.card();
        card.add(bar, BorderLayout.NORTH);
        JPanel body = Ui.transparent(new BorderLayout());
        body.setBorder(Ui.pad(14, 0, 0, 0));
        body.add(Ui.scroll(productsTable), BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private void refreshProducts() {
        Ui.fill(productsTable,
            "SELECT s.name AS store, p.title AS product, pv.variant_id, "
          + "       pv.description AS variant, pv.barcode, pv.price, "
          + "       IFNULL(SUM(i.quantity), 0) AS stock, pv.low_stock_threshold AS threshold "
          + "FROM        store           s "
          + "JOIN        product         p  ON p.store_id    = s.store_id "
          + "JOIN        product_variant pv ON pv.product_id = p.product_id "
          + "LEFT JOIN   inventory       i  ON i.variant_id  = pv.variant_id "
          + "WHERE  s.vendor_id = ? "
          + "GROUP BY s.name, p.title, pv.variant_id, pv.description, pv.barcode, "
          + "         pv.price, pv.low_stock_threshold "
          + "ORDER BY s.name, p.title",
            Integer.valueOf(vendorId));
    }

    // -------------------------------------------------------------- stock

    private JPanel stockPage() {
        JButton refresh = Ui.ghost("Refresh");
        refresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { refreshLowStock(); }
        });
        JPanel bar = Ui.row(10);
        bar.add(refresh);

        Ui.Card card = Ui.card();
        card.add(bar, BorderLayout.NORTH);
        JPanel body = Ui.transparent(new BorderLayout());
        body.setBorder(Ui.pad(14, 0, 0, 0));
        body.add(Ui.scroll(lowStockTable), BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private void refreshLowStock() {
        Ui.fill(lowStockTable,
            "SELECT store_name AS store, product_title AS product, "
          + "       variant_description AS variant, barcode, "
          + "       stock_on_hand AS stock, low_stock_threshold AS threshold "
          + "FROM   vw_low_stock WHERE vendor_id = ? ORDER BY stock_on_hand",
            Integer.valueOf(vendorId));
    }

    // --------------------------------------------------------- promotions

    private JPanel promoPage() {
        Ui.Card card = Ui.card("Promotions created by this vendor", Ui.bare(promoTable));
        return card;
    }

    private void refreshPromos() {
        Ui.fill(promoTable,
            "SELECT pr.code, pr.reason, pr.discount_type AS type, pr.amount, "
          + "       pr.min_order_amount AS min_basket, pr.start_date, pr.end_date, "
          + "       COUNT(pv.variant_id) AS variants "
          + "FROM        promotion          pr "
          + "LEFT JOIN   promotion_variant  pv ON pv.promotion_id = pr.promotion_id "
          + "WHERE  pr.vendor_id = ? "
          + "GROUP BY pr.promotion_id, pr.code, pr.reason, pr.discount_type, pr.amount, "
          + "         pr.min_order_amount, pr.start_date, pr.end_date "
          + "ORDER BY pr.start_date DESC",
            Integer.valueOf(vendorId));
    }

    // ------------------------------------------------------------ helpers

    private static String[] row(Connection c, String sql, int id, int cols) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        try {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            try {
                if (!rs.next()) {
                    return null;
                }
                String[] out = new String[cols];
                for (int i = 0; i < cols; i++) {
                    out[i] = String.valueOf(rs.getObject(i + 1));
                }
                return out;
            } finally {
                rs.close();
            }
        } finally {
            ps.close();
        }
    }

    private static void setStat(Ui.Card card, String value) {
        JPanel body = (JPanel) ((BorderLayout) card.getLayout())
                        .getLayoutComponent(BorderLayout.CENTER);
        JLabel v = (JLabel) ((BorderLayout) body.getLayout())
                        .getLayoutComponent(BorderLayout.NORTH);
        v.setText(value);
    }
}
