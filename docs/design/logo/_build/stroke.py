"""把折线描边转成填充多边形。

ImageMagick 自带的 SVG 渲染器不支持 stroke，只认 fill；而且 logo 用填充轮廓
本身就比依赖 stroke-width 更稳（缩放、栅格化、转 ico 都不会跑形）。
"""

import math


def _sub(a, b):
    return (a[0] - b[0], a[1] - b[1])


def _add(a, b):
    return (a[0] + b[0], a[1] + b[1])


def _mul(a, k):
    return (a[0] * k, a[1] * k)


def _norm(a):
    n = math.hypot(a[0], a[1])
    return (a[0] / n, a[1] / n) if n > 1e-12 else (0.0, 0.0)


def _left_normal(d):
    return (-d[1], d[0])


def stroke_to_polys(polylines, width, miter_limit=4.0, caps='butt', closed=False):
    """polylines: [[(x,y), ...], ...] -> [[(x,y), ...], ...] 填充多边形列表"""
    h = width / 2.0
    polys = []
    for pts in polylines:
        if len(pts) < 2:
            continue
        if closed and pts[0] != pts[-1]:
            pts = pts + [pts[0]]
        segs = []
        for i in range(len(pts) - 1):
            d = _norm(_sub(pts[i + 1], pts[i]))
            if d == (0.0, 0.0):
                continue
            segs.append((pts[i], pts[i + 1], d))
        if not segs:
            continue

        # 每段一个矩形
        for p, q, d in segs:
            n = _mul(_left_normal(d), h)
            polys.append([_add(p, n), _add(q, n), _sub(q, n), _sub(p, n)])

        # 每个内顶点补两块斜接楔形（左右各一）
        for i in range(len(segs) - 1):
            p1, q1, d1 = segs[i]
            p2, q2, d2 = segs[i + 1]
            v = q1
            cross = d1[0] * d2[1] - d1[1] * d2[0]
            if abs(cross) < 1e-9:
                continue
            n1, n2 = _left_normal(d1), _left_normal(d2)
            dot = n1[0] * n2[0] + n1[1] * n2[1]
            denom = 1.0 + dot
            # 屏幕坐标（y 向下）下：cross < 0 时凸侧在左法线那一侧
            side = 1.0 if cross < 0 else -1.0
            if denom < 1e-6:
                m = _mul(n1, side)
            else:
                m = _mul(_add(n1, n2), side * h / denom)
                if math.hypot(*m) > miter_limit * h:
                    m = _mul(_norm(m), miter_limit * h)
            # 必须含顶点 v 本身，否则 90° 转角处每边缺一个方块
            polys.append([v,
                          _add(v, _mul(n1, side * h)),
                          _add(v, m),
                          _add(v, _mul(n2, side * h))])

        # 端头
        if caps == 'square' and not closed:
            for idx in (0, -1):
                p, q, d = segs[idx] if idx == 0 else segs[-1]
                if idx == 0:
                    end = p
                    d = _mul(d, -1)
                else:
                    end = segs[-1][1]
                    d = segs[-1][2]
                n = _mul(_left_normal(d), h)
                polys.append([end, _add(end, n), _add(_add(end, n), d and _mul(d, h)),
                              _add(_sub(end, n), _mul(d, h)), _sub(end, n)])

    # 统一绕向为逆时针(正面积)，避免 nonzero 下互相抵消
    out = []
    for poly in polys:
        s = 0.0
        for i in range(len(poly)):
            x1, y1 = poly[i]
            x2, y2 = poly[(i + 1) % len(poly)]
            s += x1 * y2 - x2 * y1
        out.append(poly if s >= 0 else poly[::-1])
    return out


def polys_to_d(polys, prec=2):
    def f(v):
        s = f"{v:.{prec}f}".rstrip('0').rstrip('.')
        return '0' if s in ('-0', '') else s
    chunks = []
    for poly in polys:
        pts = [f"L{f(x)} {f(y)}" for x, y in poly]
        chunks.append("M" + pts[0][1:] + " " + " ".join(pts[1:]) + " Z")
    return " ".join(chunks)
