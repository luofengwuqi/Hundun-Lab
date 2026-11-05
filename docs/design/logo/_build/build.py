"""Hundun Lab 品牌标识生成器。

设计约束：
  家族胎记  —— 沿用馄饨系列那只手绘馄饨轮廓（与 HundunDesktop 图标同源）
  英文主标  —— 单线等宽、方角、宽字距（丝印/设备面板感）
  中文副标  —— 毛笔手写（马善政楷书描成的路径），承接兄弟品牌的手作气
  配色      —— 朱红 #C3272B / 墨黑 #17130F / 馄饨盒米白 #FFF8E8
"""

import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from canvas import Canvas
from stroke import stroke_to_polys

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.dirname(HERE)

INK = "#17130f"
RED = "#c3272b"
PAPER = "#fff8e8"

# ---------------------------------------------------------------- 字标骨架
from letters import draw as _draw_letters, width as _letters_width

ADV, SPACE, BODY = 12.0, 6.5, 10.0


def wordmark_width(text, cap):
    return _letters_width(text, cap)


def draw_wordmark(cv, text, x, y_top, cap, color, weight=None):
    _draw_letters(cv, text, x, y_top, cap, color)


# ---------------------------------------------------------------- 素材
DATA = json.load(open(os.path.join(HERE, "assets.json")))


def _bbox_of(parts):
    xs, ys = [], []
    for part in parts:
        for seg in part:
            if seg[0] == 'M':
                xs.append(seg[1][0]); ys.append(seg[1][1])
            elif seg[0] == 'C':
                for p in seg[1:]:
                    xs.append(p[0]); ys.append(p[1])
    return min(xs), min(ys), max(xs), max(ys)


def _place(cv, color, parts, bbox, x, y, w, h):
    bx0, by0, bx1, by1 = bbox
    bw, bh = bx1 - bx0, by1 - by0
    k = min(w / bw, h / bh)
    ox = x + (w - bw * k) / 2 - bx0 * k
    oy = y + (h - bh * k) / 2 - by0 * k
    contours = []
    for part in parts:
        segs = []
        for seg in part:
            if seg[0] == 'M':
                segs.append(('M', (ox + seg[1][0] * k, oy + seg[1][1] * k)))
            elif seg[0] == 'C':
                segs.append(('C',
                             (ox + seg[1][0] * k, oy + seg[1][1] * k),
                             (ox + seg[2][0] * k, oy + seg[2][1] * k),
                             (ox + seg[3][0] * k, oy + seg[3][1] * k)))
            else:
                segs.append(('Z',))
        contours.append(('bez', segs))
    cv.parity(color, contours)


MARK = DATA["mark"]["parts"]
MARK_BOX = tuple(DATA["mark"]["bounds"])
MARK_ASPECT = MARK_BOX[2] / MARK_BOX[3] if False else (MARK_BOX[2] - MARK_BOX[0]) / (MARK_BOX[3] - MARK_BOX[1])

CN = DATA["cn"]["parts"]
CN_BOX = _bbox_of(CN)
LABTXT = DATA["lab"]["parts"]
LAB_BOX = _bbox_of(LABTXT)


def draw_mark(cv, x, y, h, color):
    w = h * MARK_ASPECT
    _place(cv, color, MARK, MARK_BOX, x, y, w, h)
    return w


def draw_mark_lab(cv, x, y, h, color, ratio=0.19, letter="LAB", top_ratio=0.55):
    """h = 馄饨高。字顶压在 y + h*top_ratio，落在内腔里、避开上半的褶子。

    top_ratio 是字形顶线的位置，不是基线——所以放大字高时整行会往下长：
      · 三字版 cap = 0.19h，顶线 0.55 刚好，基线落在 161.6；
      · 单字母要 cap = 0.34h 才立得住，若还用 0.55 就会沉底（底边距内壁只剩 6，
        上方空 30）。改成 0.45，上下留白 16.5 / 19.1 —— A 上窄下宽，下方多留一点
        才看着居中。
    """
    w = draw_mark(cv, x, y, h, color)
    cap = h * ratio
    lw = wordmark_width(letter, cap)
    draw_wordmark(cv, letter, x + (w - lw) / 2, y + h * top_ratio, cap, color)
    return w


