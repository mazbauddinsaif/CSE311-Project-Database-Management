package marketplace;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Window frame shared by the three dashboards: a green navigation rail on the
 * left, a title bar across the top and a card stack for the pages.
 */
public abstract class Shell extends JFrame {

    private final JPanel     rail    = new JPanel();
    private final JPanel     pages   = new JPanel(new CardLayout());
    private final JLabel     title   = Ui.h1("");
    private final JLabel     caption = Ui.sub("");
    private final JPanel     actions = Ui.row(8);
    private final List<Item> items   = new ArrayList<Item>();

    private final String userName;
    private final String roleName;

    /** One entry in the navigation rail. */
    private final class Item extends JPanel {
        final String  key;
        final String  label;
        final String  heading;
        final String  sub;
        final Glyph   icon;
        boolean       active;
        boolean       hover;

        Item(String key, String label, String heading, String sub, String glyph) {
            super(new BorderLayout());
            this.key     = key;
            this.label   = label;
            this.heading = heading;
            this.sub     = sub;
            this.icon    = new Glyph(glyph, 17, Theme.DEEP_DIM);

            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

            JLabel text = new JLabel(label);
            text.setFont(Theme.body(14));
            text.setForeground(Theme.DEEP_DIM);
            text.setIcon(icon);
            text.setIconTextGap(12);
            add(text, BorderLayout.CENTER);
            putClientProperty("text", text);

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                @Override public void mouseExited (MouseEvent e) { hover = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { showPage(key); }
            });
        }

