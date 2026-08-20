"""Renders the entity relationship diagram with cardinality, using orthogonal routing."""
import math
import os
from PIL import Image, ImageDraw, ImageFont

OUT = r"D:\NSU Academic\4th Semester (12)\CSE311 NdA\project\docs\er-diagram.png"


def font(name, size):
    p = r"C:\Windows\Fonts\%s" % name
    return ImageFont.truetype(p, size) if os.path.exists(p) else ImageFont.load_default()


F_TITLE = font("segoeuib.ttf", 32)
F_SUB   = font("segoeui.ttf", 18)
F_ENT   = font("segoeuib.ttf", 15)
F_REL   = font("segoeui.ttf", 13)
F_LEG   = font("segoeui.ttf", 15)
F_LEGB  = font("segoeuib.ttf", 19)

BG    = (255, 255, 255)
INK   = (28, 32, 38)
MUTED = (108, 117, 128)
WIRE  = (158, 168, 180)

STYLE = {
    "user":    ((228, 240, 252), (44, 106, 172)),
    "catalog": ((228, 247, 234), (36, 122, 68)),
    "stock":   ((252, 244, 226), (150, 102, 18)),
    "order":   ((243, 235, 252), (108, 66, 168)),
    "fulfil":  ((252, 234, 234), (166, 54, 54)),
    "assoc":   ((237, 240, 244), (86, 95, 106)),
}

EW, EH  = 178, 50
COL_X   = [140, 435, 730, 1025, 1320]
ROW_Y   = [200, 350, 500, 650, 800]
GUT     = [COL_X[i] + EW + 58 for i in range(4)]      # gutters between columns
GUT_L   = COL_X[0] - 46                               # gutter left of column 0
GUT_R   = COL_X[4] + EW + 46                          # gutter right of column 4
HY_BOT  = ROW_Y[-1] + EH + 56
HY_TOP  = 122

# name -> (col, row, style, associative)
E = {
    "User":            (0, 0, "user",    0),
    "Vendor":          (0, 1, "user",    0),
    "Buyer":           (0, 2, "user",    0),
    "Support":         (0, 3, "user",    0),
    "Address":         (0, 4, "user",    0),

    "Category":        (1, 0, "catalog", 0),
    "Store":           (1, 1, "catalog", 0),
    "Cart Item":       (1, 2, "assoc",   1),
    "Wishlist Item":   (1, 3, "assoc",   1),
    "Warehouse":       (1, 4, "stock",   0),

    "Product":         (2, 0, "catalog", 0),
    "Promotion":       (2, 1, "stock",   0),
    "Product Variant": (2, 2, "catalog", 0),
    "Inventory":       (2, 3, "assoc",   1),

    "Checkout":        (3, 0, "order",   0),
    "Order":           (3, 1, "order",   0),
    "Order Item":      (3, 2, "assoc",   1),
    "Payment":         (3, 3, "order",   0),
    "Review":          (3, 4, "fulfil",  0),

    "Courier":         (4, 0, "fulfil",  0),
    "Shipment":        (4, 1, "fulfil",  0),
    "Shipment Item":   (4, 2, "assoc",   1),
    "Ticket":          (4, 4, "fulfil",  0),
}

# (a, b, label, card at a, card at b)
R = [
    ("Vendor",          "Store",           "owns",          "1", "N"),
    ("Store",           "Product",         "sells",         "1", "N"),
    ("Category",        "Product",         "classifies",    "1", "N"),
    ("Product",         "Product Variant", "has",           "1", "N"),
    ("Vendor",          "Promotion",       "creates",       "1", "N"),
    ("Promotion",       "Order",           "applied to",    "1", "N"),
    ("Buyer",           "Cart Item",       "",              "1", "N"),
    ("Cart Item",       "Product Variant", "cart",          "N", "1"),
    ("Buyer",           "Wishlist Item",   "",              "1", "N"),
    ("Wishlist Item",   "Product Variant", "wishlist",      "N", "1"),
    ("Product Variant", "Inventory",       "",              "1", "N"),
    ("Inventory",       "Warehouse",       "stored in",     "N", "1"),
    ("Warehouse",       "Address",         "located at",    "N", "1"),
    ("Buyer",           "Address",         "saves",         "1", "N"),
    ("Buyer",           "Checkout",        "places",        "1", "N"),
    ("Checkout",        "Order",           "splits into",   "1", "N"),
    ("Checkout",        "Payment",         "settled by",    "1", "1"),
    ("Checkout",        "Address",         "ships to",      "N", "1"),
    ("Order",           "Store",           "fulfilled by",  "N", "1"),
    ("Order",           "Order Item",      "",              "1", "N"),
    ("Order Item",      "Product Variant", "references",    "N", "1"),
    ("Order",           "Shipment",        "dispatched as", "1", "N"),
    ("Shipment",        "Courier",         "carried by",    "N", "1"),
    ("Shipment",        "Shipment Item",   "",              "1", "N"),
    ("Shipment Item",   "Order Item",      "contains",      "N", "1"),
    ("Shipment",        "Address",         "delivered to",  "N", "1"),
    ("Buyer",           "Review",          "writes",        "1", "N"),
    ("Review",          "Product Variant", "about",         "N", "1"),
    ("Buyer",           "Ticket",          "submits",       "1", "N"),
    ("Support",         "Ticket",          "manages",       "1", "N"),
    ("Ticket",          "Order",           "concerns",      "N", "1"),
    ("Payment",         "User",            "paid by",       "N", "1"),
]