def draw_sub(cv, x, y, h, color, gap_ratio=0.22):
    """h = 中文字高；『lab』缩到 0.78 倍高，落在同一条基线上"""
    kw = h / (CN_BOX[3] - CN_BOX[1])
    _place(cv, color, CN, CN_BOX, x, y, (CN_BOX[2] - CN_BOX[0]) * kw, h)
    lh = h * 0.78
    kl = lh / (LAB_BOX[3] - LAB_BOX[1])
    lw = (LAB_BOX[2] - LAB_BOX[0]) * kl
    ly = y + h - lh
    lx = x + (CN_BOX[2] - CN_BOX[0]) * kw + h * gap_ratio
    _place(cv, color, LABTXT, LAB_BOX, lx, ly, lw, lh)
    return lx + lw - x


def sub_width(h, gap_ratio=0.22):
    kw = h / (CN_BOX[3] - CN_BOX[1])
    lh = h * 0.78
    kl = lh / (LAB_BOX[3] - LAB_BOX[1])
    return (CN_BOX[2] - CN_BOX[0]) * kw + h * gap_ratio + (LAB_BOX[2] - LAB_BOX[0]) * kl


# ---------------------------------------------------------------- 组合
def lockup(ink, red, mark_h=88, cap=40, gap=34, sub_h=26, sub_gap=14):
    mark_w = mark_h * MARK_ASPECT
    wm_w = wordmark_width("HUNDUN LAB", cap)
    text_w = max(wm_w, sub_width(sub_h))
    text_h = cap + sub_gap + sub_h
    block_h = max(mark_h, text_h)
    W = mark_w + gap + text_w
    H = block_h

    cv = Canvas(W, H)
    draw_mark(cv, 0, (block_h - mark_h) / 2, mark_h, red)
    ty = (block_h - text_h) / 2
    draw_wordmark(cv, "HUNDUN LAB", mark_w + gap, ty, cap, ink)
    draw_sub(cv, mark_w + gap + 2, ty + cap + sub_gap, sub_h, red)
    return cv


def mark(size, color=RED, with_lab=True, pad=0.09, ratio=0.19, letter="LAB", top_ratio=0.55):
    inner = size * (1 - pad * 2)
    h = inner / MARK_ASPECT
    w = h * MARK_ASPECT
    cv = Canvas(size, size)
    x, y = (size - w) / 2, (size - h) / 2
    if with_lab:
        draw_mark_lab(cv, x, y, h, color, ratio=ratio, letter=letter, top_ratio=top_ratio)
    else:
        draw_mark(cv, x, y, h, color)
    return cv


def wordmark(color=INK, cap=64):
    w = wordmark_width("HUNDUN LAB", cap)
    cv = Canvas(w, cap)
    draw_wordmark(cv, "HUNDUN LAB", 0, 0, cap, color)
    return cv


def favicon_cv(size=256, bg=PAPER, bold=1.0, pleats=True, pad=0.06):
    """方形容器里的图形标。

    bold < 1 时把内孔往中心收，环变粗；pleats=False 去掉褶子。
    16/24 px 下细环和褶子都会糊成一团红点，所以小尺寸走「粗环无褶」这一档。
    """
    cv = Canvas(size, size, bg=bg)
    bx0, by0, bx1, by1 = MARK_BOX
    k = size * (1 - pad * 2) / max(bx1 - bx0, by1 - by0)
    ox = (size - (bx1 - bx0) * k) / 2 - bx0 * k
    oy = (size - (by1 - by0) * k) / 2 - by0 * k
    cx, cy = (bx0 + bx1) / 2, (by0 + by1) / 2
    # parts[0] 外轮廓，parts[1] 线环内孔，parts[2:] 褶子
    keep = [MARK[0], MARK[1]] + (MARK[2:] if pleats else [])

    def warp(p, b):
        x, y = p
        return (ox + (cx + (x - cx) * b) * k, oy + (cy + (y - cy) * b) * k)

    contours = []
    for i, part in enumerate(keep):
        b = bold if i == 1 else 1.0
        segs = []
        for seg in part:
            if seg[0] == 'M':
                segs.append(('M', warp(seg[1], b)))
            elif seg[0] == 'C':
                segs.append(('C',) + tuple(warp(p, b) for p in seg[1:]))
            else:
                segs.append(('Z',))
        contours.append(('bez', segs))
    cv.parity(RED, contours)
    return cv


