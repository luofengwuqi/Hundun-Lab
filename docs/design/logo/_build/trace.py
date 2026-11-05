"""极简等值线追踪 + 平滑，把位图轮廓变成可用的 SVG 路径。

没有 potrace / cv2 / skimage 可用，所以自己写：
1. marching squares 在浮点场均上找闭合等值线（亚像素精度）
2. RDP 抽稀
3. Catmull-Rom -> 三次贝塞尔，输出平滑闭合路径
4. 用嵌套深度判定孔洞方向，输出 nonzero 可用的子路径
"""

import math

# ---------------------------------------------------------------- marching squares

def _lerp(a, b, va, vb, t):
    d = vb - va
    if abs(d) < 1e-12:
        return 0.5
    return max(0.0, min(1.0, (t - va) / d))


def marching_squares(field, threshold):
    """field: 二维 list[list[float]]。返回 [[(x,y), ...], ...] 闭合折线。"""
    rows = len(field)
    cols = len(field[0])
    segs = []  # (edge_id_a, edge_id_b)

    def H(i, j):  # 水平边：连接 (i,j)-(i,j+1)
        return ('h', i, j)

    def V(i, j):  # 垂直边：连接 (i,j)-(i+1,j)
        return ('v', i, j)

    for i in range(rows - 1):
        for j in range(cols - 1):
            tl = field[i][j]
            tr = field[i][j + 1]
            br = field[i + 1][j + 1]
            bl = field[i + 1][j]
            case = (8 if tl > threshold else 0) | (4 if tr > threshold else 0) | \
                   (2 if br > threshold else 0) | (1 if bl > threshold else 0)
            if case in (0, 15):
                continue
            top, right = H(i, j), V(i, j + 1)
            bottom, left = H(i + 1, j), V(i, j)

            pairs = {
                1: [(left, bottom)], 2: [(bottom, right)], 3: [(left, right)],
                4: [(top, right)], 5: [(left, top), (bottom, right)],
                6: [(top, bottom)], 7: [(left, top)],
                8: [(left, top)], 9: [(top, bottom)],
                10: [(left, top), (bottom, right)],
                11: [(top, right)], 12: [(left, right)],
                13: [(bottom, right)], 14: [(left, bottom)],
            }[case]
            segs.extend(pairs)

    # 边 id -> 坐标
    def coord(e):
        kind, i, j = e
        if kind == 'h':
            va, vb = field[i][j], field[i][j + 1]
            return (j + _lerp(j, j + 1, va, vb, threshold), float(i))
        va, vb = field[i][j], field[i + 1][j]
        return (float(j), i + _lerp(i, i + 1, va, vb, threshold))

    # 连接成环：每个线段是图的一条边，节点度数为 2
    adj = {}
    for idx, (a, b) in enumerate(segs):
        adj.setdefault(a, []).append(idx)
        adj.setdefault(b, []).append(idx)

    used = [False] * len(segs)
    loops = []
    for start in range(len(segs)):
        if used[start]:
            continue
        used[start] = True
        a, b = segs[start]
        nodes = [a, b]
        cur_node, prev_edge = b, start
        guard = 0
        while guard <= len(segs) + 4:
            guard += 1
            nxt = None
            for ei in adj.get(cur_node, ()):
                if ei != prev_edge and not used[ei]:
                    nxt = ei
                    break
            if nxt is None:
                break
            used[nxt] = True
            p, q = segs[nxt]
            nxt_node = q if p == cur_node else p
            prev_edge, cur_node = nxt, nxt_node
            if nxt_node == nodes[0]:
                break
            nodes.append(nxt_node)
        if len(nodes) > 2:
            loops.append([coord(e) for e in nodes])

    return loops


def rdp(points, eps):
    if len(points) < 3:
        return points[:]
    # 闭合折线：先找最远两点作为锚
    x0, y0 = points[0]
    far, fi = -1, 0
    for idx, (x, y) in enumerate(points):
        d = (x - x0) ** 2 + (y - y0) ** 2
        if d > far:
            far, fi = d, idx
    a = points[:fi + 1]
    b = points[fi:] + [points[0]]
    out = _rdp_open(a, eps)[:-1] + _rdp_open(b, eps)[:-1]
    return out if len(out) > 2 else points[:]