        void setActive(boolean on) {
            active = on;
            JLabel text = (JLabel) getClientProperty("text");
            text.setForeground(on ? Color.WHITE : Theme.DEEP_DIM);
            text.setFont(on ? Theme.bodyBold(14) : Theme.body(14));
            icon.setColour(on ? Theme.MARIGOLD : Theme.DEEP_DIM);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);
            if (active) {
                g.setColor(Theme.DEEP_HI);
                g.fillRoundRect(8, 2, getWidth() - 16, getHeight() - 4, 8, 8);
                g.setColor(Theme.MARIGOLD);
                g.fillRoundRect(0, 8, 4, getHeight() - 16, 4, 4);
            } else if (hover) {
                g.setColor(new Color(255, 255, 255, 22));
                g.fillRoundRect(8, 2, getWidth() - 16, getHeight() - 4, 8, 8);
            }
            g.dispose();
            super.paintComponent(g0);
        }
    }

    protected Shell(String windowTitle, String userName, String roleName) {
        super(windowTitle);
        this.userName = userName;
        this.roleName = roleName;

        Theme.install();
        buildRail();

        JPanel head = Ui.transparent(new BorderLayout());
        head.setBorder(BorderFactory.createEmptyBorder(26, 30, 14, 30));
        JPanel titles = Ui.transparent(new BorderLayout(0, 3));
        titles.add(title, BorderLayout.NORTH);
        titles.add(caption, BorderLayout.CENTER);
        // CENTER rather than WEST so a long subtitle is never squeezed to its
        // preferred width and clipped.
        head.add(titles, BorderLayout.CENTER);
        head.add(actions, BorderLayout.EAST);

        pages.setOpaque(false);
        pages.setBorder(BorderFactory.createEmptyBorder(0, 30, 26, 30));

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Theme.PAPER);
        main.add(head, BorderLayout.NORTH);
        main.add(pages, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(rail, BorderLayout.WEST);
        add(main, BorderLayout.CENTER);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 780);
        setMinimumSize(new Dimension(1080, 640));
        setLocationRelativeTo(null);
    }

    private void buildRail() {
        rail.setLayout(new BorderLayout());
        rail.setBackground(Theme.DEEP);
        rail.setPreferredSize(new Dimension(232, 10));

        // brand mark
        JPanel brand = new JPanel(new BorderLayout(10, 0));
        brand.setOpaque(false);
        brand.setBorder(BorderFactory.createEmptyBorder(24, 20, 22, 16));
        JLabel mark = new JLabel() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                   RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Theme.MARIGOLD);
                g.fillRoundRect(0, 0, 30, 30, 9, 9);
                g.setColor(Theme.DEEP);
                g.setFont(Theme.display(17));
                g.drawString("M", 9, 21);
                g.dispose();
            }
        };
        mark.setPreferredSize(new Dimension(30, 30));

        JPanel words = new JPanel(new java.awt.GridLayout(2, 1));
        words.setOpaque(false);
        JLabel n1 = new JLabel("Marketplace");
        n1.setFont(Theme.display(16));
        n1.setForeground(Color.WHITE);
        JLabel n2 = new JLabel("Multi-vendor platform");
        n2.setFont(Theme.body(11));
        n2.setForeground(Theme.DEEP_DIM);
        words.add(n1);
        words.add(n2);

        brand.add(mark, BorderLayout.WEST);
        brand.add(words, BorderLayout.CENTER);

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new javax.swing.BoxLayout(nav, javax.swing.BoxLayout.Y_AXIS));
        nav.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        rail.putClientProperty("nav", nav);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(brand, BorderLayout.NORTH);
        top.add(nav, BorderLayout.CENTER);

        rail.add(top, BorderLayout.NORTH);
        rail.add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildFooter() {
        JPanel foot = new JPanel(new BorderLayout(10, 0));
        foot.setOpaque(false);
        foot.setBorder(BorderFactory.createEmptyBorder(14, 18, 20, 16));

        JPanel avatar = new JPanel() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                   RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Theme.DEEP_HI);
                g.fillOval(0, 0, 32, 32);
                g.setColor(Color.WHITE);
                g.setFont(Theme.bodyBold(13));
                String in = initials(userName);
                int w = g.getFontMetrics().stringWidth(in);
                g.drawString(in, (32 - w) / 2, 21);
                g.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(32, 32));

        JPanel who = new JPanel(new java.awt.GridLayout(2, 1));
        who.setOpaque(false);
        JLabel a = new JLabel(userName);
        a.setFont(Theme.bodyBold(13));
        a.setForeground(Color.WHITE);
        JLabel b = new JLabel(roleName);
        b.setFont(Theme.body(11));
        b.setForeground(Theme.DEEP_DIM);
        who.add(a);
        who.add(b);

        JLabel out = new JLabel(new Glyph("logout", 16, Theme.DEEP_DIM));
        out.setToolTipText("Sign out");
        out.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        out.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { signOut(); }
        });

        JPanel divider = new JPanel();
        divider.setBackground(Theme.DEEP_HI);
        divider.setPreferredSize(new Dimension(10, 1));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        foot.add(avatar, BorderLayout.WEST);
        foot.add(who, BorderLayout.CENTER);
        foot.add(out, BorderLayout.EAST);
        wrap.add(divider, BorderLayout.NORTH);
        wrap.add(foot, BorderLayout.CENTER);
        return wrap;
    }

    private static String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    // ------------------------------------------------------------------ api

    /** Registers a page and its rail entry. */
    protected void page(String key, String navLabel, String heading, String subtitle,
                        String glyph, JComponent content) {
        Item it = new Item(key, navLabel, heading, subtitle, glyph);
        items.add(it);
        JPanel nav = (JPanel) rail.getClientProperty("nav");
        nav.add(it);
        pages.add(content, key);
    }

    protected void showPage(String key) {
        for (Item it : items) {
            boolean on = it.key.equals(key);
            it.setActive(on);
            if (on) {
                title.setText(it.heading);
                caption.setText(it.sub);
            }
        }
        ((CardLayout) pages.getLayout()).show(pages, key);
        onPageShown(key);
    }

    /** Buttons shown on the right of the title bar. */
    protected JPanel headerActions() {
        return actions;
    }

    protected void onPageShown(String key) {
        // subclasses refresh their data here
    }

    protected void signOut() {
        dispose();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new App().setVisible(true);
            }
        });
    }

    protected static GridBagConstraints gbc(int x, int y, double wx, double wy, int fill) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.weightx = wx;
        c.weighty = wy;
        c.fill = fill;
        c.insets = new java.awt.Insets(0, 0, 14, 0);
        return c;
    }

    protected static JPanel stack(JComponent top, JComponent centre, int gap) {
        JPanel p = new JPanel(new BorderLayout(0, gap));
        p.setOpaque(false);
        if (top != null) {
            p.add(top, BorderLayout.NORTH);
        }
        p.add(centre, BorderLayout.CENTER);
        return p;
    }

    static { /* ensure look and feel is ready even if a frame is built first */ }
}
