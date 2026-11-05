"""生成预览图与品牌总览图（PNG）。

所有位图都由 canvas.py 自己光栅化，不用 ImageMagick 的 SVG 渲染器，
避免预览和浏览器所见不一致。
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from PIL import Image, ImageDraw, ImageFont

import build
from canvas import Canvas

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.dirname(HERE)
PREV = os.path.join(OUT, "preview")
MONO = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf"
CJK = "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"

INK, RED, PAPER = build.INK, build.RED, build.PAPER


def font(size, cjk=False):
    path = CJK if cjk else MONO
    try:
        return ImageFont.truetype(path, size)
    except OSError:
        return ImageFont.truetype(MONO, size)


def paste(base, cv, x, y, w, h, bg):
    img = cv.render(out_size=(w, h), bg_override=bg)
    base.paste(img, (x, y))


def main():
    os.makedirs(PREV, exist_ok=True)

    lock = build.lockup(INK, RED, **build.LOCKUP)
    lock_inv = build.lockup(PAPER, PAPER, **build.LOCKUP)
    mk = build.mark(256, RED, True, ratio=0.21)
    mk_plain = build.mark(256, RED, False)
    fav = build.favicon_cv(64)

    # ---- 单件预览
    lock.save_png(os.path.join(PREV, "lockup.png"),
                  out_size=(int(lock.w * 2.4), int(lock.h * 2.4)), bg_override=PAPER)
    lock_inv.save_png(os.path.join(PREV, "lockup-inverse.png"),
                      out_size=(int(lock.w * 2.4), int(lock.h * 2.4)), bg_override=INK)
    mk.save_png(os.path.join(PREV, "mark.png"), out_size=(512, 512), bg_override=PAPER)
    mk_plain.save_png(os.path.join(PREV, "mark-plain.png"), out_size=(512, 512), bg_override=PAPER)

    # ---- 尺寸阶梯
    sizes = [16, 24, 32, 48, 64, 96, 128]
    gap = 34
    W = sum(sizes) + gap * (len(sizes) + 1)
    H = 128 + 96
    board = Image.new("RGB", (W, H), PAPER)
    dr = ImageDraw.Draw(board)
    f = font(15)
    x = gap
    for s in sizes:
        cv = build.favicon_cv(s, **build.FAVICON_STEPS.get(s, {})) if s < 48 else mk
        img = cv.render(out_size=(s, s), bg_override=PAPER)
        board.paste(img, (x, 40 + (128 - s) // 2))
        label = f"{s}px"
        dr.text((x, 40 + 128 + 18), label, font=f, fill="#8a7d6e")
        x += s + gap
    board.save(os.path.join(PREV, "sizes.png"))

    # ---- 品牌总览
    SW, SH = 1680, 1120
    sheet = Image.new("RGB", (SW, SH), PAPER)
    dr = ImageDraw.Draw(sheet)
    dr.rectangle([40, 40, SW - 40, SH - 40], outline=INK, width=3)

    fh = font(26)
    fl = font(15)
    dr.text((76, 74), "HUNDUN LAB", font=fh, fill=INK)
    dr.text((300, 82), "馄饨系列软件 · 品牌标识 v1", font=font(18, True), fill="#8a7d6e")
    dr.line([76, 130, SW - 76, 130], fill="#d8c9ac", width=2)

    # 主组合
    hw = 1240
    hh = int(lock.h * hw / lock.w)
    paste(sheet, lock, (SW - hw) // 2, 196, hw, hh, PAPER)

    # 三格
    ty = 470
    th = 330
    col = (SW - 152) // 3
    tiles = [
        (mk, "图形标 / mark", PAPER, INK),
        (mk_plain, "图形标 · 无字版 / small", PAPER, INK),
        (lock_inv, "反白组合 / inverse", INK, PAPER),
    ]
    for i, (cv, label, bg, fg) in enumerate(tiles):
        x = 76 + i * (col + 0)
        box = [x, ty, x + col - 20, ty + th]
        dr.rectangle(box, fill=bg, outline="#d8c9ac", width=2)
        if cv is lock_inv:
            w = col - 120
            h = int(cv.h * w / cv.w)
        else:
            w = 190
            h = 190
        paste(sheet, cv, x + (col - 20 - w) // 2, ty + (th - h) // 2 - 12, w, h, bg)
        dr.text((x + 22, ty + th - 42), label, font=font(15, True), fill=fg if bg == INK else "#8a7d6e")

    # 色板
    cy = 880
    dr.text((76, cy - 34), "COLOR", font=fl, fill="#8a7d6e")
    chips = [(RED, "#C3272B", "朱红 / 主色"), (INK, "#17130F", "墨黑 / 字标"),
             (PAPER, "#FFF8E8", "米白 / 底"), ("#e9dcc6", "#E9DCC6", "次级底")]
    cx = 76
    for colr, hx, name in chips:
        dr.rectangle([cx, cy, cx + 150, cy + 92], fill=colr, outline="#d8c9ac", width=2)
        dr.text((cx, cy + 104), hx, font=fl, fill=INK)
        dr.text((cx, cy + 126), name, font=font(14, True), fill="#8a7d6e")
        cx += 176

    dr.text((SW - 480, cy + 8), "字体", font=font(15, True), fill="#8a7d6e")
    dr.text((SW - 480, cy + 32), "英文主标：自绘几何等宽，方角宽字距", font=font(16, True), fill=INK)
    dr.text((SW - 480, cy + 60), "中文副标：毛笔手写，已转路径", font=font(16, True), fill=INK)
    dr.text((SW - 480, cy + 88), "图形：沿用馄饨系列手绘轮廓", font=font(16, True), fill=INK)

    sheet.save(os.path.join(OUT, "brand-sheet.png"))
    print("brand-sheet.png", os.path.getsize(os.path.join(OUT, "brand-sheet.png")) // 1024, "KB")


if __name__ == "__main__":
    main()
