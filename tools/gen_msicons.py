#!/usr/bin/env python3
"""
从官方码点表生成 MsGlyphs.kt。

为什么按码点渲染而不是连字（ligature）：
  HTML 里写的是 >menu_book< 这种连字，但在 Android 上连字依赖 HarfBuzz 的 GSUB
  默认行为，且会与周边中日韩字形的整形互相干扰。用 String(Character.toChars(cp))
  是确定性的，不受上下文影响。

用法：python -I tools/gen_msicons.py
"""

from __future__ import annotations

import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, "tools", ".cache")
GLYPH_FILE = os.path.join(ROOT, "tools", "ezmenu_glyphs.txt")
NAMES_FILE = os.path.join(ROOT, "tools", "ezmenu_glyph_names.txt")
CODEPOINTS = os.path.join(CACHE, "MaterialSymbolsOutlined.codepoints")
OUT = os.path.join(
    ROOT, "app", "src", "main", "java", "com", "ezmenu", "app", "ui", "icons", "MsGlyphs.kt"
)

HEADER = '''package com.ezmenu.app.ui.icons

/**
 * 设计稿用到的 Material Symbols 字形。
 *
 * 由 `tools/gen_msicons.py` 从官方 `.codepoints` 表生成 —— **请勿手改**，
 * 需要增删字形时改 `tools/fetch_assets.py` 里的 GLYPHS 列表并重新生成。
 *
 * 字体只保留了这些码点（见 `tools/ezmenu_glyphs.txt`），共 {count} 个。
 * 码点均位于 BMP 私用区，每个字形对应单个码位，可直接用 [text] 交给 Text 渲染。
 */
enum class MsGlyph(val glyph: String, val codePoint: Int) {{
{entries}
    ;

    /** 供 Compose `Text` 渲染的单字符。 */
    val text: String = String(Character.toChars(codePoint))
}}
'''


def kotlin_name(glyph: str) -> str:
    return "".join(part.capitalize() for part in glyph.split("_"))


def main() -> int:
    for path in (GLYPH_FILE, NAMES_FILE):
        if not os.path.isfile(path):
            print(f"找不到 {path}，请先运行 tools/fetch_assets.py", file=sys.stderr)
            return 1

    with open(GLYPH_FILE, "r", encoding="utf-8") as fh:
        wanted_cps = {int(line.strip(), 16) for line in fh if line.strip()}

    with open(NAMES_FILE, "r", encoding="utf-8") as fh:
        wanted_names = [line.strip() for line in fh if line.strip()]

    table: dict[str, int] = {}
    with open(CODEPOINTS, "r", encoding="utf-8") as fh:
        for line in fh:
            parts = line.split()
            if len(parts) == 2:
                table[parts[0]] = int(parts[1], 16)

    # 按显式字形名取值 —— 不能用 `cp in wanted_cps` 过滤，否则所有共用码点的
    # 别名（turned_in_not / camera_alt / watch_later …）都会被带进来。
    glyphs = sorted((name, table[name]) for name in wanted_names if name in table)

    # 一致性校验：生成的码点集合必须与字体实际子集化的集合一致
    generated_cps = {cp for _, cp in glyphs}
    if generated_cps != wanted_cps:
        only_gen = sorted(generated_cps - wanted_cps)
        only_sub = sorted(wanted_cps - generated_cps)
        raise RuntimeError(
            "码点集合不一致，字体子集与 MsGlyphs.kt 会脱节：\n"
            f"  仅在 MsIcons 中: {[hex(c) for c in only_gen]}\n"
            f"  仅在子集字体中: {[hex(c) for c in only_sub]}"
        )

    seen_names: set[str] = set()
    lines = []
    for name, cp in glyphs:
        kname = kotlin_name(name)
        if kname in seen_names:
            raise RuntimeError(f"Kotlin 枚举名冲突: {kname}（来自 {name}）")
        seen_names.add(kname)
        lines.append(f'    {kname}("{name}", 0x{cp:04X}),')

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(HEADER.format(count=len(glyphs), entries="\n".join(lines)))

    print(f"已写出 {OUT}")
    print(f"  {len(glyphs)} 个字形，覆盖 {len({cp for _, cp in glyphs})} 个唯一码点")
    return 0


if __name__ == "__main__":
    sys.exit(main())
