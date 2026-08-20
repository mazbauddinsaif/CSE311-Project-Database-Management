"""Renders the relational schema diagram (27 tables) to a PNG."""
import os
from PIL import Image, ImageDraw, ImageFont

OUT = r"D:\NSU Academic\4th Semester (12)\CSE311 NdA\project\docs\schema-diagram.png"

# ---------------------------------------------------------------- fonts
def font(name, size):
    for path in (r"C:\Windows\Fonts\%s" % name,):
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()

F_TITLE   = font("segoeuib.ttf", 30)
F_SUB     = font("segoeui.ttf", 17)
F_HEAD    = font("segoeuib.ttf", 16)
F_COL     = font("consola.ttf", 14)
F_LEGEND  = font("segoeui.ttf", 15)
F_CLUSTER = font("segoeuib.ttf", 18)

# ---------------------------------------------------------------- colours
BG        = (255, 255, 255)
INK       = (28, 32, 38)
MUTED     = (108, 117, 128)
LINE      = (176, 186, 197)
BOX_EDGE  = (150, 160, 172)
ROW_ALT   = (250, 251, 252)

CLUSTERS = {
    "users":     ((228, 240, 252), (44, 106, 172), "Users and addresses"),
    "catalog":   ((228, 247, 234), (36, 122, 68),  "Stores and catalogue"),
    "stock":     ((252, 244, 226), (158, 108, 20), "Inventory and promotions"),
    "orders":    ((243, 235, 252), (108, 66, 168), "Checkout and orders"),
    "fulfil":    ((252, 234, 234), (166, 54, 54),  "Fulfilment and after sales"),
}

# ---------------------------------------------------------------- schema
# table -> (cluster, column, [(name, kind)]) where kind in {pk, fk, pkfk, ''}
T = {
 "user":            ("users", 0, [("user_id","pk"),("email","u"),("password",""),("first_name",""),("last_name",""),("created_at","")]),
 "user_phone":      ("users", 0, [("user_id","pkfk"),("phone","pk")]),
 "vendor":          ("users", 0, [("user_id","pkfk"),("business_name",""),("tax_id","u"),("commission_rate","")]),
 "buyer":           ("users", 0, [("user_id","pkfk"),("dob",""),("loyalty_points","")]),
 "support":         ("users", 0, [("user_id","pkfk"),("response_time_min","")]),
 "address":         ("users", 0, [("address_id","pk"),("owner_user_id","fk"),("house",""),("block",""),("street",""),("city",""),("postal_code",""),("country","")]),

 "store":           ("catalog", 1, [("store_id","pk"),("vendor_id","fk"),("name",""),("reputation_score",""),("is_active","")]),
 "category":        ("catalog", 1, [("category_id","pk"),("name",""),("parent_category_id","fk")]),
 "product":         ("catalog", 1, [("product_id","pk"),("store_id","fk"),("category_id","fk"),("title",""),("description",""),("brand","")]),
 "product_variant": ("catalog", 1, [("variant_id","pk"),("product_id","fk"),("barcode","u"),("description",""),("price",""),("low_stock_threshold","")]),
 "variant_option":  ("catalog", 1, [("variant_id","pkfk"),("option_name","pk"),("option_value","")]),
 "variant_image":   ("catalog", 1, [("image_id","pk"),("variant_id","fk"),("image_url","")]),

 "warehouse":       ("stock", 2, [("warehouse_id","pk"),("name",""),("address_id","fk")]),
 "inventory":       ("stock", 2, [("variant_id","pkfk"),("warehouse_id","pkfk"),("quantity","")]),
 "promotion":       ("stock", 2, [("promotion_id","pk"),("vendor_id","fk"),("code","u"),("reason",""),("discount_type",""),("amount",""),("min_order_amount",""),("start_date",""),("end_date","")]),
 "promotion_variant":("stock", 2, [("promotion_id","pkfk"),("variant_id","pkfk")]),
 "cart_item":       ("stock", 2, [("buyer_id","pkfk"),("variant_id","pkfk"),("quantity",""),("added_at","")]),
 "wishlist_item":   ("stock", 2, [("buyer_id","pkfk"),("variant_id","pkfk"),("added_at","")]),

 "checkout":        ("orders", 3, [("checkout_id","pk"),("buyer_id","fk"),("shipping_address_id","fk"),("checkout_date",""),("total_amount","")]),
 "customer_order":  ("orders", 3, [("order_id","pk"),("checkout_id","fk"),("store_id","fk"),("promotion_id","fk"),("order_date",""),("status",""),("total_amount","")]),
 "order_item":      ("orders", 3, [("order_item_id","pk"),("order_id","fk"),("variant_id","fk"),("quantity",""),("unit_price","")]),
 "payment":         ("orders", 3, [("payment_id","pk"),("checkout_id","u fk"),("paid_by_user_id","fk"),("payment_method",""),("paid_amount",""),("paid_date",""),("reference","u")]),

 "shipment_company":("fulfil", 4, [("company_id","pk"),("courier_name","u"),("phone_number",""),("is_active","")]),
 "shipment":        ("fulfil", 4, [("shipment_id","pk"),("order_id","fk"),("company_id","fk"),("address_id","fk"),("dispatch_date",""),("estimated_delivery_date",""),("status","")]),
 "shipment_item":   ("fulfil", 4, [("shipment_id","pkfk"),("order_item_id","pkfk"),("quantity","")]),
 "review":          ("fulfil", 4, [("buyer_id","pkfk"),("variant_id","pkfk"),("stars",""),("review_text",""),("review_date","")]),
 "ticket":          ("fulfil", 4, [("ticket_id","pk"),("buyer_id","fk"),("support_id","fk"),("order_id","fk"),("description",""),("status",""),("resolution_text",""),("created_at","")]),
}

