package marketplace;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws one checkout fanning out into its vendor orders.
 *
 * The whole design rests on the idea that a customer pays once but each vendor
 * fulfils separately, and that relationship is hard to see in a table. This
 * component makes it literal: a single payment node on the left, one branch per
 * vendor order on the right, each with its store, status and share of the total.
 */
public class SplitRibbon extends JComponent {

    private static final class Branch {
        int        orderId;
        String     store;
        String     status;
        BigDecimal amount;
        int        units;
    }

    private int          checkoutId = -1;
    private String       buyer      = "";
    private String       when       = "";
    private BigDecimal   total      = BigDecimal.ZERO;
    private String       method     = "";
    private final List<Branch> branches = new ArrayList<Branch>();
    private String       emptyText  = "Select an order to see how its basket was split.";

    public SplitRibbon() {
        setPreferredSize(new Dimension(560, 210));
    }

    public void clear(String message) {
        checkoutId = -1;
        branches.clear();
        emptyText = message;
        repaint();
    }

    /** Loads one checkout and every vendor order beneath it. */
    public void load(int checkout) {
        branches.clear();
        checkoutId = checkout;

        String head =
            "SELECT ck.checkout_id, ck.checkout_date, ck.total_amount, "
          + "       CONCAT(u.first_name,' ',u.last_name) AS buyer, "
          + "       IFNULL(p.payment_method,'UNPAID')    AS method "
          + "FROM        checkout ck "
          + "JOIN        `user`   u ON u.user_id     = ck.buyer_id "
          + "LEFT JOIN   payment  p ON p.checkout_id = ck.checkout_id "
          + "WHERE  ck.checkout_id = ?";

        String legs =
            "SELECT o.order_id, s.name AS store, o.status, o.total_amount, "
          + "       IFNULL(SUM(oi.quantity),0) AS units "
          + "FROM        customer_order o "
          + "JOIN        store          s  ON s.store_id  = o.store_id "
          + "LEFT JOIN   order_item     oi ON oi.order_id = o.order_id "
          + "WHERE  o.checkout_id = ? "
          + "GROUP BY o.order_id, s.name, o.status, o.total_amount "
          + "ORDER BY o.order_id";

        try (Connection c = Db.open()) {
            PreparedStatement ps = c.prepareStatement(head);
            try {
                ps.setInt(1, checkout);
                ResultSet rs = ps.executeQuery();
                try {
                    if (rs.next()) {
                        buyer  = rs.getString("buyer");
                        total  = rs.getBigDecimal("total_amount");
                        method = rs.getString("method");
                        String d = String.valueOf(rs.getTimestamp("checkout_date"));
                        when = d.length() >= 16 ? d.substring(0, 16) : d;
                    }
                } finally {
                    rs.close();
                }
            } finally {
                ps.close();
            }

            ps = c.prepareStatement(legs);
            try {
                ps.setInt(1, checkout);
                ResultSet rs = ps.executeQuery();
                try {
                    while (rs.next()) {
                        Branch b = new Branch();
                        b.orderId = rs.getInt("order_id");
                        b.store   = rs.getString("store");
                        b.status  = rs.getString("status");
                        b.amount  = rs.getBigDecimal("total_amount");
                        b.units   = rs.getInt("units");
                        branches.add(b);
                    }
                } finally {
                    rs.close();
                }
            } finally {
                ps.close();
            }
        } catch (SQLException e) {
            TableUtil.error(this, e);
        }

        setPreferredSize(new Dimension(560, Math.max(190, 74 + branches.size() * 56)));
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        if (checkoutId < 0 || branches.isEmpty()) {
            g.setColor(Theme.FAINT);
            g.setFont(Theme.body(13));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(emptyText, Math.max(12, (w - fm.stringWidth(emptyText)) / 2), h / 2);
            g.dispose();
            return;
        }

        int leftW  = 152;
        int leftX  = 2;
        int gapX   = 52;
        int rightX = leftX + leftW + gapX;
        int rightW = Math.max(180, w - rightX - 6);
        int rowH   = 46;
        int rowGap = 10;
        int topY   = 8;
        int blockH = branches.size() * rowH + (branches.size() - 1) * rowGap;
        int leftH  = 92;
        int leftY  = topY + Math.max(0, (blockH - leftH) / 2);

        // ---- the single payment on the left
        g.setColor(Theme.DEEP);
        g.fillRoundRect(leftX, leftY, leftW, leftH, 12, 12);

        g.setColor(Theme.DEEP_DIM);
        g.setFont(Theme.label(10));
        g.drawString("O N E   C H E C K O U T", leftX + 14, leftY + 22);

        g.setColor(Color.WHITE);
        g.setFont(Theme.display(21));
        g.drawString("#" + checkoutId, leftX + 14, leftY + 48);

        g.setFont(Theme.mono(12));
        g.setColor(Theme.MARIGOLD);
        g.drawString(Theme.money(total), leftX + 14, leftY + 68);

        g.setFont(Theme.body(11));
        g.setColor(Theme.DEEP_DIM);
        String foot = method + "  ·  " + when;
        g.drawString(clip(g, foot, leftW - 26), leftX + 14, leftY + 84);

        // ---- one branch per vendor order
        int cx = leftX + leftW;
        int cy = leftY + leftH / 2;

        for (int i = 0; i < branches.size(); i++) {
            Branch b = branches.get(i);
            int y = topY + i * (rowH + rowGap);
            int my = y + rowH / 2;
            Color accent = Theme.statusColour(b.status);

            // connector: horizontal out, curve, horizontal in
            GeneralPath p = new GeneralPath();
            p.moveTo(cx, cy);
            p.lineTo(cx + gapX * 0.34f, cy);
            p.curveTo(cx + gapX * 0.72f, cy, cx + gapX * 0.30f, my, cx + gapX, my);
            g.setColor(Ui.tint(accent, 0.55f));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(p);

            // node
            g.setColor(Theme.SURFACE);
            g.fillRoundRect(rightX, y, rightW, rowH, 10, 10);
            g.setColor(Theme.LINE);
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(rightX, y, rightW, rowH, 10, 10);

            g.setColor(accent);
            g.fillRoundRect(rightX, y + 9, 3, rowH - 18, 3, 3);

            // Measure the amount first so the store name is clipped to whatever
            // space is genuinely left, instead of a guessed reserve.
            String amt = Theme.money(b.amount);
            g.setFont(Theme.mono(12));
            int aw = g.getFontMetrics().stringWidth(amt);

            g.setColor(Theme.INK);
            g.drawString(amt, rightX + rightW - aw - 14, y + 20);

            g.setFont(Theme.bodyBold(13));
            g.drawString(clip(g, b.store, rightW - aw - 40), rightX + 14, y + 20);

            g.setColor(Theme.MUTED);
            g.setFont(Theme.body(11));
            g.drawString("Order #" + b.orderId + "  ·  " + b.units
                         + (b.units == 1 ? " item" : " items"), rightX + 14, y + 36);

            String st = b.status.replace('_', ' ');
            g.setFont(Theme.bodyBold(10));
            int sw = g.getFontMetrics().stringWidth(st);
            int pillW = sw + 16, pillH = 16;
            int px = rightX + rightW - pillW - 14;
            g.setColor(Ui.tint(accent, 0.86f));
            g.fillRoundRect(px, y + 26, pillW, pillH, pillH, pillH);
            g.setColor(accent);
            g.drawString(st, px + 8, y + 38);
        }

        g.dispose();
    }

    private static String clip(Graphics2D g, String s, int max) {
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(s) <= max) {
            return s;
        }
        String out = s;
        while (out.length() > 3 && fm.stringWidth(out + "...") > max) {
            out = out.substring(0, out.length() - 1);
        }
        return out + "...";
    }
}
