#!/usr/bin/env python3
"""
阶段 1 资源抓取：字体 TTF + Material Symbols 码点表。

背景：本机 dl.google.com / github.com / fonts.gstatic.com / lh3.googleusercontent.com
均不可达（Clash 代理规则未覆盖），但 ghfast.top 这个 GitHub 代理可用。

产出：
  tools/.cache/          原始下载（不提交）
  app/src/main/res/font/ manrope.ttf · newsreader.ttf · newsreader_italic.ttf
                         （图标字体由 build_icon_font.py 生成）
  tools/ezmenu_glyphs.txt  设计稿用到的全部 Material Symbols 字形名

用法：python -I tools/fetch_assets.py
"""

from __future__ import annotations

import os
import sys
import urllib.request

PROXY = "https://ghfast.top/"
GH = "https://raw.githubusercontent.com/"

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, "tools", ".cache")
FONT_DIR = os.path.join(ROOT, "app", "src", "main", "res", "font")

# --- 正文字体：来自 google/fonts ---
TEXT_FONTS = {
    "Manrope[wght].ttf": ("google/fonts/main/ofl/manrope/Manrope%5Bwght%5D.ttf", "manrope.ttf"),
    "Newsreader[opsz,wght].ttf": (
        "google/fonts/main/ofl/newsreader/Newsreader%5Bopsz%2Cwght%5D.ttf",
        "newsreader.ttf",
    ),
    "Newsreader-Italic[opsz,wght].ttf": (
        "google/fonts/main/ofl/newsreader/Newsreader-Italic%5Bopsz%2Cwght%5D.ttf",
        "newsreader_italic.ttf",
    ),
}

# --- 图标字体：Material Symbols 可变字体 + 官方码点表 ---
ICON_FONT_VAR = "google/material-design-icons/master/variablefont/MaterialSymbolsOutlined%5BFILL%2CGRAD%2Copsz%2Cwght%5D.ttf"
ICON_CODEPOINTS = "google/material-design-icons/master/variablefont/MaterialSymbolsOutlined%5BFILL%2CGRAD%2Copsz%2Cwght%5D.codepoints"

# --- 中文衬线：Noto Serif SC 可变字体 ---
# Newsreader/Manrope 都是纯拉丁字体，中文会回退到系统黑体。为了让中文标题也有
# DESIGN.md 要求的衬线气质，需要把 Noto Serif SC 与 Newsreader 合并成一个字体
# （见 tools/build_cjk_serif.py 里关于「FontFamily 不是回退链」的说明）。
# 原始文件 25MB，只作为中间产物，不进 APK。
CJK_FONT_VAR = "google/fonts/main/ofl/notoserifsc/NotoSerifSC%5Bwght%5D.ttf"

# 设计稿 7 个界面用到的全部字形（去重后 59 个）
GLYPHS = [
    "add", "add_photo_alternate", "arrow_back", "arrow_downward", "arrow_forward",
    "arrow_upward", "auto_awesome", "bookmark", "bookmark_border", "bookmark_heart",
    "cancel", "change_circle", "check", "check_circle", "chevron_right", "close",
    "content_copy", "cooking", "delete", "download_for_offline", "drag_indicator",
    "drafts", "explore", "favorite", "favorite_border", "front_hand", "grid_view",
    "group", "history_edu", "hourglass_top", "info", "kitchen", "light_mode",
    "lightbulb", "local_fire_department", "magic_button", "menu_book", "mic",
    "notifications", "pause", "person", "person_outline", "photo_camera",
    "play_arrow", "play_circle", "remove", "replay", "restart_alt", "scale",
    "schedule", "search", "share", "skillet", "soup_kitchen", "spa", "timer",
    "tips_and_updates", "tune", "wb_sunny",
]


def fetch(rel_path: str, timeout: int = 120) -> bytes:
    """先试 ghfast 代理，再试直连（代理开启时可能直连也行）。"""
    errors = []
    for url in (PROXY + GH + rel_path, GH + rel_path):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "curl/8"})
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                return resp.read()
        except Exception as exc:  # noqa: BLE001
            errors.append(f"{url} -> {exc}")
    raise RuntimeError("全部下载途径失败:\n  " + "\n  ".join(errors))


