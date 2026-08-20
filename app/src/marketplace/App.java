package marketplace;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Sign in screen. After the credentials match a row in `user`, the three
 * subclass tables of the ISA hierarchy are probed to decide which dashboard
 * to open.
 */
public class App extends JFrame {

    private final JTextField     emailField    = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JLabel         status        = new JLabel(" ");

    public App() {
        super("Marketplace");
        Theme.install();

        setLayout(new BorderLayout());
        add(brandPanel(), BorderLayout.WEST);
        add(formPanel(), BorderLayout.CENTER);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(940, 580);
        setResizable(false);
        setLocationRelativeTo(null);
    }

    /** Left half: deep green, states what the project is about. */
    private JPanel brandPanel() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                   RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Theme.DEEP);
                g.fillRect(0, 0, getWidth(), getHeight());

                // one basket branching into three vendor orders, drawn large
                int cx = 62, cy = getHeight() - 132;
                g.setColor(Theme.DEEP_HI);
                g.setStroke(new java.awt.BasicStroke(2f));
                for (int i = 0; i < 3; i++) {
                    int ty = cy - 46 + i * 46;
                    java.awt.geom.GeneralPath path = new java.awt.geom.GeneralPath();
                    path.moveTo(cx, cy);
                    path.lineTo(cx + 26, cy);
                    path.curveTo(cx + 58, cy, cx + 30, ty, cx + 78, ty);
                    g.draw(path);
                    g.setColor(i == 1 ? Theme.MARIGOLD : Theme.DEEP_HI);
                    g.fillOval(cx + 74, ty - 5, 10, 10);
                    g.setColor(Theme.DEEP_HI);
                }
                g.setColor(Theme.MARIGOLD);
                g.fillOval(cx - 7, cy - 7, 14, 14);
                g.dispose();
            }
        };
        p.setPreferredSize(new Dimension(400, 10));
        p.setLayout(null);

        JLabel mark = new JLabel("M");
        mark.setFont(Theme.display(20));
        mark.setForeground(Theme.DEEP);
        mark.setOpaque(false);
        mark.setHorizontalAlignment(JLabel.CENTER);
        mark.setBounds(46, 46, 38, 38);
        JPanel markBg = new JPanel(null) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                   RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Theme.MARIGOLD);
                g.fillRoundRect(0, 0, 38, 38, 11, 11);
                g.dispose();
            }
        };
        markBg.setOpaque(false);
        markBg.setBounds(46, 46, 38, 38);
        markBg.add(mark);
        mark.setBounds(0, 0, 38, 38);
        p.add(markBg);

        JLabel h = new JLabel("<html>One basket.<br>Many vendors.</html>");
        h.setFont(Theme.display(34));
        h.setForeground(Color.WHITE);
        h.setBounds(46, 122, 320, 100);
        p.add(h);

        // Explicit line breaks rather than automatic wrapping, which measures a
        // little short and drops the last word of a line.
        JLabel s = new JLabel("<html>A customer pays once.<br>"
            + "The platform splits that basket into<br>"
            + "a separate order for every seller.</html>");
        s.setFont(Theme.body(13));
        s.setForeground(Theme.DEEP_DIM);
        s.setBounds(46, 230, 330, 76);
        p.add(s);

        JLabel c = new JLabel("CSE 311  ·  Database Management System");
        c.setFont(Theme.label(10));
        c.setForeground(Theme.DEEP_DIM);
        c.setBounds(46, getHeight() - 40, 320, 20);
        c.setBounds(46, 500, 320, 20);
        p.add(c);
        return p;
    }

    /** Right half: the form. */
    private JPanel formPanel() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Theme.SURFACE);
        wrap.setBorder(BorderFactory.createEmptyBorder(0, 54, 0, 54));

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new javax.swing.BoxLayout(form, javax.swing.BoxLayout.Y_AXIS));

        JLabel t = new JLabel("Sign in");
        t.setFont(Theme.display(26));
        t.setForeground(Theme.INK);
        t.setAlignmentX(0f);

        JLabel sub = new JLabel("Use your marketplace account.");
        sub.setFont(Theme.body(13));
        sub.setForeground(Theme.MUTED);
        sub.setAlignmentX(0f);
        sub.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        sub.setBorder(BorderFactory.createEmptyBorder(4, 0, 26, 0));

        emailField.putClientProperty("JTextField.placeholderText", "you@example.com");
        passwordField.putClientProperty("JTextField.placeholderText", "Your password");
        for (javax.swing.JComponent f : new javax.swing.JComponent[] {emailField, passwordField}) {
            f.setFont(Theme.body(14));
            f.setAlignmentX(0f);
            f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.LINE),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        }

        JButton login = Ui.primary("Sign in");
        login.setAlignmentX(0f);
        login.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        login.setFont(Theme.bodyBold(14));

        JButton console = Ui.ghost("Open reporting console");
        console.setAlignmentX(0f);
        console.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        status.setFont(Theme.body(12));
        status.setForeground(Theme.DANGER);
        status.setAlignmentX(0f);
        status.setBorder(BorderFactory.createEmptyBorder(10, 0, 4, 0));

        form.add(t);
        form.add(sub);
        form.add(label("Email"));
        form.add(emailField);
        form.add(gap(14));
        form.add(label("Password"));
        form.add(passwordField);
        form.add(status);
        form.add(gap(6));
        form.add(login);
        form.add(gap(10));
        form.add(console);
        form.add(gap(24));
        form.add(demoAccounts());

        ActionListener submit = new ActionListener() {
            public void actionPerformed(ActionEvent e) { attemptLogin(); }
        };
        login.addActionListener(submit);
        passwordField.addActionListener(submit);
        emailField.addActionListener(submit);
        console.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new ReportsFrame().setVisible(true);
            }
        });
        getRootPane().setDefaultButton(login);

        JPanel centre = new JPanel(new java.awt.GridBagLayout());
        centre.setOpaque(false);
        centre.add(form, new java.awt.GridBagConstraints());
        wrap.add(centre, BorderLayout.CENTER);
        return wrap;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.bodyBold(12));
        l.setForeground(Theme.MUTED);
        l.setAlignmentX(0f);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        return l;
    }

    private JPanel gap(int h) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(1, h));
        p.setPreferredSize(new Dimension(1, h));
        p.setAlignmentX(0f);
        return p;
    }

    /** Clickable sample accounts so the demonstration never stalls on typing. */
    private JPanel demoAccounts() {
        JPanel p = new JPanel(new GridLayout(3, 1, 0, 6));
        p.setOpaque(false);
        p.setAlignmentX(0f);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        String[][] demo = {
            {"Buyer",   "nusrat.jahan@mail.com"},
            {"Vendor",  "karim.hossain@shop.bd"},
            {"Support", "zareef.mirza@help.bd"},
        };
        for (final String[] d : demo) {
            JPanel rowP = new JPanel(new BorderLayout());
            rowP.setOpaque(false);
            rowP.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel role = new JLabel(d[0]);
            role.setFont(Theme.bodyBold(11));
            role.setForeground(Theme.DEEP);
            role.setPreferredSize(new Dimension(62, 18));

            JLabel mail = new JLabel(d[1]);
            mail.setFont(Theme.mono(11));
            mail.setForeground(Theme.MUTED);

            rowP.add(role, BorderLayout.WEST);
            rowP.add(mail, BorderLayout.CENTER);
            rowP.addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    emailField.setText(d[1]);
                    passwordField.setText("pass123");
                    status.setText(" ");
                    passwordField.requestFocusInWindow();
                }
            });
            p.add(rowP);
        }

        JPanel box = new JPanel(new BorderLayout(0, 8));
        box.setOpaque(false);
        box.setAlignmentX(0f);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        JLabel cap = Ui.eyebrow("Sample accounts  ·  password pass123");
        box.add(cap, BorderLayout.NORTH);
        box.add(p, BorderLayout.CENTER);
        return box;
    }

    private void attemptLogin() {
        String email    = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            status.setText("Enter an email and a password.");
            return;
        }

        try (Connection c = Db.open()) {
            int    userId = -1;
            String name   = null;

            PreparedStatement ps = c.prepareStatement(
                "SELECT user_id, first_name, last_name FROM `user` "
              + "WHERE email = ? AND password = ?");
            try {
                ps.setString(1, email);
                ps.setString(2, password);
                ResultSet rs = ps.executeQuery();
                try {
                    if (rs.next()) {
                        userId = rs.getInt("user_id");
                        name   = rs.getString("first_name") + " " + rs.getString("last_name");
                    }
                } finally {
                    rs.close();
                }
            } finally {
                ps.close();
            }

            if (userId == -1) {
                status.setText("No account matches that email and password.");
                return;
            }

            JFrame dashboard;
            if (existsIn(c, "vendor", userId)) {
                dashboard = new VendorFrame(userId, name);
            } else if (existsIn(c, "buyer", userId)) {
                dashboard = new BuyerFrame(userId, name);
            } else if (existsIn(c, "support", userId)) {
                dashboard = new SupportFrame(userId, name);
            } else {
                status.setText("This account has no role assigned.");
                return;
            }

            dashboard.setVisible(true);
            dispose();

        } catch (SQLException e) {
            status.setText("Cannot reach the database. Is XAMPP MySQL running?");
        }
    }

    /** Probes one subclass table of the ISA hierarchy. */
    private boolean existsIn(Connection c, String table, int userId) throws SQLException {
        PreparedStatement ps = c.prepareStatement(
            "SELECT 1 FROM " + table + " WHERE user_id = ?");
        try {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            try {
                return rs.next();
            } finally {
                rs.close();
            }
        } finally {
            ps.close();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Theme.install();
                new App().setVisible(true);
            }
        });
    }
}
