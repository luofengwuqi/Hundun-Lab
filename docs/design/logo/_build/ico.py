"""把若干 PNG 尺寸打包成 .ico。

PIL 的 ICO 保存只能从单张图缩，小尺寸会糊；这里每个尺寸都由矢量单独光栅化，
直接把 PNG 负载写进 ICO 目录（Vista 之后浏览器、Windows 资源管理器都认）。
"""

import struct


def write_ico(path, pngs):
    """pngs: [(size, png_bytes), ...]，按尺寸升序"""
    pngs = sorted(pngs, key=lambda t: t[0])
    count = len(pngs)
    header = struct.pack("<HHH", 0, 1, count)
    offset = 6 + 16 * count
    entries, blobs = [], []
    for size, data in pngs:
        dim = 0 if size >= 256 else size
        entries.append(struct.pack("<BBBBHHII", dim, dim, 0, 0, 1, 32, len(data), offset))
        blobs.append(data)
        offset += len(data)
    with open(path, "wb") as f:
        f.write(header)
        for e in entries:
            f.write(e)
        for b in blobs:
            f.write(b)