# (child table, child column) -> parent table
FK = [
 ("user_phone","user_id","user"),
 ("vendor","user_id","user"),
 ("buyer","user_id","user"),
 ("support","user_id","user"),
 ("address","owner_user_id","user"),
 ("store","vendor_id","vendor"),
 ("category","parent_category_id","category"),
 ("product","store_id","store"),
 ("product","category_id","category"),
 ("product_variant","product_id","product"),
 ("variant_option","variant_id","product_variant"),
 ("variant_image","variant_id","product_variant"),
 ("warehouse","address_id","address"),
 ("inventory","variant_id","product_variant"),
 ("inventory","warehouse_id","warehouse"),
 ("promotion","vendor_id","vendor"),
 ("promotion_variant","promotion_id","promotion"),
 ("promotion_variant","variant_id","product_variant"),
 ("cart_item","buyer_id","buyer"),
 ("cart_item","variant_id","product_variant"),
 ("wishlist_item","buyer_id","buyer"),
 ("wishlist_item","variant_id","product_variant"),
 ("checkout","buyer_id","buyer"),
 ("checkout","shipping_address_id","address"),
 ("customer_order","checkout_id","checkout"),
 ("customer_order","store_id","store"),
 ("customer_order","promotion_id","promotion"),
 ("order_item","order_id","customer_order"),
 ("order_item","variant_id","product_variant"),
 ("payment","checkout_id","checkout"),
 ("payment","paid_by_user_id","user"),
 ("shipment","order_id","customer_order"),
 ("shipment","company_id","shipment_company"),
 ("shipment","address_id","address"),
 ("shipment_item","shipment_id","shipment"),
 ("shipment_item","order_item_id","order_item"),
 ("review","buyer_id","buyer"),
 ("review","variant_id","product_variant"),
 ("ticket","buyer_id","buyer"),
 ("ticket","support_id","support"),
 ("ticket","order_id","customer_order"),
]

# ---------------------------------------------------------------- layout
BOX_W     = 300
ROW_H     = 21
HEAD_H    = 28
GAP_Y     = 26
COL_X     = [70, 470, 870, 1270, 1670]
TOP       = 150
GUTTER    = 46          # horizontal space between a column and the next

pos = {}
col_y = {c: TOP for c in range(5)}
for name, (cluster, col, cols) in T.items():
    h = HEAD_H + ROW_H * len(cols)
    pos[name] = (COL_X[col], col_y[col], BOX_W, h, cluster, cols)
    col_y[col] += h + GAP_Y

W = COL_X[-1] + BOX_W + 340
H = max(col_y.values()) + 60

img  = Image.new("RGB", (W, H), BG)
draw = ImageDraw.Draw(img)


def row_y(table, column):
    x, y, w, h, cluster, cols = pos[table]
    for i, (cname, _) in enumerate(cols):
        if cname == column:
            return y + HEAD_H + i * ROW_H + ROW_H // 2
    return y + HEAD_H // 2


def pk_y(table):
    x, y, w, h, cluster, cols = pos[table]
    return y + HEAD_H + ROW_H // 2


