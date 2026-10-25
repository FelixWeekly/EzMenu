#!/usr/bin/env python3
"""
构建「拉丁衬线 Newsreader + 中文衬线 Noto Serif SC」的合并字体。

为什么必须合并，而不是靠 Compose 的 FontFamily 回退：
  Compose 的 `FontFamily` 是**字重/样式的选择列表**，不是按语种的回退链。
  某一行文字里如果某个字在选中的 Font 里没有，Android 会回退到**系统字体**，
  而不会去找同一个 FontFamily 里的其它条目。而系统里根本没有中文衬线
  （实测 FontFamily.Serif / Default / Monospace 对中文渲染结果完全一样）。
  所以想让「香煎迷迭香 Salmon Fillet」这种中英混排的一行里，中文走衬线、
  拉丁走 Newsreader，只能把两者真正合成一个字体文件。

处理步骤（每个字重各做一遍）：
  1. Newsreader 实例化到目标字重 → 子集化到拉丁/标点 → **缩放 UPM 2000→1000**
  2. Noto Serif SC 实例化到目标字重 → 子集化到 GB2312 一级字表
  3. 两者都去掉 GSUB/GPOS/kern（避免合并冲突；标题不需要复杂整形）
  4. 合并：Newsreader 在前，所以拉丁与标点的字形由 Newsreader 提供，
     Noto Serif SC 只贡献中文字形
  5. 统一垂直度量（中文字面框更高，取能同时容纳两者的值）

用法：python -I tools/build_cjk_serif.py
"""

from __future__ import annotations

import os
import sys

from fontTools.merge import Merger
from fontTools.subset import Options, Subsetter
from fontTools.ttLib import TTFont
from fontTools.ttLib.scaleUpem import scale_upem
from fontTools.varLib import instancer

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, "tools", ".cache")
FONT_DIR = os.path.join(ROOT, "app", "src", "main", "res", "font")

LATIN_SRC = os.path.join(FONT_DIR, "newsreader.ttf")
CJK_SRC = os.path.join(CACHE, "NotoSerifSC-VF.ttf")

# 设计稿的标题字重：headline 用 500/600
WEIGHTS = (500, 600)
TARGET_UPM = 1000

# 合并后的垂直度量（单位 = TARGET_UPM）。
# 中文字面框比拉丁高，所以取能同时容纳两者的范围；Compose 侧每条 TextStyle
# 都显式指定 lineHeight，这里主要影响基线在行盒里的落点。
MERGED_ASCENT = 1160
MERGED_DESCENT = -290
MERGED_TYPO_ASC = 880
MERGED_TYPO_DESC = -120
MERGED_WIN_ASC = 1160
MERGED_WIN_DESC = 290


def gb2312_level1() -> set[str]:
    """GB2312 一级字表（3755 个最常用汉字）。"""
    chars: set[str] = set()
    for hi in range(0xB0, 0xD7 + 1):
        for lo in range(0xA1, 0xFE + 1):
            try:
                ch = bytes([hi, lo]).decode("gb2312")
            except UnicodeDecodeError:
                continue
            if len(ch) == 1 and "一" <= ch <= "鿿":
                chars.add(ch)
    return chars


def strip_layout_tables(font: TTFont) -> None:
    """
    去掉复杂整形表与竖排表，避免合并时冲突。

    变体表（HVAR/VVAR/MVAR/STAT/fvar/gvar/avar）**必须**清掉：
    钉死全部轴之后理论上它们会自动消失，但 `fontTools.merge` 对残留的
    variation store 会直接抛 `AttributeError: VarStore has no mergeMap`，
    所以这里显式兜底。竖排表（vhea/vmtx/BASE）来自 CJK 字体，合并时也不兼容。
    """
    for tag in (
        "GSUB", "GPOS", "GDEF", "kern", "morx", "mort", "DSIG",
        "HVAR", "VVAR", "MVAR", "STAT", "fvar", "gvar", "avar", "cvar",
        "BASE", "vhea", "vmtx", "VORG",
    ):
        if tag in font:
            del font[tag]


