package marketplace;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/** Helpers for pushing a ResultSet into a Swing table. */
public final class TableUtil {

    private TableUtil() { }

    /** Converts any ResultSet into a read-only table model. */
    public static DefaultTableModel toModel(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();

        String[] headers = new String[cols];
        for (int i = 1; i <= cols; i++) {
            headers[i - 1] = md.getColumnLabel(i);
        }

        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        while (rs.next()) {
            Object[] row = new Object[cols];
            for (int i = 1; i <= cols; i++) {
                row[i - 1] = rs.getObject(i);
            }
            model.addRow(row);
        }
        return model;
    }

    /** Runs a parameterised query and loads the result straight into a table. */
    public static void fill(JTable table, String sql, Object... params) {
        try (Connection c = Db.open();
             PreparedStatement ps = c.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                table.setModel(toModel(rs));
            }
            autoSize(table);

        } catch (SQLException e) {
            error(table, e);
        }
    }

    /** Widens each column to fit its content, capped so one long cell cannot dominate. */
    public static void autoSize(JTable table) {
        for (int col = 0; col < table.getColumnCount(); col++) {
            int width = 60;
            for (int row = 0; row < table.getRowCount(); row++) {
                Component comp = table.prepareRenderer(table.getCellRenderer(row, col), row, col);
                width = Math.max(width, comp.getPreferredSize().width + 16);
            }
            width = Math.min(width, 320);
            table.getColumnModel().getColumn(col).setPreferredWidth(width);
        }
    }

    public static void error(Component parent, Exception e) {
        JOptionPane.showMessageDialog(parent,
            e.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent,
            message, "Marketplace", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Reads an int from a table cell, tolerating Long/BigDecimal column types. */
    public static int intAt(JTable table, int row, int col) {
        Object value = table.getValueAt(row, col);
        return value == null ? -1 : Integer.parseInt(value.toString().trim());
    }
}
