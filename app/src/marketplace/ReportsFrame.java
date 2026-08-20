package marketplace;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Reporting console. Each entry runs one statement from queries.sql and shows
 * the SQL beside its result, which is what the project demonstration walks
 * through.
 */
public class ReportsFrame extends JFrame {

    /** A named report: a caption and the statement behind it. */
    private static final class Report {
        final String label;
        final String topic;
        final String sql;

        Report(String label, String topic, String sql) {
            this.label = label;
            this.topic = topic;
            this.sql   = sql;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final Report[] REPORTS = {

        new Report("Vendor revenue leaderboard", "View, GROUP BY, computed commission",
            "SELECT business_name, orders_count, units_sold, gross_revenue,\n"
          + "       platform_commission, vendor_earning\n"
          + "FROM   vw_vendor_sales\n"
          + "ORDER BY gross_revenue DESC"),

        new Report("Baskets split across several vendors", "GROUP BY with HAVING",
            "SELECT ck.checkout_id,\n"
          + "       CONCAT(u.first_name,' ',u.last_name) AS buyer,\n"
          + "       COUNT(o.order_id)                    AS vendor_orders,\n"
          + "       GROUP_CONCAT(s.name ORDER BY s.name SEPARATOR ' | ') AS stores,\n"
          + "       ck.total_amount\n"
          + "FROM   checkout       ck\n"
          + "JOIN   `user`         u ON u.user_id     = ck.buyer_id\n"
          + "JOIN   customer_order o ON o.checkout_id = ck.checkout_id\n"
          + "JOIN   store          s ON s.store_id    = o.store_id\n"
          + "GROUP BY ck.checkout_id, u.first_name, u.last_name, ck.total_amount\n"
          + "HAVING COUNT(o.order_id) > 1\n"
          + "ORDER BY vendor_orders DESC, ck.checkout_id DESC\n"
          + "LIMIT 200"),

        new Report("Category tree", "Self join on a recursive relationship",
            "SELECT child.category_id AS sub_category_id,\n"
          + "       child.name        AS sub_category,\n"
          + "       parent.name       AS parent_category\n"
          + "FROM   category child\n"
          + "JOIN   category parent ON child.parent_category_id = parent.category_id\n"
          + "ORDER BY parent.name, child.name"),

        new Report("Variants priced above the catalogue average", "Single row subquery",
            "SELECT barcode, description, price\n"
          + "FROM   product_variant\n"
          + "WHERE  price > (SELECT AVG(price) FROM product_variant)\n"
          + "ORDER BY price DESC\n"
          + "LIMIT 200"),

        new Report("Variants nobody has ever ordered", "Multi row subquery with NOT IN",
            "SELECT pv.variant_id, pv.barcode, pv.description, pv.price\n"
          + "FROM   product_variant pv\n"
          + "WHERE  pv.variant_id NOT IN (SELECT variant_id FROM order_item)\n"
          + "ORDER BY pv.price DESC\n"
          + "LIMIT 200"),

        new Report("Best selling storefronts", "GROUP BY with HAVING",
            "SELECT s.name AS store,\n"
          + "       COUNT(DISTINCT o.order_id)       AS orders,\n"
          + "       SUM(oi.quantity)                 AS units,\n"
          + "       SUM(oi.quantity * oi.unit_price) AS revenue\n"
          + "FROM   store          s\n"
          + "JOIN   customer_order o  ON o.store_id  = s.store_id\n"
          + "JOIN   order_item     oi ON oi.order_id = o.order_id\n"
          + "WHERE  o.status <> 'CANCELLED'\n"
          + "GROUP BY s.name\n"
          + "HAVING SUM(oi.quantity * oi.unit_price) > 20000\n"
          + "ORDER BY revenue DESC"),

        new Report("Worst rated products", "Aggregate over a three table join",
            "SELECT p.title, p.brand, COUNT(r.stars) AS reviews,\n"
          + "       ROUND(AVG(r.stars),2) AS avg_stars\n"
          + "FROM   product         p\n"
          + "JOIN   product_variant pv ON pv.product_id = p.product_id\n"
          + "JOIN   review          r  ON r.variant_id  = pv.variant_id\n"
          + "GROUP BY p.title, p.brand\n"
          + "HAVING COUNT(r.stars) >= 2\n"
          + "ORDER BY avg_stars ASC\n"
          + "LIMIT 200"),

        new Report("Low stock restock alert", "View built on GROUP BY and HAVING",
            "SELECT store_name, product_title, variant_description,\n"
          + "       stock_on_hand, low_stock_threshold\n"
          + "FROM   vw_low_stock\n"
          + "ORDER BY stock_on_hand\n"
          + "LIMIT 200"),

        new Report("Baskets with no payment", "LEFT OUTER JOIN finding gaps",
            "SELECT ck.checkout_id, ck.buyer_id, ck.checkout_date,\n"
          + "       ck.total_amount, pay.payment_id\n"
          + "FROM   checkout ck\n"
          + "LEFT OUTER JOIN payment pay ON pay.checkout_id = ck.checkout_id\n"
          + "WHERE  pay.payment_id IS NULL\n"
          + "ORDER BY ck.checkout_date DESC"),

        new Report("Couriers never used", "RIGHT OUTER JOIN",
            "SELECT sc.courier_name, COUNT(sh.shipment_id) AS parcels\n"
          + "FROM   shipment sh\n"
          + "RIGHT OUTER JOIN shipment_company sc ON sc.company_id = sh.company_id\n"
          + "GROUP BY sc.courier_name\n"
          + "ORDER BY parcels"),

        new Report("Registered buyers who never ordered", "NOT EXISTS",
            "SELECT CONCAT(u.first_name,' ',u.last_name) AS buyer, u.email\n"
          + "FROM   buyer  b\n"
          + "JOIN   `user` u ON u.user_id = b.user_id\n"
          + "WHERE  NOT EXISTS (SELECT 1 FROM checkout ck WHERE ck.buyer_id = b.user_id)\n"
          + "ORDER BY buyer"),

        new Report("Customers spending above average", "Subquery over a view",
            "SELECT buyer_name, age, total_checkouts, lifetime_spend\n"
          + "FROM   vw_buyer_profile\n"
          + "WHERE  lifetime_spend > (SELECT AVG(lifetime_spend) FROM vw_buyer_profile)\n"
          + "ORDER BY lifetime_spend DESC\n"
          + "LIMIT 200"),

        new Report("Payment methods used", "GROUP BY on an enumerated column",
            "SELECT payment_method, COUNT(*) AS payments,\n"
          + "       SUM(paid_amount) AS total_collected,\n"
          + "       ROUND(AVG(paid_amount),2) AS average_basket\n"
          + "FROM   payment\n"
          + "GROUP BY payment_method\n"
          + "ORDER BY payments DESC"),

        new Report("Sales by district", "Join through address to group geographically",
            "SELECT a.city AS district, COUNT(DISTINCT ck.checkout_id) AS baskets,\n"
          + "       SUM(ck.total_amount) AS total_amount\n"
          + "FROM   checkout ck\n"
          + "JOIN   address  a ON a.address_id = ck.shipping_address_id\n"
          + "GROUP BY a.city\n"
          + "ORDER BY total_amount DESC"),

        new Report("Stock held per warehouse", "Aggregate across an M:N table",
            "SELECT w.name AS warehouse, COUNT(i.variant_id) AS variant_lines,\n"
          + "       SUM(i.quantity) AS total_units\n"
          + "FROM   warehouse w\n"
          + "JOIN   inventory i ON i.warehouse_id = w.warehouse_id\n"
          + "GROUP BY w.name\n"
          + "ORDER BY total_units DESC"),

        new Report("Full order line items", "Seven table join",
            "SELECT o.order_id,\n"
          + "       CONCAT(u.first_name,' ',u.last_name) AS buyer,\n"
          + "       s.name AS store, p.title AS product,\n"
          + "       oi.quantity, oi.unit_price,\n"
          + "       oi.quantity * oi.unit_price AS sub_total\n"
          + "FROM   customer_order  o\n"
          + "JOIN   checkout        ck ON ck.checkout_id = o.checkout_id\n"
          + "JOIN   `user`          u  ON u.user_id      = ck.buyer_id\n"
          + "JOIN   store           s  ON s.store_id     = o.store_id\n"
          + "JOIN   order_item      oi ON oi.order_id    = o.order_id\n"
          + "JOIN   product_variant pv ON pv.variant_id  = oi.variant_id\n"
          + "JOIN   product         p  ON p.product_id   = pv.product_id\n"
          + "ORDER BY o.order_id DESC\n"
          + "LIMIT 300"),

        new Report("Support workload per agent", "LEFT JOIN so idle agents still appear",
            "SELECT CONCAT(u.first_name,' ',u.last_name) AS agent,\n"
          + "       sp.response_time_min, COUNT(t.ticket_id) AS tickets_handled\n"
          + "FROM   support sp\n"
          + "JOIN   `user`  u ON u.user_id    = sp.user_id\n"
          + "LEFT JOIN ticket t ON t.support_id = sp.user_id\n"
          + "GROUP BY u.first_name, u.last_name, sp.response_time_min\n"
          + "ORDER BY tickets_handled DESC")
    };

    private final JComboBox<Report> reportBox = new JComboBox<Report>(REPORTS);
    private final JTextArea         sqlArea   = new JTextArea();
    private final JTable            results   = Ui.table();
    private final JLabel            rowCount  = new JLabel(" ");
    private final JLabel            topic     = Ui.sub(" ");

    public ReportsFrame() {
        super("Marketplace - Reporting Console");
        Theme.install();

        JButton run = Ui.primary("Run");
        run.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { run(); }
        });
        reportBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { showSql(); run(); }
        });
        reportBox.setPreferredSize(new Dimension(360, 36));
        reportBox.setFont(Theme.body(13));

        JPanel head = Ui.transparent(new BorderLayout());
        head.setBorder(BorderFactory.createEmptyBorder(24, 28, 12, 28));
        JPanel titles = Ui.transparent(new BorderLayout(0, 3));
        titles.add(Ui.h1("Reporting console"), BorderLayout.NORTH);
        titles.add(topic, BorderLayout.CENTER);
        JPanel controls = Ui.row(10);
        controls.add(reportBox);
        controls.add(run);
        head.add(titles, BorderLayout.CENTER);
        head.add(controls, BorderLayout.EAST);

        sqlArea.setEditable(false);
        sqlArea.setFont(Theme.mono(13));
        sqlArea.setForeground(Theme.INK);
        sqlArea.setBackground(Theme.SURFACE);
        sqlArea.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JScrollPane sqlScroll = Ui.bare(sqlArea);
        sqlScroll.setPreferredSize(new Dimension(10, 210));
        Ui.Card sqlCard = Ui.card("SQL", sqlScroll);

        rowCount.setFont(Theme.body(12));
        rowCount.setForeground(Theme.MUTED);
        JPanel resultHead = Ui.transparent(new BorderLayout());
        resultHead.setBorder(Ui.pad(0, 0, 12, 0));
        resultHead.add(Ui.sectionTitle("Result"), BorderLayout.WEST);
        resultHead.add(rowCount, BorderLayout.EAST);

        Ui.Card resultCard = Ui.card();
        resultCard.add(resultHead, BorderLayout.NORTH);
        resultCard.add(Ui.bare(results), BorderLayout.CENTER);

        JPanel body = Ui.transparent(new BorderLayout(0, 16));
        body.setBorder(BorderFactory.createEmptyBorder(0, 28, 24, 28));
        body.add(sqlCard, BorderLayout.NORTH);
        body.add(resultCard, BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.PAPER);
        root.add(head, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);

        add(root);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1180, 800);
        setLocationRelativeTo(null);

        showSql();
        run();
    }

    private void showSql() {
        Report r = (Report) reportBox.getSelectedItem();
        if (r != null) {
            sqlArea.setText(r.sql);
            sqlArea.setCaretPosition(0);
            topic.setText(r.topic);
        }
    }

    private void run() {
        Report r = (Report) reportBox.getSelectedItem();
        if (r == null) {
            return;
        }
        Ui.fill(results, r.sql);
        rowCount.setText(results.getRowCount() + " rows");
    }
}
