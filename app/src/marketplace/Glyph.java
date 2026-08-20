package marketplace;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Stroke drawn icons. Kept as vector paths rather than image files so the
 * application stays a single jar with no external assets.
 */
public class Glyph implements Icon {

    private final String name;
    private final int    size;
    private Color        colour;

    public Glyph(String name, int size, Color colour) {
        this.name   = name;
        this.size   = size;
        this.colour = colour;
    }

    public void setColour(Color c) {
        this.colour = c;
    }

    public int getIconWidth()  { return size; }
    public int getIconHeight() { return size; }

    public void paintIcon(Component c, Graphics g0, int x, int y) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(x, y);
        g.setColor(colour);

        float s = size;
        float w = Math.max(1.5f, s / 11f);
        g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        if ("browse".equals(name)) {
            float u = s * 0.36f, gap = s * 0.10f, o = s * 0.09f;
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    g.draw(new RoundRectangle2D.Float(o + i * (u + gap), o + j * (u + gap),
                                                      u, u, w * 2, w * 2));
                }
            }
        } else if ("cart".equals(name)) {
            g.draw(new Line2D.Float(s * 0.10f, s * 0.20f, s * 0.26f, s * 0.20f));
            GeneralPath p = new GeneralPath();
            p.moveTo(s * 0.26f, s * 0.20f);
            p.lineTo(s * 0.38f, s * 0.62f);
            p.lineTo(s * 0.84f, s * 0.62f);
            p.lineTo(s * 0.92f, s * 0.32f);
            p.lineTo(s * 0.31f, s * 0.32f);
            g.draw(p);
            g.fill(new Ellipse2D.Float(s * 0.42f, s * 0.74f, w * 2.2f, w * 2.2f));
            g.fill(new Ellipse2D.Float(s * 0.72f, s * 0.74f, w * 2.2f, w * 2.2f));
        } else if ("orders".equals(name)) {
            g.draw(new RoundRectangle2D.Float(s * 0.14f, s * 0.24f, s * 0.72f, s * 0.60f,
                                              w * 2, w * 2));
            g.draw(new Line2D.Float(s * 0.14f, s * 0.44f, s * 0.86f, s * 0.44f));
            g.draw(new Line2D.Float(s * 0.36f, s * 0.24f, s * 0.36f, s * 0.84f));
            g.draw(new Line2D.Float(s * 0.30f, s * 0.14f, s * 0.70f, s * 0.14f));
        } else if ("user".equals(name)) {
            g.draw(new Ellipse2D.Float(s * 0.32f, s * 0.16f, s * 0.36f, s * 0.36f));
            GeneralPath p = new GeneralPath();
            p.moveTo(s * 0.16f, s * 0.88f);
            p.quadTo(s * 0.50f, s * 0.56f, s * 0.84f, s * 0.88f);
            g.draw(p);
        } else if ("chart".equals(name)) {
            g.draw(new Line2D.Float(s * 0.14f, s * 0.86f, s * 0.88f, s * 0.86f));
            g.draw(new Line2D.Float(s * 0.28f, s * 0.86f, s * 0.28f, s * 0.56f));
            g.draw(new Line2D.Float(s * 0.50f, s * 0.86f, s * 0.50f, s * 0.30f));
            g.draw(new Line2D.Float(s * 0.72f, s * 0.86f, s * 0.72f, s * 0.46f));
        } else if ("box".equals(name)) {
            GeneralPath p = new GeneralPath();
            p.moveTo(s * 0.50f, s * 0.12f);
            p.lineTo(s * 0.88f, s * 0.32f);
            p.lineTo(s * 0.88f, s * 0.72f);
            p.lineTo(s * 0.50f, s * 0.92f);
            p.lineTo(s * 0.12f, s * 0.72f);
            p.lineTo(s * 0.12f, s * 0.32f);
            p.closePath();
            g.draw(p);
            g.draw(new Line2D.Float(s * 0.12f, s * 0.32f, s * 0.50f, s * 0.52f));
            g.draw(new Line2D.Float(s * 0.88f, s * 0.32f, s * 0.50f, s * 0.52f));
            g.draw(new Line2D.Float(s * 0.50f, s * 0.52f, s * 0.50f, s * 0.92f));
        } else if ("alert".equals(name)) {
            GeneralPath p = new GeneralPath();
            p.moveTo(s * 0.50f, s * 0.12f);
            p.lineTo(s * 0.92f, s * 0.84f);
            p.lineTo(s * 0.08f, s * 0.84f);
            p.closePath();
            g.draw(p);
            g.draw(new Line2D.Float(s * 0.50f, s * 0.40f, s * 0.50f, s * 0.60f));
            g.fill(new Ellipse2D.Float(s * 0.455f, s * 0.68f, w * 1.6f, w * 1.6f));
        } else if ("ticket".equals(name)) {
            g.draw(new RoundRectangle2D.Float(s * 0.12f, s * 0.20f, s * 0.76f, s * 0.52f,
                                              w * 2.5f, w * 2.5f));
            GeneralPath p = new GeneralPath();
            p.moveTo(s * 0.30f, s * 0.72f);
            p.lineTo(s * 0.30f, s * 0.90f);
            p.lineTo(s * 0.48f, s * 0.72f);
            g.draw(p);
            g.draw(new Line2D.Float(s * 0.28f, s * 0.38f, s * 0.72f, s * 0.38f));
            g.draw(new Line2D.Float(s * 0.28f, s * 0.54f, s * 0.58f, s * 0.54f));
        } else if ("heart".equals(name)) {
            GeneralPath p = new GeneralPath();
            p.moveTo(s * 0.50f, s * 0.86f);
            p.curveTo(s * 0.06f, s * 0.58f, s * 0.18f, s * 0.16f, s * 0.50f, s * 0.34f);
            p.curveTo(s * 0.82f, s * 0.16f, s * 0.94f, s * 0.58f, s * 0.50f, s * 0.86f);
            p.closePath();
            g.draw(p);
        } else if ("search".equals(name)) {
            g.draw(new Ellipse2D.Float(s * 0.14f, s * 0.14f, s * 0.52f, s * 0.52f));
            g.draw(new Line2D.Float(s * 0.62f, s * 0.62f, s * 0.88f, s * 0.88f));
        } else if ("split".equals(name)) {
            g.draw(new Line2D.Float(s * 0.12f, s * 0.50f, s * 0.42f, s * 0.50f));
            g.draw(new Line2D.Float(s * 0.42f, s * 0.22f, s * 0.42f, s * 0.78f));
            g.draw(new Line2D.Float(s * 0.42f, s * 0.22f, s * 0.86f, s * 0.22f));
            g.draw(new Line2D.Float(s * 0.42f, s * 0.50f, s * 0.86f, s * 0.50f));
            g.draw(new Line2D.Float(s * 0.42f, s * 0.78f, s * 0.86f, s * 0.78f));
        } else if ("store".equals(name)) {
            g.draw(new Line2D.Float(s * 0.12f, s * 0.34f, s * 0.88f, s * 0.34f));
            GeneralPath p = new GeneralPath();
            p.moveTo(s * 0.12f, s * 0.34f);
            p.lineTo(s * 0.22f, s * 0.14f);
            p.lineTo(s * 0.78f, s * 0.14f);
            p.lineTo(s * 0.88f, s * 0.34f);
            g.draw(p);
            g.draw(new java.awt.geom.Rectangle2D.Float(s * 0.18f, s * 0.34f,
                                                       s * 0.64f, s * 0.52f));
            g.draw(new java.awt.geom.Rectangle2D.Float(s * 0.38f, s * 0.56f,
                                                       s * 0.24f, s * 0.30f));
        } else if ("logout".equals(name)) {
            GeneralPath p = new GeneralPath();
            p.moveTo(s * 0.56f, s * 0.16f);
            p.lineTo(s * 0.18f, s * 0.16f);
            p.lineTo(s * 0.18f, s * 0.84f);
            p.lineTo(s * 0.56f, s * 0.84f);
            g.draw(p);
            g.draw(new Line2D.Float(s * 0.44f, s * 0.50f, s * 0.88f, s * 0.50f));
            GeneralPath a = new GeneralPath();
            a.moveTo(s * 0.72f, s * 0.34f);
            a.lineTo(s * 0.88f, s * 0.50f);
            a.lineTo(s * 0.72f, s * 0.66f);
            g.draw(a);
        }
        g.dispose();
    }
}