# 小尺寸专门档：粗环、去褶
FAVICON_STEPS = {
    16: dict(bold=0.64, pleats=False, pad=0.0),
    24: dict(bold=0.70, pleats=False, pad=0.01),
    32: dict(bold=0.86, pleats=True, pad=0.03),
}


LOCKUP = dict(mark_h=120, cap=40, gap=36, sub_h=26, sub_gap=12)

PNG_DIR = os.path.join(OUT, "png")


def main():
    made = []

    def save(name, cv):
        p = os.path.join(OUT, name)
        open(p, "w").write(cv.to_svg())
        made.append((name, os.path.getsize(p)))

    def png(name, cv, scale=1.0, bg=None):
        os.makedirs(PNG_DIR, exist_ok=True)
        p = os.path.join(PNG_DIR, name)
        cv.save_png(p, out_size=(int(round(cv.w * scale)), int(round(cv.h * scale))),
                    bg_override=bg)
        made.append((name, os.path.getsize(p)))
        return p

    save("hundun-lab-lockup.svg", lockup(INK, RED, **LOCKUP))
    save("hundun-lab-lockup-inverse.svg", lockup(PAPER, PAPER, **LOCKUP))
    save("hundun-lab-mark.svg", mark(256, RED, True, ratio=0.21))
    save("hundun-lab-mark-plain.svg", mark(256, RED, False))
    # 单字母版：只有 A。层高放到 0.34、顶线提到 0.45，理由见 draw_mark_lab
    save("hundun-lab-mark-a.svg", mark(256, RED, True, ratio=0.34, letter="A", top_ratio=0.45))
    save("hundun-lab-wordmark.svg", wordmark(INK))
    save("hundun-lab-wordmark-inverse.svg", wordmark(PAPER))
    save("hundun-lab-favicon.svg", favicon_cv(64))  # 浏览器标签页用，矢量自适应

    # 位图导出：透明底，按 2 倍/4 倍给 retina 用
    png("lockup@2x.png", lockup(INK, RED, **LOCKUP), 2.0)
    png("lockup-inverse@2x.png", lockup(PAPER, PAPER, **LOCKUP), 2.0)
    png("mark-512.png", mark(512, RED, True, ratio=0.21))
    png("mark-plain-512.png", mark(512, RED, False))
    png("mark-a-512.png", mark(512, RED, True, ratio=0.34, letter="A", top_ratio=0.45))
    png("wordmark@2x.png", wordmark(INK), 2.0)
    png("wordmark-inverse@2x.png", wordmark(PAPER), 2.0)

    # favicon：每个尺寸单独光栅化，小图标才不糊
    import io
    from ico import write_ico
    blobs = []
    for s in (16, 24, 32, 48, 64, 128, 256):
        cv = favicon_cv(s, **FAVICON_STEPS.get(s, {}))
        buf = io.BytesIO()
        cv.render(out_size=(s, s)).save(buf, format="PNG")
        blobs.append((s, buf.getvalue()))
        png(f"favicon-{s}.png", cv, 1.0)
    write_ico(os.path.join(OUT, "hundun-lab-favicon.ico"), blobs)
    made.append(("hundun-lab-favicon.ico", os.path.getsize(os.path.join(OUT, "hundun-lab-favicon.ico"))))

    for n, sz in made:
        print(f"{n:34s} {sz/1024:6.1f} KB")


if __name__ == "__main__":
    main()