def _rdp_open(pts, eps):
    if len(pts) < 3:
        return pts[:]
    (x1, y1), (x2, y2) = pts[0], pts[-1]
    idx, dmax = 0, -1.0
    for i in range(1, len(pts) - 1):
        x, y = pts[i]
        dx, dy = x2 - x1, y2 - y1
        n = math.hypot(dx, dy)
        d = abs(dy * (x - x1) - dx * (y - y1)) / n if n > 1e-9 else math.hypot(x - x1, y - y1)
        if d > dmax:
            dmax, idx = d, i
    if dmax > eps:
        left = _rdp_open(pts[:idx + 1], eps)
        right = _rdp_open(pts[idx:], eps)
        return left[:-1] + right
    return [pts[0], pts[-1]]


# ---------------------------------------------------------------- 平滑 + 输出

def catmull_to_bezier(pts, tension=1.0, closed=True):
    """返回 [('M',p) | ('C',c1,c2,p), ...]"""
    n = len(pts)
    out = [('M', pts[0])]
    rng = range(n) if closed else range(n - 1)
    for i in rng:
        p0 = pts[(i - 1) % n] if closed else pts[max(i - 1, 0)]
        p1 = pts[i % n] if closed else pts[i]
        p2 = pts[(i + 1) % n] if closed else pts[i + 1]
        p3 = pts[(i + 2) % n] if closed else pts[min(i + 2, n - 1)]
        c1 = (p1[0] + (p2[0] - p0[0]) / 6.0 * tension,
              p1[1] + (p2[1] - p0[1]) / 6.0 * tension)
        c2 = (p2[0] - (p3[0] - p1[0]) / 6.0 * tension,
              p2[1] - (p3[1] - p1[1]) / 6.0 * tension)
        out.append(('C', c1, c2, p2))
    if closed:
        out.append(('Z',))
    return out


def signed_area(pts):
    s = 0.0
    for i in range(len(pts)):
        x1, y1 = pts[i]
        x2, y2 = pts[(i + 1) % len(pts)]
        s += x1 * y2 - x2 * y1
    return s / 2.0


def point_in_poly(pt, poly):
    x, y = pt
    inside = False
    n = len(poly)
    for i in range(n):
        x1, y1 = poly[i]
        x2, y2 = poly[(i + 1) % n]
        if (y1 > y) != (y2 > y):
            xi = x1 + (y - y1) * (x2 - x1) / (y2 - y1)
            if x < xi:
                inside = not inside
    return inside


def build_path(loops, transform, eps=1.0, tension=1.0, min_area=4.0):
    """loops 是像素坐标的闭合折线；transform 把像素坐标映射到目标坐标。"""
    polys = []
    for lp in loops:
        lp = rdp(lp, eps)
        if len(lp) < 3 or abs(signed_area(lp)) < min_area:
            continue
        polys.append(lp)

    # 每个环的嵌套深度：深度是奇数 => 孔洞
    depths = []
    for i, lp in enumerate(polys):
        probe = lp[0]
        d = sum(1 for j, other in enumerate(polys) if j != i and point_in_poly(probe, other))
        depths.append(d)

    parts = []
    for lp, d in zip(polys, depths):
        area = signed_area(lp)
        want_positive = (d % 2 == 0)  # 外轮廓统一逆时针(area>0)，孔洞顺时针
        if (area > 0) != want_positive:
            lp = lp[::-1]
        pts = [transform(x, y) for x, y in lp]
        parts.append(catmull_to_bezier(pts, tension=tension))
    return parts


def parts_to_d(parts, prec=2):
    def f(v):
        s = f"{v:.{prec}f}".rstrip('0').rstrip('.')
        return s if s not in ('-0', '') else '0'
    chunks = []
    for part in parts:
        buf = []
        for seg in part:
            if seg[0] == 'M':
                buf.append(f"M{f(seg[1][0])} {f(seg[1][1])}")
            elif seg[0] == 'C':
                _, c1, c2, p = seg
                buf.append(f"C{f(c1[0])} {f(c1[1])} {f(c2[0])} {f(c2[1])} {f(p[0])} {f(p[1])}")
            else:
                buf.append('Z')
        chunks.append(''.join(buf))
    return ' '.join(chunks)
