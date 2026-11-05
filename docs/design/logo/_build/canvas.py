"""自建画布：同一份几何同时输出 SVG 和 PNG。

ImageMagick 内置的 SVG 渲染器对重叠子路径的填充规则是错的（同向子路径重叠会被挖空），
拿它做预览等于自欺。所以这里用 PIL 自己做光栅化，4 倍超采样后降采样，
保证预览 PNG 与浏览器里的 SVG 一致。

两类形状：
  union  —— 同色多边形叠加求并集（描边转出来的字母，nonzero 语义）
  parity —— 奇偶规则（描出来的馄饨：外轮廓 + 内孔 + 褶子）

轮廓可以是折线 [(x,y), ...]，也可以是 ('bez', [('M',p), ('C',c1,c2,p), ('Z',)])
"""

from PIL import Image, ImageDraw, ImageChops

SS = 4


def _fmt(v, precision=1):
    s = f"{v:.{precision}f}".rstrip('0').rstrip('.')
    return '0' if s in ('-0', '') else s


class Canvas:
    def __init__(self, w, h, bg=None):
        self.w, self.h = w, h
        self.bg = bg
        self.items = []          # (kind, fill, [contour, ...])

    # ---------------------------------------------------------------- 录入
    def union(self, fill, polys):
        """同色多边形求并集。统一绕向，否则浏览器里的 nonzero 会把重叠区挖空。"""
        norm = []
        for p in polys:
            p = [(float(x), float(y)) for x, y in p]
            s = 0.0
            for i in range(len(p)):
                x1, y1 = p[i]
                x2, y2 = p[(i + 1) % len(p)]
                s += x1 * y2 - x2 * y1
            norm.append(p if s >= 0 else p[::-1])
        self.items.append(('union', fill, norm))

    def parity(self, fill, contours):
        self.items.append(('parity', fill, list(contours)))

    @staticmethod
    def _is_bez(c):
        return isinstance(c, tuple) and len(c) == 2 and c[0] == 'bez'

    # ---------------------------------------------------------------- 展开
    @staticmethod
    def _flatten(segs, steps=16):
        pts, cur = [], None
        for seg in segs:
            if seg[0] == 'M':
                if cur and len(cur) > 2:
                    pts.append(cur)
                cur = [seg[1]]
            elif seg[0] == 'C':
                _, c1, c2, p = seg
                p0 = cur[-1]
                for i in range(1, steps + 1):
                    t = i / steps
                    mt = 1 - t
                    cur.append((
                        mt ** 3 * p0[0] + 3 * mt * mt * t * c1[0] + 3 * mt * t * t * c2[0] + t ** 3 * p[0],
                        mt ** 3 * p0[1] + 3 * mt * mt * t * c1[1] + 3 * mt * t * t * c2[1] + t ** 3 * p[1],
                    ))
            elif seg[0] == 'Z':
                if cur and len(cur) > 2:
                    pts.append(cur)
                cur = None
        if cur and len(cur) > 2:
            pts.append(cur)
        return pts

    def _raster(self, c):
        return self._flatten(c[1]) if self._is_bez(c) else [c]

    # ---------------------------------------------------------------- SVG
    def to_svg(self, view=None, precision=1):
        vx, vy, vw, vh = view or (0, 0, self.w, self.h)
        out = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="'
               f'{_fmt(vx, precision)} {_fmt(vy, precision)} {_fmt(vw, precision)} {_fmt(vh, precision)}">']
        if self.bg:
            out.append(f'<rect x="{_fmt(vx, precision)}" y="{_fmt(vy, precision)}" '
                       f'width="{_fmt(vw, precision)}" height="{_fmt(vh, precision)}" fill="{self.bg}"/>')
        for kind, fill, data in self.items:
            d = []
            for c in data:
                if self._is_bez(c):
                    for seg in c[1]:
                        if seg[0] == 'M':
                            d.append(f"M{_fmt(seg[1][0], precision)} {_fmt(seg[1][1], precision)}")
                        elif seg[0] == 'C':
                            _, a, b, p = seg
                            d.append(f"C{_fmt(a[0], precision)} {_fmt(a[1], precision)} "
                                     f"{_fmt(b[0], precision)} {_fmt(b[1], precision)} "
                                     f"{_fmt(p[0], precision)} {_fmt(p[1], precision)}")
                        else:
                            d.append('Z')
                else:
                    pts = [f"M{_fmt(c[0][0], precision)} {_fmt(c[0][1], precision)}"]
                    pts += [f"L{_fmt(x, precision)} {_fmt(y, precision)}" for x, y in c[1:]]
                    d.append("".join(pts) + "Z")
            rule = 'evenodd' if kind == 'parity' else 'nonzero'
            out.append(f'<path fill-rule="{rule}" fill="{fill}" d="{" ".join(d)}"/>')
        out.append('</svg>')
        return "\n".join(out)

    # ---------------------------------------------------------------- PNG
    def render(self, view=None, out_size=None, bg_override=None):
        vx, vy, vw, vh = view or (0, 0, self.w, self.h)
        W, H = out_size or (int(round(vw)), int(round(vh)))
        bg = bg_override or self.bg
        # 没有底色就出透明通道，导出的 PNG 才能直接压在任意背景上
        mode = "RGBA" if bg is None else "RGB"
        img = Image.new(mode, (W * SS, H * SS), bg or (0, 0, 0, 0))
        dr = ImageDraw.Draw(img)
        kx, ky = W * SS / vw, H * SS / vh

        def proj(pts):
            return [((x - vx) * kx, (y - vy) * ky) for x, y in pts]

        for kind, fill, data in self.items:
            if kind == 'union':
                for poly in data:
                    if len(poly) >= 3:
                        dr.polygon(proj(poly), fill=fill)
            else:
                acc = Image.new("1", img.size, 0)
                for c in data:
                    for poly in self._raster(c):
                        if len(poly) >= 3:
                            tmp = Image.new("1", img.size, 0)
                            ImageDraw.Draw(tmp).polygon(proj(poly), fill=1)
                            acc = ImageChops.logical_xor(acc, tmp)
                img.paste(fill, mask=acc)
        return img.resize((W, H), Image.LANCZOS)

    def save_png(self, path, view=None, out_size=None, bg_override=None):
        self.render(view, out_size, bg_override).save(path)
        return path
