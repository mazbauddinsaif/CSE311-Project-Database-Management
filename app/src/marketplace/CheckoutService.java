package marketplace;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a buyer's cart into one checkout and one order per store.
 *
 * This is the vendor-splitting rule from the requirements. The buyer pays
 * once, so a single row goes into `checkout`; fulfilment happens per vendor,
 * so the cart is grouped by store and each group becomes its own
 * `customer_order`. Everything runs inside one transaction: if any line
 * fails the stock check, the whole basket rolls back and nothing is written.
 */
public final class CheckoutService {

    private CheckoutService() { }

    /** One cart line, already resolved to the store that will fulfil it. */
    private static final class Line {
        int        variantId;
        int        quantity;
        BigDecimal unitPrice;
        int        storeId;
        String     title;
    }

    private static final String CART_SQL =
        "SELECT ci.variant_id, ci.quantity, pv.price, p.store_id, p.title "
      + "FROM   cart_item       ci "
      + "JOIN   product_variant pv ON pv.variant_id = ci.variant_id "
      + "JOIN   product         p  ON p.product_id  = pv.product_id "
      + "WHERE  ci.buyer_id = ? "
      + "ORDER BY p.store_id, ci.variant_id";

    private static final String STOCK_SQL =
        "SELECT IFNULL(SUM(quantity), 0) FROM inventory WHERE variant_id = ?";

    private static final String PICK_WAREHOUSE_SQL =
        "SELECT warehouse_id, quantity FROM inventory "
      + "WHERE variant_id = ? AND quantity > 0 ORDER BY quantity DESC";

    /**
     * @return the new checkout id
     * @throws SQLException if the cart is empty or a line has insufficient stock
     */
    public static int placeOrder(int buyerId, int addressId, String paymentMethod)
            throws SQLException {

        Connection c = null;
        try {
            c = Db.open();
            c.setAutoCommit(false);

            List<Line> lines = readCart(c, buyerId);
            if (lines.isEmpty()) {
                throw new SQLException("Your cart is empty.");
            }
            verifyStock(c, lines);

            // Group the cart by store. LinkedHashMap keeps the store order stable
            // so the orders created below are numbered predictably.
            Map<Integer, List<Line>> byStore = new LinkedHashMap<Integer, List<Line>>();
            for (Line line : lines) {
                List<Line> group = byStore.get(line.storeId);
                if (group == null) {
                    group = new ArrayList<Line>();
                    byStore.put(line.storeId, group);
                }
                group.add(line);
            }

            int        checkoutId  = insertCheckout(c, buyerId, addressId);
            BigDecimal basketTotal = BigDecimal.ZERO;

            for (Map.Entry<Integer, List<Line>> entry : byStore.entrySet()) {
                BigDecimal orderTotal = BigDecimal.ZERO;
                for (Line line : entry.getValue()) {
                    orderTotal = orderTotal.add(
                        line.unitPrice.multiply(BigDecimal.valueOf(line.quantity)));
                }

                int orderId = insertOrder(c, checkoutId, entry.getKey(), orderTotal);
                insertItems(c, orderId, entry.getValue());
                basketTotal = basketTotal.add(orderTotal);
            }

            updateCheckoutTotal(c, checkoutId, basketTotal);
            insertPayment(c, checkoutId, buyerId, paymentMethod, basketTotal);

            for (Line line : lines) {
                deductStock(c, line.variantId, line.quantity);
            }
            clearCart(c, buyerId);

            c.commit();
            return checkoutId;

        } catch (SQLException e) {
            if (c != null) {
                try {
                    c.rollback();
                } catch (SQLException ignored) {
                    // the original exception is the useful one
                }
            }
            throw e;
        } finally {
            if (c != null) {
                try {
                    c.setAutoCommit(true);
                    c.close();
                } catch (SQLException ignored) {
                    // nothing useful to do while closing
                }
            }
        }
    }

    private static List<Line> readCart(Connection c, int buyerId) throws SQLException {
        List<Line> lines = new ArrayList<Line>();
        PreparedStatement ps = c.prepareStatement(CART_SQL);
        try {
            ps.setInt(1, buyerId);
            ResultSet rs = ps.executeQuery();
            try {
                while (rs.next()) {
                    Line line     = new Line();
                    line.variantId = rs.getInt("variant_id");
                    line.quantity  = rs.getInt("quantity");
                    line.unitPrice = rs.getBigDecimal("price");
                    line.storeId   = rs.getInt("store_id");
                    line.title     = rs.getString("title");
                    lines.add(line);
                }
            } finally {
                rs.close();
            }
        } finally {
            ps.close();
        }
        return lines;
    }

    private static void verifyStock(Connection c, List<Line> lines) throws SQLException {
        PreparedStatement ps = c.prepareStatement(STOCK_SQL);
        try {
            for (Line line : lines) {
                ps.setInt(1, line.variantId);
                ResultSet rs = ps.executeQuery();
                try {
                    int available = rs.next() ? rs.getInt(1) : 0;
                    if (available < line.quantity) {
                        throw new SQLException("Not enough stock for " + line.title
                            + ". Requested " + line.quantity + ", available " + available + ".");
                    }
                } finally {
                    rs.close();
                }
            }
        } finally {
            ps.close();
        }
    }