W = GUT_R + 430
H = HY_BOT + 9 * 28 + 70
img  = Image.new("RGB", (W, H), BG)
draw = ImageDraw.Draw(img)


def box(name):
    c, r, s, a = E[name]
    x, y = COL_X[c], ROW_Y[r]
    return x, y, x + EW, y + EH


def cx(name):
    x1, _, x2, _ = box(name)
    return (x1 + x2) / 2.0


def cy(name):
    _, y1, _, y2 = box(name)
    return (y1 + y2) / 2.0


lane_v = {}
lane_h = {}


def vlane(x):
    lane_v[x] = lane_v.get(x, -1) + 1
    return x + ((lane_v[x] % 7) - 3) * 8


def hlane(y, down):
    lane_h[y] = lane_h.get(y, -1) + 1
    k = lane_h[y]
    return y + k * (28 if down else 17), k


def bar(p, ang, colour):
    nx, ny = -math.sin(ang), math.cos(ang)
    bx, by = p[0] + 12 * math.cos(ang), p[1] + 12 * math.sin(ang)
    draw.line([(bx - 7 * nx, by - 7 * ny), (bx + 7 * nx, by + 7 * ny)], fill=colour, width=3)


def foot(p, ang, colour):
    for spread in (-0.44, 0.0, 0.44):
        draw.line([p, (p[0] + 17 * math.cos(ang + spread),
                       p[1] + 17 * math.sin(ang + spread))], fill=colour, width=2)


def marker(p, ang, colour, many):
    (foot if many else bar)(p, ang, colour)


labels = []


def route(a, b, label, ca, cb):
    acol, arow = E[a][0], E[a][1]
    bcol, brow = E[b][0], E[b][1]
    colour = STYLE[E[a][2]][1]
    ax1, ay1, ax2, ay2 = box(a)
    bx1, by1, bx2, by2 = box(b)
    span = abs(acol - bcol)

    if acol == bcol:
        # vertical neighbour: run down the gutter beside the column
        gx = vlane(GUT[acol] if acol < 4 else GUT_R) if acol != 0 else vlane(GUT_L)
        pa = (ax2, cy(a)) if gx > ax2 else (ax1, cy(a))
        pb = (bx2, cy(b)) if gx > bx2 else (bx1, cy(b))
        pts = [pa, (gx, pa[1]), (gx, pb[1]), pb]
        aang = 0 if gx > ax2 else math.pi
        bang = 0 if gx > bx2 else math.pi

    elif span == 1:
        left, right = (a, b) if acol < bcol else (b, a)
        gx = vlane(GUT[min(acol, bcol)])
        pl = (box(left)[2], cy(left))
        pr = (box(right)[0], cy(right))
        pts = [pl, (gx, pl[1]), (gx, pr[1]), pr]
        if left == a:
            pa, pb, aang, bang = pl, pr, 0, math.pi
        else:
            pa, pb, aang, bang = pr, pl, math.pi, 0

    else:
        # long hop: leave sideways into a gutter, travel on a highway, come back
        down = max(arow, brow) >= 2
        hy, klane = hlane(HY_BOT if down else HY_TOP, down)
        ga   = vlane(GUT[acol] if acol < bcol else GUT[acol - 1])
        gb   = vlane(GUT[bcol - 1] if acol < bcol else GUT[bcol])
        pa   = (ax2, cy(a)) if acol < bcol else (ax1, cy(a))
        pb   = (bx1, cy(b)) if acol < bcol else (bx2, cy(b))
        aang = 0 if acol < bcol else math.pi
        bang = math.pi if acol < bcol else 0
        pts  = [pa, (ga, pa[1]), (ga, hy), (gb, hy), (gb, pb[1]), pb]
        if label:
            frac = (0.20, 0.38, 0.56, 0.74, 0.88)[klane % 5]
            labels.append(((ga + (gb - ga) * frac, hy), label))
            label = None

    draw.line(pts, fill=WIRE, width=2, joint="curve")
    marker(pa, aang, colour, ca in ("N", "M"))
    marker(pb, bang, STYLE[E[b][2]][1], cb in ("N", "M"))

    if label:
        best, blen = None, 0
        for i in range(len(pts) - 1):
            if abs(pts[i][0] - pts[i + 1][0]) < 1:          # vertical run
                seg = abs(pts[i][1] - pts[i + 1][1])
                if seg > blen:
                    blen, best = seg, (pts[i][0], (pts[i][1] + pts[i + 1][1]) / 2.0)
        if blen < 46:                                        # too short, use horizontal
            best, blen = None, 0
            for i in range(len(pts) - 1):
                if abs(pts[i][1] - pts[i + 1][1]) < 1:
                    seg = abs(pts[i][0] - pts[i + 1][0])
                    if seg > blen:
                        blen, best = seg, ((pts[i][0] + pts[i + 1][0]) / 2.0, pts[i][1])
        if best:
            labels.append((best, label))


