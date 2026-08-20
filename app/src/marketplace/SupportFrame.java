package marketplace;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Support desk: claim tickets, record a resolution, watch the queue. */
public class SupportFrame extends Shell {

    private final int    agentId;
    private final String agentName;

    private final JTable    ticketTable   = Ui.table();
    private final JTable    workloadTable = Ui.table();
    private final JTextArea resolution    = new JTextArea(5, 40);
    private final JCheckBox unassignedOnly = new JCheckBox("Unassigned only");

    private final JComboBox<String> statusBox = new JComboBox<String>(
        new String[] { "OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED" });

    private Ui.Card tOpen, tMine, tResolved, tResponse;

    public SupportFrame(int agentId, String agentName) {
        super("Marketplace", agentName, "Support");
        this.agentId   = agentId;
        this.agentName = agentName;

        page("queue", "Ticket Queue", "Ticket queue",
             "Unassigned tickets and the ones you are handling.", "ticket", queuePage());
        page("team", "Team Workload", "Team workload",
             "Tickets handled by each support agent.", "chart", workloadPage());

        showPage("queue");
    }

    @Override
    protected void onPageShown(String key) {
        if ("queue".equals(key)) {
            refreshTickets();
            refreshStats();
        } else {
            refreshWorkload();
        }
    }

