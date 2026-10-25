#!/usr/bin/env python3
"""
从 7 份设计稿 HTML 里提取全部图片 URL 及其归属界面。

设计稿的图片都指向 lh3.googleusercontent.com，本机直连不可达（代理规则未覆盖），
但经 wsrv.nl 代理可取原图。先把 URL 和它们的「谁是谁」整理出来，
供 fetch_photos.py 按语义命名下载。

用法：python -I tools/extract_image_urls.py
"""

from __future__ import annotations

import glob
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "tools", ".cache", "image_urls.json")

IMG_TAG = re.compile(r"<img[^>]*>", re.I)
SRC_ATTR = re.compile(r'src="(https://lh3[^"]+)"')
ALT_ATTR = re.compile(r'alt="([^"]*)"')
DATA_ALT_ATTR = re.compile(r'data-alt="([^"]*)"')
BG_URL = re.compile(r"background-image:\s*url\([\"']?(https://lh3[^\"')]+)[\"']?\)")


def main() -> int:
    found: dict[str, dict] = {}

    for path in sorted(glob.glob(os.path.join(ROOT, "reference", "*", "code.html"))):
        screen = os.path.basename(os.path.dirname(path))
        html = open(path, encoding="utf-8").read()

        def record(url: str, is_bg: bool, alt: str = "", data_alt: str = "") -> None:
            entry = found.setdefault(url, {"screens": set(), "alt": set(), "dataAlt": set(), "bg": False})
            entry["screens"].add(screen)
            entry["bg"] = entry["bg"] or is_bg
            if alt:
                entry["alt"].add(alt)
            if data_alt:
                entry["dataAlt"].add(data_alt)

        for tag in IMG_TAG.findall(html):
            src = SRC_ATTR.search(tag)
            if not src:
                continue
            alt = ALT_ATTR.search(tag)
            data_alt = DATA_ALT_ATTR.search(tag)
            record(
                src.group(1),
                is_bg=False,
                alt=alt.group(1) if alt else "",
                data_alt=data_alt.group(1) if data_alt else "",
            )

        for m in BG_URL.finditer(html):
            record(m.group(1), is_bg=True)

    # set 不能直接 JSON 序列化，转成排序后的 list
    serializable = {
        url: {
            "screens": sorted(v["screens"]),
            "alt": sorted(v["alt"]),
            "dataAlt": sorted(v["dataAlt"]),
            "bg": v["bg"],
        }
        for url, v in found.items()
    }

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(serializable, fh, ensure_ascii=False, indent=1)

    print(f"唯一图片 URL: {len(serializable)} 个 → {OUT}\n")
    for i, (url, v) in enumerate(sorted(serializable.items(), key=lambda kv: (kv[1]["screens"], kv[1]["alt"])), 1):
        screens = ",".join(v["screens"])
        alt = " | ".join(v["alt"]) or "(无 alt)"
        kind = "背景图" if v["bg"] else "img"
        print(f"{i:2d}. [{screens:20s}] {kind:4s} {alt}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