for a, b, label, ca, cb in R:
    route(a, b, label, ca, cb)

# Category parent-of self reference
cx1, cy1, cx2, cy2 = box("Category")
draw.line([(cx2, cy1 + 14), (cx2 + 30, cy1 + 14), (cx2 + 30, cy1 - 26),
           (cx1 + 44, cy1 - 26), (cx1 + 44, cy1)], fill=WIRE, width=2)
foot((cx1 + 44, cy1), math.pi / 2, STYLE["catalog"][1])
labels.append(((cx1 + 116, cy1 - 26), "parent of"))

# ISA triangle
ux, uy1 = cx("User"), box("User")[3]
tri = uy1 + 34
draw.line([(ux, uy1), (ux, tri - 15)], fill=STYLE["user"][1], width=2)
draw.polygon([(ux, tri + 15), (ux - 30, tri - 15), (ux + 30, tri - 15)],
             outline=STYLE["user"][1], fill=(228, 240, 252))
tw = draw.textlength("ISA", font=F_REL)
draw.text((ux - tw / 2, tri - 12), "ISA", font=F_REL, fill=STYLE["user"][1])
hub = vlane(GUT_L)
for sub in ("Vendor", "Buyer", "Support"):
    sy = cy(sub)
    draw.line([(ux, tri + 15), (ux, tri + 26), (hub, tri + 26), (hub, sy),
               (box(sub)[0], sy)], fill=STYLE["user"][1], width=2)

# entity boxes painted over the wiring
for name, (c, r, s, assoc) in E.items():
    fill, edge = STYLE[s]
    x1, y1, x2, y2 = box(name)
    draw.rectangle([x1 + 3, y1 + 3, x2 + 3, y2 + 3], fill=(236, 239, 242))
    draw.rectangle([x1, y1, x2, y2], fill=fill, outline=edge, width=2)
    if assoc:
        draw.rectangle([x1 + 5, y1 + 5, x2 - 5, y2 - 5], outline=edge, width=1)
    tw = draw.textlength(name, font=F_ENT)
    draw.text((x1 + (EW - tw) / 2, y1 + EH / 2 - 10), name, font=F_ENT, fill=edge)

# labels painted last so nothing covers them
for (px, py), text in labels:
    tw = draw.textlength(text, font=F_REL)
    draw.rectangle([px - tw / 2 - 6, py - 11, px + tw / 2 + 6, py + 11],
                   fill=BG, outline=(214, 220, 226))
    draw.text((px - tw / 2, py - 8), text, font=F_REL, fill=(66, 74, 84))

draw.text((140, 40), "Multi-Vendor E-Commerce Marketplace", font=F_TITLE, fill=INK)
draw.text((140, 82), "Entity relationship diagram with cardinality  |  23 entities, 32 relationships",
          font=F_SUB, fill=MUTED)

lx, ly = GUT_R + 40, 190
draw.text((lx, ly - 38), "Legend", font=F_LEGB, fill=INK)
draw.rectangle([lx, ly, lx + 28, ly + 20], fill=STYLE["order"][0], outline=STYLE["order"][1], width=2)
draw.text((lx + 40, ly), "strong entity", font=F_LEG, fill=INK)
ly += 34
draw.rectangle([lx, ly, lx + 28, ly + 20], fill=STYLE["assoc"][0], outline=STYLE["assoc"][1], width=2)
draw.rectangle([lx + 4, ly + 4, lx + 24, ly + 16], outline=STYLE["assoc"][1])
draw.text((lx + 40, ly), "associative entity", font=F_LEG, fill=INK)

ly += 50
bar((lx + 6, ly + 10), 0, INK)
draw.text((lx + 40, ly), "exactly one", font=F_LEG, fill=MUTED)
ly += 34
foot((lx + 6, ly + 10), 0, INK)
draw.text((lx + 40, ly), "many", font=F_LEG, fill=MUTED)

ly += 46
for line in ["Crow's foot notation. The symbol nearest",
             "an entity states how many of that entity",
             "take part in the relationship.",
             "",
             "An associative entity resolves an M:N",
             "relationship carrying its own attributes.",
             "Order Item, for example, carries the",
             "quantity and the unit price snapshot.",
             "",
             "The ISA triangle is a total, disjoint",
             "specialisation: every user is exactly one",
             "of vendor, buyer or support."]:
    draw.text((lx, ly), line, font=F_LEG, fill=MUTED)
    ly += 23

os.makedirs(os.path.dirname(OUT), exist_ok=True)
img.save(OUT, dpi=(200, 200))
print("wrote", OUT, img.size)
