"""把位图素材（家族馄饨图标、毛笔字）描成可复用的矢量几何，缓存到 assets.json。

没有 potrace / cv2，所以用 trace.py 里的 marching squares + RDP + Catmull-Rom。
跑一次就够，产物是纯路径数据。
"""

import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from PIL import Image, ImageFilter, ImageDraw, ImageFont

from trace import marching_squares, build_path, signed_area

HERE = os.path.dirname(os.path.abspath(__file__))
ICON = "/home/wuqi/code/hundunDesktop/build/icons/256x256.png"
FONT = os.path.join(HERE, "fonts", "MaShanZheng-Regular.ttf")


def _loops_from_mask(mask, blur=1.4, threshold=127.5):
    mask = mask.filter(ImageFilter.GaussianBlur(blur))
    w, h = mask.size
    px = list(mask.getdata())
    field = [px[i * w:(i + 1) * w] for i in range(h)]
    return marching_squares(field, threshold)


def _to_bez(loops, eps, tension, min_area, xf, keep=99):
    loops = sorted(loops, key=lambda l: -abs(signed_area(l)))[:keep]
    parts = build_path(loops, xf, eps=eps, tension=tension, min_area=min_area)
    return [p for p in parts]


def extract_mark():
    """家族馄饨轮廓（来自 HundunDesktop 图标），归一到宽 1000、原点居中"""
    im = Image.open(ICON).convert("RGBA")
    bg = Image.new("RGBA", im.size, (255, 255, 255, 255))
    bg.alpha_composite(im)
    rgb = bg.convert("RGB")
    w, h = rgb.size
    r, g, b = rgb.split()
    R, G, B = r.load(), g.load(), b.load()
    mask = Image.new("L", (w, h))
    mask.putdata([255 if (R[x, y] - G[x, y] > 60 and R[x, y] - B[x, y] > 40 and R[x, y] > 110) else 0
                  for y in range(h) for x in range(w)])
    # 原图标线条偏细，和重字标放一起会失衡；先做形态学膨胀把线加粗
    mask = mask.filter(ImageFilter.MaxFilter(5))
    loops = _loops_from_mask(mask, blur=1.4)
    xs = [p[0] for l in loops for p in l]
    ys = [p[1] for l in loops for p in l]
    x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
    s = 1000.0 / (x1 - x0)
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    parts = _to_bez(loops, eps=1.25, tension=1.0, min_area=6,
                    xf=lambda x, y: ((x - cx) * s, (y - cy) * s))
    return {
        "parts": parts,
        "bounds": [-500, -307.7, 500, 307.7],
        "aspect": (x1 - x0) / (y1 - y0),
    }


def extract_text(text, px=340, blur=0.9, eps=0.7):
    font = ImageFont.truetype(FONT, px)
    pad = 60
    im = Image.new("L", (px * max(len(text), 2) + pad * 2, px * 2 + pad * 2), 255)
    ImageDraw.Draw(im).text((pad, pad), text, font=font, fill=0)
    mask = Image.new("L", im.size)
    mask.putdata([255 if v < 140 else 0 for v in im.getdata()])
    bbox = mask.getbbox()
    if not bbox:
        raise SystemExit(f"字体里没有 {text!r} 的字形")
    x0, y0, x1, y1 = bbox
    mask = mask.crop((max(0, x0 - 2), max(0, y0 - 2), x1 + 2, y1 + 2))
    loops = _loops_from_mask(mask, blur=blur)
    w, h = mask.size
    parts = _to_bez(loops, eps=eps, tension=1.0, min_area=2.5, xf=lambda x, y: (x, y))
    return {"parts": parts, "size": [w, h]}


def main():
    data = {"mark": extract_mark()}
    for key, text in (("cn", "馄饨"), ("lab", "lab")):
        data[key] = extract_text(text)
    out = os.path.join(HERE, "assets.json")
    json.dump(data, open(out, "w"))
    for k, v in data.items():
        n = sum(len(p) for p in v["parts"])
        print(f"{k:5s} 子路径 {len(v['parts']):3d}  段 {n:4d}")
    print("->", out, f"{os.path.getsize(out)/1024:.0f} KB")


if __name__ == "__main__":
    main()
