package com.ezmenu.app.ui.icons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import com.ezmenu.app.R

/**
 * Material Symbols 图标渲染。
 *
 * 做法：把图标当作**字形文本**渲染，而不是用 `material-icons-extended`。
 *
 *   - 设计稿用的是 Material **Symbols**，而 `material-icons-extended` 里冻结的是
 *     Material **Icons**，两者字形不同，混用会出现两种图标语言。
 *   - 设计稿有 8 处用 `font-variation-settings: 'FILL' 1` 在实心/描边之间切换
 *     （收藏心形、导航栏书签）。字体方案可以原生还原。
 *
 * 字体由 `tools/build_icon_font.py` 预实例化 + 子集化：
 *   原始可变字体 10.7 MB → 两个静态子集各约 10 KB。
 * 因为已经实例化成静态字体，这里**不需要** FontVariation，也就完全不受
 * 可变轴在低版本上静默降级的影响。
 */

/** 描边版（FILL=0） */
val MsOutlinedFamily = FontFamily(Font(R.font.material_symbols_outlined))

/** 实心版（FILL=1） */
val MsFilledFamily = FontFamily(Font(R.font.material_symbols_filled))

// 图标字形自带完整宽度，不需要字体额外的上下留白 —— 去掉 includeFontPadding
// 才能让字形在设计稿那些 8dp/11dp 的小圆形容器里真正居中。
// 注：`PlatformTextStyle(Boolean)` 在新版 Compose 里标记为 deprecated 但仍可用，
// 且仍是控制 includeFontPadding 的公开途径，故显式抑制。
@Suppress("DEPRECATION")
private val IconTextStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

/**
 * 渲染一个 Material Symbols 字形。
 *
 * 尺寸约定：设计稿里图标用 `text-[Npx]` 指定，范围 9–24px，按 **1 CSS px = 1 dp** 映射。
 * 图标保持 dp 尺寸，避免大字体时字形被固定容器裁切。
 *
 * @param icon 字形，取自生成的 [MsGlyph] 枚举
 * @param filled `true` 用实心版字形（对应设计稿的 `'FILL' 1`）
 */
@Composable
fun MsIcon(
    icon: MsGlyph,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = LocalContentColor.current,
    filled: Boolean = false,
    contentDescription: String? = null,
) {
    Box(modifier = modifier.size(size).clearAndSetSemantics { if(contentDescription!=null) this.contentDescription=contentDescription }, contentAlignment = Alignment.Center) {
        Text(
            text = icon.text,
            color = tint,
            fontFamily = if (filled) MsFilledFamily else MsOutlinedFamily,
            fontSize = with(LocalDensity.current) { size.toSp() },
            lineHeight = with(LocalDensity.current) { size.toSp() },
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            style = IconTextStyle,
        )
    }
}
