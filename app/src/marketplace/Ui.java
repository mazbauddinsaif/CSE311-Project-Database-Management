package marketplace;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Builds the styled widgets every screen is assembled from. */
public final class Ui {

    private Ui() { }

    // ---------------------------------------------------------------- panels

    /** Panel with a rounded white surface and a hairline border. */
    public static class Card extends JPanel {
        private final int arc;

        public Card(int arc) {
            super(new BorderLayout());
            this.arc = arc;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Theme.SURFACE);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g.setColor(Theme.LINE);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g.dispose();
            super.paintComponent(g0);
        }
    }

    public static Card card() {
        return new Card(12);
    }

    /** Card with a title strip above its body. */
    public static Card card(String title, JComponent body) {
        Card c = new Card(12);
        if (title != null) {
            JPanel head = new JPanel(new BorderLayout());
            head.setOpaque(false);
            head.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
            head.add(sectionTitle(title), BorderLayout.WEST);
            c.add(head, BorderLayout.NORTH);
        }
        c.add(body, BorderLayout.CENTER);
        return c;
    }

    public static JPanel row(int gap) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
        p.setOpaque(false);
        return p;
    }

    public static JPanel transparent(java.awt.LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setOpaque(false);
        return p;
    }

    // ----------------------------------------------------------------- text

    public static JLabel h1(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.display(24));
        l.setForeground(Theme.INK);
        return l;
    }

    public static JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.display(15));
        l.setForeground(Theme.INK);
        return l;
    }

    public static JLabel sub(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.body(13));
        l.setForeground(Theme.MUTED);
        return l;
    }

    /** Uppercase, letter spaced caption used above figures. */
    public static JLabel eyebrow(String text) {
        JLabel l = new JLabel(spaced(text.toUpperCase()));
        l.setFont(Theme.label(10));
        l.setForeground(Theme.FAINT);
        return l;
    }

    private static String spaced(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            b.append(s.charAt(i));
            if (i < s.length() - 1) {
                b.append(' ');
            }
        }
        return b.toString();
    }

    // -------------------------------------------------------------- buttons

    public static JButton primary(String text) {
        JButton b = new JButton(text);
        b.setFont(Theme.bodyBold(13));
        b.setForeground(Color.WHITE);
        b.setBackground(Theme.DEEP);
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton accent(String text) {
        JButton b = primary(text);
        b.setBackground(Theme.MARIGOLD);
        b.setForeground(new Color(0x3A, 0x2A, 0x08));
        return b;
    }

    public static JButton ghost(String text) {
        JButton b = new JButton(text);
        b.setFont(Theme.body(13));
        b.setForeground(Theme.INK);
        b.setBackground(Theme.SURFACE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.LINE),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ---------------------------------------------------------------- input

    public static JTextField search(String placeholder, int columns) {
        JTextField f = new JTextField(columns);
        f.putClientProperty("JTextField.placeholderText", placeholder);
        f.putClientProperty("JTextField.leadingIcon",
                            new Glyph("search", 15, Theme.FAINT));
        f.setFont(Theme.body(13));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.LINE),
            BorderFactory.createEmptyBorder(7, 9, 7, 9)));
        return f;
    }

    // ----------------------------------------------------------- stat tiles

    /** Headline figure with a caption, used across the dashboard tops. */
    public static Card stat(String caption, String value, String detail, Color accent,
                            String glyph) {
        Card c = new Card(12);
        c.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JPanel top = transparent(new BorderLayout());
        top.add(eyebrow(caption), BorderLayout.WEST);
        if (glyph != null) {
            JLabel ic = new JLabel(new Glyph(glyph, 16, accent));
            top.add(ic, BorderLayout.EAST);
        }

        JLabel v = new JLabel(value);
        v.setFont(Theme.display(26));
        v.setForeground(Theme.INK);
        v.setBorder(BorderFactory.createEmptyBorder(8, 0, 2, 0));

        JLabel d = new JLabel(detail == null ? " " : detail);
        d.setFont(Theme.body(12));
        d.setForeground(accent);

        JPanel body = transparent(new BorderLayout());
        body.add(v, BorderLayout.NORTH);
        body.add(d, BorderLayout.CENTER);

        c.add(top, BorderLayout.NORTH);
        c.add(body, BorderLayout.CENTER);
        c.setPreferredSize(new Dimension(200, 108));
        return c;
    }

    public static JPanel statRow(Card... cards) {
        JPanel p = new JPanel(new GridLayout(1, cards.length, 14, 0));
        p.setOpaque(false);
        for (Card c : cards) {
            p.add(c);
        }
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 116));
        p.setPreferredSize(new Dimension(100, 116));
        return p;
    }

    // ---------------------------------------------------------------- table

    public static JTable table() {
        JTable t = new JTable();
        t.setRowHeight(34);
        t.setFont(Theme.body(13));
        t.setForeground(Theme.INK);
        t.setBackground(Theme.SURFACE);
        t.setSelectionBackground(Theme.SELECT);
        t.setSelectionForeground(Theme.INK);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(Theme.LINE);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        t.setFillsViewportHeight(true);

        JTableHeader h = t.getTableHeader();
        h.setFont(Theme.label(11));
        h.setForeground(Theme.MUTED);
        h.setBackground(Theme.SURFACE);
        h.setPreferredSize(new Dimension(10, 34));
        h.setReorderingAllowed(false);
        h.setDefaultRenderer(new HeaderCell(h.getDefaultRenderer()));
        return t;
    }

    /** Header cells align with the body cells rather than being centred. */
    public static class HeaderCell implements TableCellRenderer {
        private final TableCellRenderer base;

        HeaderCell(TableCellRenderer base) {
            this.base = base;
        }

        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                       boolean focus, int row, int col) {
            Component c = base.getTableCellRendererComponent(t, header(v), sel, focus, row, col);
            if (c instanceof JLabel) {
                JLabel l = (JLabel) c;
                boolean right = false;
                if (col >= 0 && col < t.getColumnCount()) {
                    TableCellRenderer r = t.getColumnModel().getColumn(col).getCellRenderer();
                    right = r instanceof Money
                         || (r instanceof Cell && ((Cell) r).numeric);
                }
                l.setHorizontalAlignment(right ? SwingConstants.RIGHT : SwingConstants.LEFT);
                l.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                l.setFont(Theme.label(11));
                l.setForeground(Theme.MUTED);
            }
            return c;
        }

        /** Turns snake_case column names into readable headings. */
        private static String header(Object v) {
            if (v == null) {
                return "";
            }
            String s = v.toString().replace('_', ' ').trim();
            if (s.isEmpty()) {
                return s;
            }
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }

    public static JScrollPane scroll(JComponent view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        sp.getViewport().setBackground(Theme.SURFACE);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        resizeWithViewport(sp, view);
        return sp;
    }

    /** Table inside a scroll pane with no outer border, for use within a card. */
    public static JScrollPane bare(JComponent view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Theme.SURFACE);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        resizeWithViewport(sp, view);
        return sp;
    }

    /**
     * Column widths depend on the viewport, which has no width until the window
     * has been laid out, so they are recalculated whenever the viewport changes
     * size. This is also what keeps the columns filling the card after a resize.
     */
    private static void resizeWithViewport(JScrollPane sp, JComponent view) {
        if (!(view instanceof JTable)) {
            return;
        }
        final JTable t = (JTable) view;
        sp.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (t.getColumnCount() > 0) {
                    sizeColumns(t);
                }
            }
        });
    }

    // ------------------------------------------------------------ renderers

    /** Zebra striping plus right alignment for numeric looking columns. */
    public static class Cell extends DefaultTableCellRenderer {
        final boolean numeric;
        private final boolean monospace;

        public Cell(boolean numeric, boolean monospace) {
            this.numeric   = numeric;
            this.monospace = monospace;
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                       boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, readable(v), sel, false, row, col);
            setHorizontalAlignment(numeric ? SwingConstants.RIGHT : SwingConstants.LEFT);
            setFont(monospace ? Theme.mono(13) : Theme.body(13));
            if (!sel) {
                setBackground(row % 2 == 0 ? Theme.SURFACE : Theme.ROW_ALT);
                setForeground(Theme.INK);
            }
            return this;
        }
    }

    /** Money column: right aligned, monospaced so the decimal points line up. */
    public static class Money extends DefaultTableCellRenderer {
        public Money() {
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                       boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, Theme.money(v), sel, false, row, col);
            setHorizontalAlignment(SwingConstants.RIGHT);
            setFont(Theme.mono(13));
            if (!sel) {
                setBackground(row % 2 == 0 ? Theme.SURFACE : Theme.ROW_ALT);
                setForeground(Theme.INK);
            }
            return this;
        }
    }

    /** Status column drawn as a coloured pill. */
    public static class Pill implements TableCellRenderer {
        private final JLabel label = new JLabel();

        public Component getTableCellRendererComponent(final JTable t, Object v, boolean sel,
                                                       boolean focus, final int row, int col) {
            final String text = v == null ? "" : v.toString().replace('_', ' ');
            final Color c = Theme.statusColour(v == null ? null : v.toString());
            final Color bg = sel ? Theme.SELECT
                                 : (row % 2 == 0 ? Theme.SURFACE : Theme.ROW_ALT);

            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)) {
                @Override
                protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                       RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(bg);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    if (!text.isEmpty()) {
                        Font f = Theme.bodyBold(11);
                        g.setFont(f);
                        int tw = g.getFontMetrics().stringWidth(text);
                        int ph = 20;
                        int pw = tw + 20;
                        int px = 12;
                        int py = (getHeight() - ph) / 2;
                        g.setColor(tint(c, 0.88f));
                        g.fillRoundRect(px, py, pw, ph, ph, ph);
                        g.setColor(c);
                        g.drawString(text, px + 10,
                                     py + ph - 6 - (g.getFontMetrics().getDescent() - 4));
                    }
                    g.dispose();
                }
            };
            p.setPreferredSize(new Dimension(120, t.getRowHeight()));
            return p;
        }
    }

    /**
     * Timestamps print as "2026-05-06 05:04". The driver hands these back as
     * either java.sql.Timestamp or java.time.LocalDateTime depending on the
     * column, so the text form is normalised rather than the type.
     */
    static Object readable(Object v) {
        if (v == null) {
            return null;
        }
        String s = v.toString();
        if (s.length() >= 16
                && s.charAt(4) == '-' && s.charAt(7) == '-'
                && (s.charAt(10) == 'T' || s.charAt(10) == ' ')
                && s.charAt(13) == ':') {
            return s.substring(0, 10) + " " + s.substring(11, 16);
        }
        return v;
    }

    static Color tint(Color c, float amount) {
        return new Color(
            (int) (c.getRed()   + (255 - c.getRed())   * amount),
            (int) (c.getGreen() + (255 - c.getGreen()) * amount),
            (int) (c.getBlue()  + (255 - c.getBlue())  * amount));
    }

    // -------------------------------------------------------------- loading

    /**
     * Runs a query into a table and applies the renderers by column name:
     * anything that reads like money is formatted, status columns become pills.
     */
    public static void fill(JTable table, String sql, Object... params) {
        try (Connection c = Db.open();
             PreparedStatement ps = c.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                table.setModel(TableUtil.toModel(rs));
            }
            style(table);

        } catch (SQLException e) {
            TableUtil.error(table, e);
        }
    }

    public static void style(JTable table) {
        for (int i = 0; i < table.getColumnCount(); i++) {
            String n = table.getColumnName(i).toLowerCase();
            if (n.contains("status") || n.equals("delivery") || n.equals("type")) {
                table.getColumnModel().getColumn(i).setCellRenderer(new Pill());
            } else if (n.contains("amount") || n.contains("price") || n.contains("total")
                    || n.contains("revenue") || n.contains("earning")
                    || n.contains("commission") || n.contains("spend")) {
                table.getColumnModel().getColumn(i).setCellRenderer(new Money());
            } else if (n.contains("id") || n.contains("qty") || n.contains("quantity")
                    || n.contains("count") || n.contains("stock") || n.contains("units")
                    || n.contains("stars") || n.contains("points") || n.contains("age")
                    || n.contains("threshold") || n.contains("lines")
                    || n.contains("reviews") || n.contains("orders")) {
                table.getColumnModel().getColumn(i).setCellRenderer(new Cell(true, true));
            } else {
                table.getColumnModel().getColumn(i).setCellRenderer(new Cell(false, false));
            }
        }
        sizeColumns(table);
    }

    public static void sizeColumns(final JTable table) {
        int total = 0;
        int[] want = new int[table.getColumnCount()];

        for (int col = 0; col < table.getColumnCount(); col++) {
            int width = 70;
            TableCellRenderer hr = table.getTableHeader().getDefaultRenderer();
            Component hc = hr.getTableCellRendererComponent(
                table, table.getColumnName(col), false, false, -1, col);
            width = Math.max(width, hc.getPreferredSize().width + 28);

            int rows = Math.min(table.getRowCount(), 60);
            for (int row = 0; row < rows; row++) {
                Component comp = table.prepareRenderer(table.getCellRenderer(row, col), row, col);
                width = Math.max(width, comp.getPreferredSize().width + 26);
            }
            want[col] = Math.min(width, 340);
            total += want[col];
        }

        // If the columns do not fill the viewport, share the slack out so the
        // table does not leave a band of empty background on the right.
        int available = table.getParent() instanceof javax.swing.JViewport
                      ? table.getParent().getWidth() : 0;
        int slack = available - total;
        for (int col = 0; col < want.length; col++) {
            int w = want[col];
            if (slack > 0 && want.length > 0) {
                w += slack / want.length;
            }
            table.getColumnModel().getColumn(col).setPreferredWidth(w);
        }
        table.setAutoResizeMode(slack > 0 ? JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS
                                          : JTable.AUTO_RESIZE_OFF);
    }

    public static Border pad(int t, int l, int b, int r) {
        return BorderFactory.createEmptyBorder(t, l, b, r);
    }

    /**
     * Adds a little horizontal slack to a component's preferred width. Text
     * measurement rounds down slightly under display scaling, which clips the
     * last character or two of labels laid out at their preferred size.
     */
    public static <T extends JComponent> T loose(T c) {
        Dimension d = c.getPreferredSize();
        c.setPreferredSize(new Dimension(d.width + 14, d.height));
        return c;
    }
}