# ---------------------------------------------------------------- edges
# Routed first so boxes paint over the ends.
lane = {}
for i, (child, ccol, parent) in enumerate(FK):
    cx, cy, cw, ch, ccl, _ = pos[child]
    px, py, pw, ph, pcl, _ = pos[parent]
    y1 = row_y(child, ccol)
    y2 = pk_y(parent)
    colour = CLUSTERS[pcl][1]
    tint = tuple(int(c + (235 - c) * 0.55) for c in colour)

    if child == parent:                       # category -> category
        draw.line([(cx + cw, y1), (cx + cw + 22, y1),
                   (cx + cw + 22, y2 - 16), (cx + cw, y2 - 16)], fill=colour, width=2)
        draw.polygon([(cx + cw + 6, y2 - 16), (cx + cw, y2 - 20), (cx + cw, y2 - 12)], fill=colour)
        continue

    if px < cx:                               # parent to the left
        start = (cx, y1)
        end   = (px + pw, y2)
        gx    = px + pw + GUTTER // 2
        key   = (round(gx / 8), 0)
        lane[key] = lane.get(key, 0) + 1
        gx += (lane[key] % 6) * 6
        draw.line([start, (gx, y1), (gx, y2), end], fill=tint, width=2)
        draw.polygon([(end[0], end[1]), (end[0] + 9, end[1] - 4), (end[0] + 9, end[1] + 4)], fill=colour)
    else:                                     # parent to the right or same column
        start = (cx + cw, y1)
        end   = (px, y2) if px > cx else (px + pw, y2)
        gx    = (cx + cw + px) // 2 if px > cx else cx + cw + 20
        key   = (round(gx / 8), 1)
        lane[key] = lane.get(key, 0) + 1
        gx += (lane[key] % 6) * 6
        draw.line([start, (gx, y1), (gx, y2), end], fill=tint, width=2)
        if px > cx:
            draw.polygon([(end[0], end[1]), (end[0] - 9, end[1] - 4), (end[0] - 9, end[1] + 4)], fill=colour)
        else:
            draw.polygon([(end[0], end[1]), (end[0] + 9, end[1] - 4), (end[0] + 9, end[1] + 4)], fill=colour)

# ---------------------------------------------------------------- boxes
for name, (x, y, w, h, cluster, cols) in pos.items():
    fill, edge, _ = CLUSTERS[cluster]

    draw.rectangle([x + 3, y + 3, x + w + 3, y + h + 3], fill=(238, 240, 243))
    draw.rectangle([x, y, x + w, y + h], fill=BG, outline=BOX_EDGE, width=1)
    draw.rectangle([x, y, x + w, y + HEAD_H], fill=fill, outline=edge, width=1)
    draw.text((x + 10, y + 6), name, font=F_HEAD, fill=edge)

    for i, (cname, kind) in enumerate(cols):
        ry = y + HEAD_H + i * ROW_H
        if i % 2 == 1:
            draw.rectangle([x + 1, ry, x + w - 1, ry + ROW_H], fill=ROW_ALT)

        marks = []
        if "pk" in kind:
            marks.append("PK")
        if "fk" in kind:
            marks.append("FK")
        if kind.startswith("u") or " u" in kind or kind == "u":
            marks.append("U")
        tag = ",".join(marks)

        draw.text((x + 10, ry + 3), tag, font=F_COL, fill=edge if tag else MUTED)
        label_x = x + 52
        is_key = "pk" in kind
        draw.text((label_x, ry + 3), cname, font=F_COL,
                  fill=INK if is_key else (72, 80, 90))
        if is_key:
            tw = draw.textlength(cname, font=F_COL)
            draw.line([(label_x, ry + ROW_H - 4), (label_x + tw, ry + ROW_H - 4)], fill=INK, width=1)

# ---------------------------------------------------------------- titles
draw.text((70, 46), "Multi-Vendor E-Commerce Marketplace", font=F_TITLE, fill=INK)
draw.text((70, 88), "Relational schema diagram  |  27 tables, 41 foreign keys  |  MariaDB / InnoDB",
          font=F_SUB, fill=MUTED)

lx, ly = COL_X[-1] + BOX_W + 40, TOP
draw.text((lx, ly - 34), "Legend", font=F_CLUSTER, fill=INK)
for key in ["users", "catalog", "stock", "orders", "fulfil"]:
    fill, edge, label = CLUSTERS[key]
    draw.rectangle([lx, ly, lx + 22, ly + 16], fill=fill, outline=edge)
    draw.text((lx + 32, ly - 1), label, font=F_LEGEND, fill=INK)
    ly += 28

ly += 14
for tag, meaning in [("PK", "primary key (underlined)"),
                     ("FK", "foreign key"),
                     ("PK,FK", "key inherited from parent"),
                     ("U", "unique constraint")]:
    draw.text((lx, ly), tag, font=F_COL, fill=INK)
    draw.text((lx + 62, ly), meaning, font=F_LEGEND, fill=MUTED)
    ly += 24

ly += 14
draw.text((lx, ly), "Arrows point from the foreign key", font=F_LEGEND, fill=MUTED)
draw.text((lx, ly + 20), "to the primary key it references.", font=F_LEGEND, fill=MUTED)

os.makedirs(os.path.dirname(OUT), exist_ok=True)
img.save(OUT, dpi=(200, 200))
print("wrote", OUT, img.size)
