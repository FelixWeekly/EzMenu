#!/usr/bin/env python3
"""
量一下「打包中文衬线字体」的三种覆盖率各要多大代价。

背景：Newsreader / Manrope 都是纯拉丁字体（CJK 覆盖为 0），中文一律回退到系统黑体。
设备上也不存在任何中文衬线字体 —— 实测 FontFamily.Serif / Default / Monospace 对中文
渲染结果完全一样。所以想让中文标题有 DESIGN.md 说的「衬线文学气质」，只能自己打包。

三种覆盖率：
  1) 只用设计稿文案里的字（584 个）—— 最小，但用户自己输入的生僻字会回退成黑体，混排难看
  2) GB2312 一级字表（3755 字）—— 覆盖现代汉语常用字的 99.7%，日常菜谱绰绰有余
  3) GB2312 全表（6763 字）—— 覆盖简化字全集，几乎不可能出现回退

用法：python -I tools/measure_cjk_font.py
"""

from __future__ import annotations

import os
import sys

from fontTools.subset import Options, Subsetter
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, "tools", ".cache")
SRC = os.path.join(CACHE, "NotoSerifSC-VF.ttf")

# 标题会用到的主色衬线字重（设计稿 headline 用 500/600）
WEIGHTS = (500, 600)


def gb2312_hanzi(level: int) -> set[str]:
    """枚举 GB2312 汉字。level=1 → 一级字表(3755)，level=2 → 含二级字表(6763)。"""
    chars: set[str] = set()
    ranges = [(0xB0, 0xD7)] if level == 1 else [(0xB0, 0xD7), (0xD8, 0xF7)]
    for hi_start, hi_end in ranges:
        for hi in range(hi_start, hi_end + 1):
            for lo in range(0xA1, 0xFE + 1):
                try:
                    ch = bytes([hi, lo]).decode("gb2312")
                except UnicodeDecodeError:
                    continue
                if len(ch) == 1 and "一" <= ch <= "鿿":
                    chars.add(ch)
    return chars


def subset_size(font_path: str, chars: set[str], weight: int, out_path: str) -> int:
    font = TTFont(font_path)
    axes = {a.axisTag for a in font["fvar"].axes} if "fvar" in font else set()
    if "wght" in axes:
        font = instancer.instantiateVariableFont(font, {"wght": weight}, inplace=True)

    options = Options()
    options.layout_features = []
    options.hinting = False
    options.desubroutinize = True
    options.drop_tables += ["DSIG"]
    options.glyph_names = False

    subsetter = Subsetter(options=options)
    subsetter.populate(text="".join(sorted(chars)))
    subsetter.subset(font)
    font.save(out_path)
    font.close()
    return os.path.getsize(out_path)


def main() -> int:
    if not os.path.isfile(SRC):
        print(f"缺少 {SRC}\n请先下载：", file=sys.stderr)
        print(
            "  curl -L -o tools/.cache/NotoSerifSC-VF.ttf "
            '"https://ghfast.top/https://raw.githubusercontent.com/google/fonts/main/'
            'ofl/notoserifsc/NotoSerifSC%5Bwght%5D.ttf"',
            file=sys.stderr,
        )
        return 1

    design_chars_path = os.path.join(CACHE, "app_chars.txt")
    design_chars = set(open(design_chars_path, encoding="utf-8").read()) if os.path.isfile(design_chars_path) else set()
    design_hanzi = {c for c in design_chars if "一" <= c <= "鿿"}
    design_other = {c for c in design_chars if not ("一" <= c <= "鿿")}

    sets = {
        "设计稿文案 (584 字)": design_hanzi | design_other,
        "GB2312 一级 (3755 字)": gb2312_hanzi(1) | design_other,
        "GB2312 全表 (6763 字)": gb2312_hanzi(2) | design_other,
    }

    print(f"源字体原始大小: {os.path.getsize(SRC):,} 字节\n")
    for label, chars in sets.items():
        total = 0
        sizes = []
        for weight in WEIGHTS:
            out = os.path.join(CACHE, f"probe_{weight}.ttf")
            size = subset_size(SRC, chars, weight, out)
            sizes.append(size)
            total += size
        per = " + ".join(f"{s/1024:.0f}KB" for s in sizes)
        print(f"  {label:26s} 字符数={len(chars):5d}  字重{WEIGHTS} → {per}  合计 {total/1024:.0f} KB")

    return 0


if __name__ == "__main__":
    sys.exit(main())