def check_ttf(data: bytes, name: str) -> None:
    """校验 sfnt magic。TTF 是 0x00010000，OTF/CFF 是 'OTTO'，ttcf 是字体集合。"""
    if len(data) < 12:
        raise RuntimeError(f"{name}: 文件过小（{len(data)} 字节）")
    magic = data[:4]
    ok = magic in (b"\x00\x01\x00\x00", b"OTTO", b"true", b"ttcf")
    if not ok:
        head = data[:120].decode("utf-8", "replace").replace("\n", " ")
        raise RuntimeError(f"{name}: 不是字体文件，magic={magic!r}\n  开头内容: {head}")
    print(f"    magic={magic!r} 大小={len(data):,} 字节  OK")


def main() -> int:
    os.makedirs(CACHE, exist_ok=True)
    os.makedirs(FONT_DIR, exist_ok=True)

    print("== 正文字体 ==")
    for src_name, (rel, out_name) in TEXT_FONTS.items():
        cache_path = os.path.join(CACHE, src_name)
        if os.path.isfile(cache_path) and os.path.getsize(cache_path) > 10_000:
            data = open(cache_path, "rb").read()
            print(f"  {out_name}: 用缓存")
        else:
            print(f"  {out_name}: 下载中…")
            data = fetch(rel)
            open(cache_path, "wb").write(data)
        check_ttf(data, out_name)
        open(os.path.join(FONT_DIR, out_name), "wb").write(data)

    print("== Material Symbols 可变字体 ==")
    icon_cache = os.path.join(CACHE, "MaterialSymbolsOutlined-VF.ttf")
    if os.path.isfile(icon_cache) and os.path.getsize(icon_cache) > 1_000_000:
        data = open(icon_cache, "rb").read()
        print("  用缓存")
    else:
        print("  下载中…（约 10.7 MB）")
        data = fetch(ICON_FONT_VAR, timeout=300)
        open(icon_cache, "wb").write(data)
    check_ttf(data, "MaterialSymbolsOutlined VF")

    print("== 中文衬线（Noto Serif SC，仅作中间产物）==")
    cjk_cache = os.path.join(CACHE, "NotoSerifSC-VF.ttf")
    if os.path.isfile(cjk_cache) and os.path.getsize(cjk_cache) > 1_000_000:
        print("  用缓存")
    else:
        print("  下载中…（约 25 MB）")
        cjk = fetch(CJK_FONT_VAR, timeout=600)
        check_ttf(cjk, "NotoSerifSC VF")
        open(cjk_cache, "wb").write(cjk)
    print(f"  {os.path.getsize(cjk_cache):,} 字节")

    print("== 官方码点表 ==")
    cp_cache = os.path.join(CACHE, "MaterialSymbolsOutlined.codepoints")
    if os.path.isfile(cp_cache) and os.path.getsize(cp_cache) > 1_000:
        content = open(cp_cache, "r", encoding="utf-8").read()
        print("  用缓存")
    else:
        content = fetch(ICON_CODEPOINTS, timeout=120).decode("utf-8")
        open(cp_cache, "w", encoding="utf-8", newline="\n").write(content)
        print(f"  {len(content.splitlines()):,} 条")

    # 校验设计稿用到的字形是否全部存在
    table = {}
    for line in content.splitlines():
        parts = line.split()
        if len(parts) == 2:
            table[parts[0]] = int(parts[1], 16)

    missing = [g for g in GLYPHS if g not in table]
    if missing:
        print(f"  !! 码点表中缺失 {len(missing)} 个字形: {missing}", file=sys.stderr)
        return 1
    print(f"  全部 {len(GLYPHS)} 个字形均已确认存在")

    # 码点清单（给字体子集化用）
    glyph_file = os.path.join(ROOT, "tools", "ezmenu_glyphs.txt")
    cps = sorted({table[g] for g in GLYPHS})
    with open(glyph_file, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(f"{cp:x}" for cp in cps) + "\n")
    print(f"  已写出 {glyph_file}（{len(cps)} 个唯一码点）")

    # 字形名清单（给 MsIcons.kt 生成用）
    # 必须按名字而非码点过滤：Material Symbols 里很多语义不同的字形共用同一码点
    # （bookmark/bookmark_border、favorite/favorite_border、person/person_outline
    #   靠 FILL 轴区分；schedule/watch_later/access_time 则完全同码点）。
    # 按码点过滤会把 turned_in_not、camera_alt 这类没在用的别名一起拖进来。
    names_file = os.path.join(ROOT, "tools", "ezmenu_glyph_names.txt")
    dupes = len(GLYPHS) - len({table[g] for g in GLYPHS})
    with open(names_file, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(sorted(GLYPHS)) + "\n")
    print(f"  已写出 {names_file}（{len(GLYPHS)} 个字形名；其中 {dupes} 个与他名共用码点）")

    print("\n完成。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
