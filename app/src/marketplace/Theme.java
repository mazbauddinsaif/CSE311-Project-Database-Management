package marketplace;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;

/** Colours, fonts and formatting shared by every screen. */
public final class Theme {

    // Deep bottle green carries the navigation; marigold is the single accent.
    public static final Color DEEP     = new Color(0x0E, 0x47, 0x39);
    public static final Color DEEP_HI  = new Color(0x16, 0x62, 0x4E);
    public static final Color DEEP_DIM = new Color(0x9C, 0xC0, 0xB4);
    public static final Color MARIGOLD = new Color(0xE0, 0xA3, 0x3C);
    public static final Color MARIGOLD_DK = new Color(0xC2, 0x88, 0x25);

    public static final Color INK      = new Color(0x10, 0x14, 0x18);
    public static final Color MUTED    = new Color(0x6B, 0x72, 0x80);
    public static final Color FAINT    = new Color(0x9A, 0xA1, 0xA9);
    public static final Color LINE     = new Color(0xE3, 0xE6, 0xE2);
    public static final Color PAPER    = new Color(0xF6, 0xF7, 0xF5);
    public static final Color SURFACE  = Color.WHITE;
    public static final Color ROW_ALT  = new Color(0xFA, 0xFB, 0xFA);
    public static final Color SELECT   = new Color(0xE4, 0xEF, 0xEA);

    public static final Color OK       = new Color(0x14, 0x7A, 0x5C);
    public static final Color INFO     = new Color(0x25, 0x63, 0xA8);
    public static final Color WARN     = new Color(0xA9, 0x77, 0x0B);
    public static final Color DANGER   = new Color(0xB0, 0x3A, 0x3A);

    private static String displayFamily = "Segoe UI";
    private static String bodyFamily    = "Segoe UI";
    private static String monoFamily    = "Consolas";
    private static String taka          = "Tk";

    private Theme() { }

    /** Installs FlatLaf and resolves which fonts and symbols this machine can render. */
    public static void install() {
        try {
            FlatLightLaf.setup();
        } catch (Exception e) {
            // the default look and feel still works, it is only less refined
        }

        Set<String> families = new HashSet<String>();
        for (String f : GraphicsEnvironment.getLocalGraphicsEnvironment()
                                           .getAvailableFontFamilyNames()) {
            families.add(f);
        }
        // Bahnschrift is condensed and technical, which suits a fulfilment system.
        if (families.contains("Bahnschrift")) {
            displayFamily = "Bahnschrift";
        } else if (families.contains("Segoe UI Semibold")) {
            displayFamily = "Segoe UI Semibold";
        }
        if (!families.contains("Segoe UI")) {
            bodyFamily = Font.SANS_SERIF;
        }
        if (!families.contains("Consolas")) {
            monoFamily = Font.MONOSPACED;
        }

        // Amounts are rendered in the monospaced face, so that is the font that has
        // to be able to draw the taka sign. Consolas cannot, hence the Tk fallback.
        if (new Font(monoFamily, Font.PLAIN, 12).canDisplay('৳')
                && new Font(bodyFamily, Font.PLAIN, 12).canDisplay('৳')) {
            taka = "৳";
        }

        UIManager.put("Component.focusWidth", Integer.valueOf(0));
        UIManager.put("Component.innerFocusWidth", Integer.valueOf(1));
        UIManager.put("Component.arc", Integer.valueOf(8));
        UIManager.put("Button.arc", Integer.valueOf(8));
        UIManager.put("TextComponent.arc", Integer.valueOf(8));
        UIManager.put("Component.focusColor", MARIGOLD);
        UIManager.put("Component.borderColor", LINE);
        UIManager.put("Panel.background", PAPER);
        UIManager.put("ScrollBar.thumbArc", Integer.valueOf(8));
        UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.width", Integer.valueOf(11));
        UIManager.put("ScrollPane.smoothScrolling", Boolean.TRUE);
        UIManager.put("Table.showHorizontalLines", Boolean.TRUE);
        UIManager.put("Table.showVerticalLines", Boolean.FALSE);
        UIManager.put("Table.gridColor", LINE);
        UIManager.put("Table.intercellSpacing", new java.awt.Dimension(0, 0));
        UIManager.put("TableHeader.separatorColor", LINE);
        UIManager.put("TableHeader.bottomSeparatorColor", LINE);
        UIManager.put("TabbedPane.showTabSeparators", Boolean.TRUE);
        UIManager.put("defaultFont", body(13));
    }

    public static Font display(int size) {
        return new Font(displayFamily, Font.BOLD, size);
    }

    public static Font body(int size) {
        return new Font(bodyFamily, Font.PLAIN, size);
    }

    public static Font bodyBold(int size) {
        return new Font(bodyFamily, Font.BOLD, size);
    }

    public static Font mono(int size) {
        return new Font(monoFamily, Font.PLAIN, size);
    }

    /** Small caps-style label: uppercase, letter spaced by the caller. */
    public static Font label(int size) {
        return new Font(bodyFamily, Font.BOLD, size);
    }

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final DecimalFormat WHOLE = new DecimalFormat("#,##0");

    public static String money(Object value) {
        if (value == null) {
            return "";
        }
        try {
            return taka + " " + MONEY.format(new BigDecimal(value.toString()));
        } catch (NumberFormatException e) {
            return value.toString();
        }
    }

    public static String moneyShort(Object value) {
        if (value == null) {
            return taka + " 0";
        }
        try {
            BigDecimal v = new BigDecimal(value.toString());
            if (v.compareTo(new BigDecimal("100000")) >= 0) {
                return taka + " " + WHOLE.format(v.divide(new BigDecimal("100000"),
                        2, RoundingMode.HALF_UP)) + " lakh";
            }
            return taka + " " + WHOLE.format(v);
        } catch (NumberFormatException e) {
            return value.toString();
        }
    }

    public static String currency() {
        return taka;
    }

    /** Colour used for a status pill. */
    public static Color statusColour(String status) {
        if (status == null) {
            return MUTED;
        }
        String s = status.trim().toUpperCase();
        if (s.equals("DELIVERED") || s.equals("RESOLVED") || s.equals("CLOSED")) {
            return OK;
        }
        if (s.equals("SHIPPED") || s.equals("IN_TRANSIT") || s.equals("IN_PROGRESS")) {
            return INFO;
        }
        if (s.equals("PACKED") || s.equals("PENDING") || s.equals("OPEN")) {
            return WARN;
        }
        if (s.equals("CANCELLED") || s.equals("RETURNED")) {
            return DANGER;
        }
        return MUTED;
    }
}
