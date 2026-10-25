#!/usr/bin/env python3
"""
下载设计稿里的 19 张菜谱照片，按设计槽位语义命名后放进 drawable-nodpi/。

为什么走 wsrv.nl 而不是直连：
  图片源是 lh3.googleusercontent.com，本机直连返回 000（Clash 代理规则未覆盖该域名），
  但公共图片代理 https://wsrv.nl/ 可达。加 `&w=1024` 取 1024px 宽的原图。

为什么不用截图裁切：
  截图里照片带着烙进去的角标、收藏心形和渐变遮罩，且菜谱库那张顶部 194px 根本没渲染。
  取原图则干净得多。

用法：python -I tools/fetch_photos.py
"""

from __future__ import annotations

import io
import json
import os
import sys
import urllib.parse
import urllib.request

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, "tools", ".cache")
URLS_JSON = os.path.join(CACHE, "image_urls.json")
OUT_DIR = os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi")

PROXY = "https://wsrv.nl/?url={}&w=1024&output=jpg&q=82"

# alt 文本 → 输出文件名。名字与 assets/seed/recipes.json 里的
# `drawable:` 引用以及各界面取图的槽位一一对应。
NAME_BY_ALT = {
    "香煎迷迭香脆皮三文鱼排": "recipe_salmon_hero",
    "腌制三文鱼": "step_salmon_marinate",
    "封边添香装盘": "step_salmon_plate",
    "法式普罗旺斯慢炖番茄牛腩": "recipe_beef_stew",
    "香煎迷迭香三文鱼排": "recipe_salmon_card",
    "酥嫩日式唐扬鸡块": "recipe_karaage",
    "牛油果溏心蛋酸种吐司": "recipe_avocado_toast",
    "松露燕麦低卡菌菇浓汤": "recipe_mushroom_soup",
    "香煎迷迭香法式羊排": "recipe_lamb_chops",
    "慢炖勃艮第红酒牛肉": "recipe_burgundy",
    "黑松露野菌手打意面": "recipe_truffle_pasta",
    "清甜马蹄竹蔗牛腩煲": "recipe_brisket_pot",
    "香脆油封鸭腿": "recipe_duck_confit",
    "焦糖肉桂法式吐司封面": "recipe_french_toast",
    "Step 1 preview": "step_toast_1",
    "Step 2 preview": "step_toast_2",
    "App Logo": "brand_logo",
    "旬食": "brand_logo",
}

# search_tags 的两张背景图没有 alt，按界面里的出现顺序对应这两道菜
BG_FALLBACK_NAMES = ["recipe_spring_soup", "recipe_asparagus_salad"]


def fetch(url: str) -> bytes:
    proxied = PROXY.format(urllib.parse.quote(url, safe=""))
    req = urllib.request.Request(proxied, headers={"User-Agent": "curl/8"})
    with urllib.request.urlopen(req, timeout=120) as resp:
        return resp.read()


def verify_and_save(data: bytes, path: str, label: str) -> tuple[int, int]:
    """
    确认拿到的确实是一张真实照片。

    这里用**局部**方差（相邻像素差）而不是全局方差来判断。这条经验是有教训的：
    设计稿里 `香煎芦笋罗勒暖沙拉` 与 `腌笃鲜` 两张图的原始 URL 返回的是平滑的
    青绿渐变，它的全局标准差能轻松通过「非纯色」检查，但相邻像素差接近零 ——
    一看就不是照片。真照片无论怎么虚化，都必然有边缘。
    """
    img = Image.open(io.BytesIO(data))
    img.load()
    w, h = img.size
    if w < 200 or h < 150:
        raise RuntimeError(f"{label}: 尺寸过小 {w}x{h}，可能是错误页")

    grey = img.convert("L").resize((160, 160))
    px = list(grey.getdata())

    # 水平与垂直方向的一阶差分 → 边缘能量
    diffs = []
    for y in range(160):
        row = y * 160
        for x in range(159):
            diffs.append(abs(px[row + x + 1] - px[row + x]))
    for y in range(159):
        row = y * 160
        for x in range(160):
            diffs.append(abs(px[row + 160 + x] - px[row + x]))
    edge_energy = sum(diffs) / len(diffs)

    if edge_energy < 1.5:
        raise RuntimeError(
            f"{label}: 边缘能量仅 {edge_energy:.2f}，是平滑渐变而非照片（原始 URL 素材已损坏）"
        )

    img.convert("RGB").save(path, "JPEG", quality=88, optimize=True)
    return w, h


def main() -> int:
    if not os.path.isfile(URLS_JSON):
        print(f"缺少 {URLS_JSON}，请先运行 tools/extract_image_urls.py", file=sys.stderr)
        return 1

    table = json.load(open(URLS_JSON, encoding="utf-8"))
    os.makedirs(OUT_DIR, exist_ok=True)

    # 按界面顺序排出稳定次序，背景图按此顺序套用 BG_FALLBACK_NAMES
    entries = sorted(table.items(), key=lambda kv: (kv[1]["screens"], str(kv[1]["alt"])))
    used_names: set[str] = set()
    bg_index = 0
    ok = failed = 0

    for url, meta in entries:
        alt = (meta["alt"] or [""])[0]

        if meta["bg"]:
            name = BG_FALLBACK_NAMES[bg_index] if bg_index < len(BG_FALLBACK_NAMES) else None
            bg_index += 1
            if name is None:
                print(f"  跳过多余背景图 {url[:60]}…")
                continue
        elif alt in NAME_BY_ALT:
            name = NAME_BY_ALT[alt]
        else:
            print(f"  !! 没有命名规则: alt={alt!r}")
            failed += 1
            continue

        if name in used_names:
            continue
        used_names.add(name)

        path = os.path.join(OUT_DIR, f"{name}.jpg")
        try:
            data = fetch(url)
            w, h = verify_and_save(data, path, name)
            size_kb = os.path.getsize(path) / 1024
            print(f"  {name:28s} {w}x{h}  {size_kb:6.0f} KB")
            ok += 1
        except Exception as exc:  # noqa: BLE001
            print(f"  {name:28s} 失败: {exc}", file=sys.stderr)
            failed += 1

    print(f"\n完成：成功 {ok} 张，失败 {failed} 张 → {OUT_DIR}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