    private static int insertCheckout(Connection c, int buyerId, int addressId)
            throws SQLException {
        PreparedStatement ps = c.prepareStatement(
            "INSERT INTO checkout (buyer_id, shipping_address_id, total_amount) VALUES (?, ?, 0)",
            Statement.RETURN_GENERATED_KEYS);
        try {
            ps.setInt(1, buyerId);
            ps.setInt(2, addressId);
            ps.executeUpdate();
            return firstKey(ps);
        } finally {
            ps.close();
        }
    }

    private static int insertOrder(Connection c, int checkoutId, int storeId, BigDecimal total)
            throws SQLException {
        PreparedStatement ps = c.prepareStatement(
            "INSERT INTO customer_order (checkout_id, store_id, status, total_amount) "
          + "VALUES (?, ?, 'PLACED', ?)",
            Statement.RETURN_GENERATED_KEYS);
        try {
            ps.setInt(1, checkoutId);
            ps.setInt(2, storeId);
            ps.setBigDecimal(3, total);
            ps.executeUpdate();
            return firstKey(ps);
        } finally {
            ps.close();
        }
    }

    private static void insertItems(Connection c, int orderId, List<Line> lines)
            throws SQLException {
        PreparedStatement ps = c.prepareStatement(
            "INSERT INTO order_item (order_id, variant_id, quantity, unit_price) "
          + "VALUES (?, ?, ?, ?)");
        try {
            for (Line line : lines) {
                ps.setInt(1, orderId);
                ps.setInt(2, line.variantId);
                ps.setInt(3, line.quantity);
                ps.setBigDecimal(4, line.unitPrice);
                ps.addBatch();
            }
            ps.executeBatch();
        } finally {
            ps.close();
        }
    }

    private static void updateCheckoutTotal(Connection c, int checkoutId, BigDecimal total)
            throws SQLException {
        PreparedStatement ps = c.prepareStatement(
            "UPDATE checkout SET total_amount = ? WHERE checkout_id = ?");
        try {
            ps.setBigDecimal(1, total);
            ps.setInt(2, checkoutId);
            ps.executeUpdate();
        } finally {
            ps.close();
        }
    }

    private static void insertPayment(Connection c, int checkoutId, int payerId,
                                      String method, BigDecimal amount) throws SQLException {
        PreparedStatement ps = c.prepareStatement(
            "INSERT INTO payment (checkout_id, paid_by_user_id, payment_method, "
          + "paid_amount, reference) VALUES (?, ?, ?, ?, ?)");
        try {
            ps.setInt(1, checkoutId);
            ps.setInt(2, payerId);
            ps.setString(3, method);
            ps.setBigDecimal(4, amount);
            ps.setString(5, "APP-" + checkoutId + "-" + method);
            ps.executeUpdate();
        } finally {
            ps.close();
        }
    }

    /**
     * Takes the units out of the warehouse holding the most of that variant,
     * spilling over to the next warehouse when one cannot cover the line.
     */
    private static void deductStock(Connection c, int variantId, int quantity)
            throws SQLException {
        List<int[]> stock = new ArrayList<int[]>();

        PreparedStatement pick = c.prepareStatement(PICK_WAREHOUSE_SQL);
        try {
            pick.setInt(1, variantId);
            ResultSet rs = pick.executeQuery();
            try {
                while (rs.next()) {
                    stock.add(new int[] { rs.getInt("warehouse_id"), rs.getInt("quantity") });
                }
            } finally {
                rs.close();
            }
        } finally {
            pick.close();
        }

        int remaining = quantity;
        PreparedStatement upd = c.prepareStatement(
            "UPDATE inventory SET quantity = quantity - ? "
          + "WHERE variant_id = ? AND warehouse_id = ?");
        try {
            for (int i = 0; i < stock.size() && remaining > 0; i++) {
                int warehouseId = stock.get(i)[0];
                int take        = Math.min(remaining, stock.get(i)[1]);

                upd.setInt(1, take);
                upd.setInt(2, variantId);
                upd.setInt(3, warehouseId);
                upd.executeUpdate();

                remaining -= take;
            }
        } finally {
            upd.close();
        }

        if (remaining > 0) {
            throw new SQLException("Stock changed while checking out. Please try again.");
        }
    }

    private static void clearCart(Connection c, int buyerId) throws SQLException {
        PreparedStatement ps = c.prepareStatement("DELETE FROM cart_item WHERE buyer_id = ?");
        try {
            ps.setInt(1, buyerId);
            ps.executeUpdate();
        } finally {
            ps.close();
        }
    }

    private static int firstKey(PreparedStatement ps) throws SQLException {
        ResultSet keys = ps.getGeneratedKeys();
        try {
            if (!keys.next()) {
                throw new SQLException("Insert did not return a generated key.");
            }
            return keys.getInt(1);
        } finally {
            keys.close();
        }
    }
}
