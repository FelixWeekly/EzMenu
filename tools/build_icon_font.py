#!/usr/bin/env python3
"""
把 Material Symbols 可变字体处理成 Android 能直接用的两个静态子集字体。

为什么分两步：
  1) varLib.instancer 把 FILL/GRAD/opsz/wght 四个轴全部钉死 → 得到静态字体。
     先实例化再子集化，可以绕开 pyftsubset 处理可变字体 gvar 表的潜在问题，
     同时也彻底去掉运行时的可变轴开销（图标不需要 FontVariation）。
  2) pyftsubset 裁到设计稿用到的 56 个码点。

为什么 opsz 取 24：
  HTML 里浏览器会按 font-size 自动做光学尺寸调整（opsz 轴），但 Android 的静态
  字体做不到这一点。设计稿图标尺寸集中在 9–24px，24 是该字体的默认设计尺寸，
  也是最中性的选择。这是有意为之的简化，不是疏漏。

用法：python -I tools/build_icon_font.py
"""

from __future__ import annotations

import os
import sys

from fontTools.subset import Options, Subsetter
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, "tools", ".cache")
FONT_DIR = os.path.join(ROOT, "app", "src", "main", "res", "font")
GLYPH_FILE = os.path.join(ROOT, "tools", "ezmenu_glyphs.txt")
SRC = os.path.join(CACHE, "MaterialSymbolsOutlined-VF.ttf")

OPSZ = 24
WGHT = 400


def read_codepoints() -> list[int]:
    with open(GLYPH_FILE, "r", encoding="utf-8") as fh:
        return [int(line.strip(), 16) for line in fh if line.strip()]


def instance(fill: int, out_path: str) -> None:
    font = TTFont(SRC)
    axes = {a.axisTag for a in font["fvar"].axes}
    print(f"    可变轴: {sorted(axes)}")

    location = {"FILL": fill, "GRAD": 0, "opsz": OPSZ, "wght": WGHT}
    location = {k: v for k, v in location.items() if k in axes}
    font = instancer.instantiateVariableFont(font, location, inplace=True, updateFontNames=False)

    # 确认实例化后轴上已无残留
    remaining = sorted({a.axisTag for a in font["fvar"].axes}) if "fvar" in font else []
    font.save(out_path)
    font.close()
    print(f"    实例化 FILL={fill} -> {os.path.getsize(out_path):,} 字节；剩余轴={remaining}")


def subset(codepoints: list[int], src_path: str, out_path: str) -> None:
    font = TTFont(src_path)
    options = Options()
    options.layout_features = []      # 按码点渲染，不需要连字等 GSUB 特性
    options.hinting = False           # 移动端不做微调
    options.desubroutinize = True
    options.notdef_outline = True     # 保留 .notdef，缺字时至少能看出问题
    options.recalc_bounds = True

    subsetter = Subsetter(options=options)
    subsetter.populate(unicodes=codepoints)
    subsetter.subset(font)
    font.save(out_path)
    font.close()

    # 校验：子集后的 cmap 必须覆盖全部请求码点
    check = TTFont(out_path)
    have = set(check.getBestCmap().keys())
    check.close()
    missing = [f"U+{cp:04X}" for cp in codepoints if cp not in have]
    status = "OK" if not missing else f"!! 缺失 {missing}"
    print(f"    子集 -> {os.path.getsize(out_path):,} 字节；覆盖 "
          f"{len(codepoints) - len(missing)}/{len(codepoints)} 码点  {status}")
    if missing:
        raise RuntimeError(f"子集后仍缺码点: {missing}")


def main() -> int:
    if not os.path.isfile(SRC):
        print(f"找不到源字体 {SRC}，请先运行 tools/fetch_assets.py", file=sys.stderr)
        return 1

    codepoints = read_codepoints()
    os.makedirs(FONT_DIR, exist_ok=True)
    print(f"目标码点数: {len(codepoints)}")

    for fill, out_name in ((0, "material_symbols_outlined.ttf"), (1, "material_symbols_filled.ttf")):
        print(f"== FILL={fill} ==")
        full = os.path.join(CACHE, f"ms_fill{fill}_full.ttf")
        instance(fill, full)
        subset(codepoints, full, os.path.join(FONT_DIR, out_name))

    print("\n完成。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