def pin_all_axes(font: TTFont, weight: int) -> TTFont:
    """
    把**所有**可变轴都钉死。

    只钉 wght 是不够的：Newsreader 还有 opsz 轴，只要还有轴没钉，fvar/gvar/HVAR
    就会留在字体里，合并时必然失败。

    opsz 取 28：设计稿里 Newsreader 的实际使用跨度是 13px（小卡片标题）到
    52px（倒计时数字），28 是居中偏保守的选择。中文字形不受 opsz 影响。
    """
    if "fvar" not in font:
        return font
    location = {}
    for axis in font["fvar"].axes:
        if axis.axisTag == "wght":
            location["wght"] = weight
        elif axis.axisTag == "opsz":
            location["opsz"] = 28
        else:
            location[axis.axisTag] = axis.defaultValue
    return instancer.instantiateVariableFont(font, location, inplace=True)


def build_latin(weight: int, out_path: str) -> None:
    font = pin_all_axes(TTFont(LATIN_SRC), weight)

    options = Options()
    options.layout_features = []
    options.hinting = False
    options.desubroutinize = True
    options.glyph_names = False
    # 只保留拉丁、数字、常用标点（CJK 交给 Noto）。\u0000-ɏ 覆盖基本拉丁+拉丁扩展A/B
    unicodes = list(range(0x0020, 0x0250)) + list(range(0x2000, 0x2070)) + [0x00B0, 0x00D7]
    subsetter = Subsetter(options=options)
    subsetter.populate(unicodes=unicodes)
    subsetter.subset(font)

    if font["head"].unitsPerEm != TARGET_UPM:
        scale_upem(font, TARGET_UPM)

    strip_layout_tables(font)
    font.save(out_path)
    font.close()


def build_cjk(weight: int, out_path: str, chars: set[str]) -> None:
    font = pin_all_axes(TTFont(CJK_SRC), weight)

    options = Options()
    options.layout_features = []
    options.hinting = False
    options.desubroutinize = True
    options.glyph_names = False
    subsetter = Subsetter(options=options)
    subsetter.populate(text="".join(sorted(chars)))
    subsetter.subset(font)

    if font["head"].unitsPerEm != TARGET_UPM:
        scale_upem(font, TARGET_UPM)

    strip_layout_tables(font)
    font.save(out_path)
    font.close()


def unify_metrics(path: str) -> None:
    font = TTFont(path)
    font["hhea"].ascender = MERGED_ASCENT
    font["hhea"].descender = MERGED_DESCENT
    os2 = font["OS/2"]
    os2.sTypoAscender = MERGED_TYPO_ASC
    os2.sTypoDescender = MERGED_TYPO_DESC
    os2.sTypoLineGap = 0
    os2.usWinAscent = MERGED_WIN_ASC
    os2.usWinDescent = MERGED_WIN_DESC
    font.save(path)
    font.close()


def main() -> int:
    for path in (LATIN_SRC, CJK_SRC):
        if not os.path.isfile(path):
            print(f"缺少 {path}", file=sys.stderr)
            return 1

    chars = gb2312_level1()
    print(f"中文字符集: {len(chars)} 个（GB2312 一级字表）")

    for weight in WEIGHTS:
        print(f"== 字重 {weight} ==")
        latin = os.path.join(CACHE, f"latin_{weight}.ttf")
        cjk = os.path.join(CACHE, f"cjk_{weight}.ttf")
        out = os.path.join(FONT_DIR, f"serif_cjk_{weight}.ttf")

        build_latin(weight, latin)
        print(f"  拉丁(Newsreader) 子集+缩放UPM: {os.path.getsize(latin):,} 字节")

        build_cjk(weight, cjk, chars)
        print(f"  中文(NotoSerifSC) 子集:        {os.path.getsize(cjk):,} 字节")

        merged = Merger().merge([latin, cjk])
        merged.save(out)
        merged.close()

        unify_metrics(out)

        # 校验：合并后必须同时具备拉丁与中文
        check = TTFont(out)
        cmap = check.getBestCmap()
        probes = {"A": 0x41, "z": 0x7A, "0": 0x30, "香": 0x9999, "煎": 0x714E, "食": 0x98DF}
        got = {k: (v in cmap) for k, v in probes.items()}
        check.close()
        ok = all(got.values())
        print(f"  → {os.path.basename(out)}: {os.path.getsize(out):,} 字节  "
              f"字形覆盖 {got}  {'OK' if ok else '!! 有缺失'}")
        if not ok:
            return 1

    print("\n完成。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