    private JPanel queuePage() {
        tOpen     = Ui.stat("Unassigned", "0", "waiting to be claimed", Theme.WARN, "ticket");
        tMine     = Ui.stat("Assigned to me", "0", "currently in progress", Theme.INFO, "user");
        tResolved = Ui.stat("Resolved by me", "0", "closed cases", Theme.OK, "chart");
        tResponse = Ui.stat("My response time", "0", "minutes on average",
                            Theme.MARIGOLD_DK, "alert");

        JButton claim = Ui.primary("Assign to me");
        JButton save  = Ui.accent("Save status");
        claim.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { claimTicket(); }
        });
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { saveTicket(); }
        });
        unassignedOnly.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { refreshTickets(); }
        });
        unassignedOnly.setOpaque(false);
        unassignedOnly.setFont(Theme.body(13));
        statusBox.setPreferredSize(new Dimension(150, 34));

        JPanel bar = Ui.row(10);
        bar.add(Ui.loose(unassignedOnly));
        bar.add(claim);
        bar.add(Ui.eyebrow("Set status"));
        bar.add(statusBox);
        bar.add(save);

        resolution.setLineWrap(true);
        resolution.setWrapStyleWord(true);
        resolution.setFont(Theme.body(13));
        resolution.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.LINE),
            BorderFactory.createEmptyBorder(9, 11, 9, 11)));

        Ui.Card list = Ui.card();
        list.add(bar, BorderLayout.NORTH);
        JPanel body = Ui.transparent(new BorderLayout());
        body.setBorder(Ui.pad(14, 0, 0, 0));
        body.add(Ui.scroll(ticketTable), BorderLayout.CENTER);
        list.add(body, BorderLayout.CENTER);

        Ui.Card note = Ui.card("Resolution note", resolution);
        note.setPreferredSize(new Dimension(10, 150));

        JPanel lower = Ui.transparent(new BorderLayout(0, 16));
        lower.add(list, BorderLayout.CENTER);
        lower.add(note, BorderLayout.SOUTH);

        JPanel p = Ui.transparent(new BorderLayout(0, 16));
        p.add(Ui.statRow(tOpen, tMine, tResolved, tResponse), BorderLayout.NORTH);
        p.add(lower, BorderLayout.CENTER);
        return p;
    }

    private void refreshTickets() {
        String sql =
            "SELECT t.ticket_id, CONCAT(u.first_name,' ',u.last_name) AS buyer, "
          + "       t.order_id, t.status, t.description, t.resolution_text AS resolution, "
          + "       t.created_at AS raised "
          + "FROM   ticket t "
          + "JOIN   `user` u ON u.user_id = t.buyer_id ";

        if (unassignedOnly.isSelected()) {
            Ui.fill(ticketTable, sql + "WHERE t.support_id IS NULL ORDER BY t.created_at");
        } else {
            Ui.fill(ticketTable,
                sql + "WHERE t.support_id IS NULL OR t.support_id = ? ORDER BY t.created_at",
                Integer.valueOf(agentId));
        }
    }

    private void refreshStats() {
        try (Connection c = Db.open()) {
            setStat(tOpen, count(c, "SELECT COUNT(*) FROM ticket WHERE support_id IS NULL", -1));
            setStat(tMine, count(c,
                "SELECT COUNT(*) FROM ticket WHERE support_id = ? "
              + "AND status IN ('OPEN','IN_PROGRESS')", agentId));
            setStat(tResolved, count(c,
                "SELECT COUNT(*) FROM ticket WHERE support_id = ? "
              + "AND status IN ('RESOLVED','CLOSED')", agentId));
            setStat(tResponse, count(c,
                "SELECT response_time_min FROM support WHERE user_id = ?", agentId));
        } catch (SQLException e) {
            TableUtil.error(this, e);
        }
    }

    private int selectedTicket() {
        int row = ticketTable.getSelectedRow();
        if (row < 0) {
            TableUtil.info(this, "Select a ticket first.");
            return -1;
        }
        return TableUtil.intAt(ticketTable, row, 0);
    }

    private void claimTicket() {
        int id = selectedTicket();
        if (id < 0) {
            return;
        }
        try (Connection c = Db.open();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE ticket SET support_id = ?, status = 'IN_PROGRESS' "
               + "WHERE ticket_id = ? AND support_id IS NULL")) {
            ps.setInt(1, agentId);
            ps.setInt(2, id);
            if (ps.executeUpdate() == 0) {
                TableUtil.info(this, "That ticket is already assigned to another agent.");
            } else {
                refreshTickets();
                refreshStats();
            }
        } catch (SQLException e) {
            TableUtil.error(this, e);
        }
    }

    private void saveTicket() {
        int id = selectedTicket();
        if (id < 0) {
            return;
        }
        String text = resolution.getText().trim();
        try (Connection c = Db.open();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE ticket SET status = ?, resolution_text = ?, support_id = ? "
               + "WHERE ticket_id = ?")) {
            ps.setString(1, (String) statusBox.getSelectedItem());
            if (text.isEmpty()) {
                ps.setNull(2, java.sql.Types.VARCHAR);
            } else {
                ps.setString(2, text);
            }
            ps.setInt(3, agentId);
            ps.setInt(4, id);
            ps.executeUpdate();
            resolution.setText("");
            refreshTickets();
            refreshStats();
        } catch (SQLException e) {
            TableUtil.error(this, e);
        }
    }

    private JPanel workloadPage() {
        return Ui.card("Tickets handled per agent", Ui.bare(workloadTable));
    }

    private void refreshWorkload() {
        Ui.fill(workloadTable,
            "SELECT CONCAT(u.first_name,' ',u.last_name) AS agent, "
          + "       sp.response_time_min AS response_minutes, "
          + "       COUNT(t.ticket_id) AS tickets_handled "
          + "FROM        support sp "
          + "JOIN        `user`  u ON u.user_id    = sp.user_id "
          + "LEFT JOIN   ticket  t ON t.support_id = sp.user_id "
          + "GROUP BY u.first_name, u.last_name, sp.response_time_min "
          + "ORDER BY tickets_handled DESC");
    }

    private static String count(Connection c, String sql, int id) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        try {
            if (id >= 0) {
                ps.setInt(1, id);
            }
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

    private static void setStat(Ui.Card card, String value) {
        JPanel body = (JPanel) ((BorderLayout) card.getLayout())
                        .getLayoutComponent(BorderLayout.CENTER);
        JLabel v = (JLabel) ((BorderLayout) body.getLayout())
                        .getLayoutComponent(BorderLayout.NORTH);
        v.setText(value);
    }
}
