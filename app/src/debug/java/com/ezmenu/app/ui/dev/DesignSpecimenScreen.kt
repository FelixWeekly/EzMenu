package com.ezmenu.app.ui.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ezmenu.app.ui.icons.MsGlyph
import com.ezmenu.app.ui.icons.MsIcon
import com.ezmenu.app.ui.theme.EzColors
import com.ezmenu.app.ui.theme.EzSpacing
import com.ezmenu.app.ui.theme.EzText
import com.ezmenu.app.ui.theme.ManropeFamily
import com.ezmenu.app.ui.theme.NewsreaderFamily
import com.ezmenu.app.ui.theme.NewsreaderItalicFamily
import com.ezmenu.app.ui.theme.ezText

/**
 * 阶段 1 的一次性核对屏 —— **不随发布版本一起走**，验证完即可删除。
 *
 * 核对四件事（这四条过不了就不该开始写页面）：
 *   1. 全部字形都能渲染，没有豆腐块
 *   2. FILL 0/1 切换真的改变了字形（收藏心形、书签）
 *   3. Manrope 的 700 是**可变字重**，不是 Compose 合成的假粗体
 *   4. Newsreader 的斜体是**真斜体文件**，不是合成倾斜
 */
@Composable
fun DesignSpecimenScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(EzColors.Surface),
        contentPadding = PaddingValues(EzSpacing.Md),
        verticalArrangement = Arrangement.spacedBy(EzSpacing.Md),
    ) {
        item { SectionTitle("0 · 中英混排衬线（长期回归护栏）") }
        item { SerifMixedScriptGuard() }

        item { SectionTitle("1 · 图标（左：描边 FILL=0　右：实心 FILL=1）") }
        item { IconGrid() }

        item { SectionTitle("2 · Manrope 字重 — 检查可变字重是否真生效") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(EzSpacing.Sm)) {
                listOf(
                    FontWeight.W400 to "400",
                    FontWeight.W500 to "500",
                    FontWeight.W600 to "600",
                    FontWeight.W700 to "700",
                    FontWeight.W800 to "800",
                ).forEach { (weight, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = label,
                            modifier = Modifier.width(44.dp),
                            style = ezText(11f, 14f, weight = FontWeight.W700),
                            color = EzColors.Outline,
                        )
                        Text(
                            text = "旬食食谱库 Savory 0123456789",
                            style = ezText(18f, 24f, ManropeFamily, weight),
                            color = EzColors.OnSurface,
                        )
                    }
                }
            }
        }

        item { SectionTitle("3 · Newsreader 常规 vs 斜体 — 检查是否真斜体文件") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(EzSpacing.Xs)) {
                listOf(FontWeight.W400, FontWeight.W500, FontWeight.W600).forEach { weight ->
                    Text(
                        text = "香煎迷迭香脆皮三文鱼排 · Step 01",
                        style = ezText(20f, 26f, NewsreaderFamily, weight),
                        color = EzColors.OnSurface,
                    )
                    Text(
                        text = "香煎迷迭香脆皮三文鱼排 · Step 01",
                        style = ezText(20f, 26f, NewsreaderItalicFamily, weight),
                        color = EzColors.OnSurfaceVariant,
                    )
                }
            }
        }

        item { SectionTitle("4 · EzText 语义化样式（含 9/10/11px 的极小组）") }
        item { SpecimenList() }

        item { SectionTitle("5 · 倒计时大号数字 52px / 字重 300") }
        item { Text(text = "02:36", style = EzText.timerDisplay, color = EzColors.OnSurface) }

        item { SectionTitle("6 · 关键色板") }
        item { ColorStrip() }
    }
}

/**
 * 中英混排衬线回归护栏。
 *
 * 这是全项目最容易被悄悄改坏的地方：Newsreader 与 Noto Serif SC 是**合并**成
 * 单一字体文件（`serif_cjk_*.ttf`）才能让中英混排整体走衬线 —— 因为 Compose 的
 * `FontFamily` 是字重选择列表，不是按语种的回退链，缺字只会回退到系统字体
 * （而系统默认回退是**黑体**，不是衬线）。
 *
 * 每次改字体或工具脚本后都该看一眼这里：
 *   中文若是宋体、拉丁若是 Newsreader（`Edible` 的 E 与 d 有独特的高对比笔形），
 *   才算正常。中文变黑体或拉丁变成通用衬线，就说明合并字体被改坏了。
 */
@Composable
private fun SerifMixedScriptGuard() {
    val samples = listOf(
        "display 44/500" to ezText(44f, 52f, NewsreaderFamily, FontWeight.W500),
        "headlineMd 22/600" to ezText(22f, 28f, NewsreaderFamily, FontWeight.W600),
        "cardTitle 16/600" to ezText(16f, 20f, NewsreaderFamily, FontWeight.W600),
        "cardTitleSm 13/600" to ezText(13f, 17f, NewsreaderFamily, FontWeight.W600),
    )
    Column(verticalArrangement = Arrangement.spacedBy(EzSpacing.Sm)) {
        samples.forEach { (label, style) ->
            Column {
                Text(
                    text = label,
                    style = ezText(9f, 12f, weight = FontWeight.W700),
                    color = EzColors.Outline,
                )
                Text(
                    text = "香煎迷迭香 Edible 三文鱼排 02:36",
                    style = style,
                    color = EzColors.OnSurface,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {    Text(
        text = text,
        style = ezText(14f, 20f, weight = FontWeight.W700),
        color = EzColors.Primary,
    )
}

@Composable
private fun SpecimenList() {
    val rows = listOf(
        Triple("heroBadge 9/700", EzText.heroBadge, "慢炖经典"),
        Triple("microLabel 10/700", EzText.microLabel, "RECIPE LIBRARY"),
        Triple("chip 11/600", EzText.chip, "全部 (24)"),
        Triple("labelSm 11/700", EzText.labelSm, "高蛋白 · 铸铁煎"),
        Triple("meta 12/600", EzText.meta, "18分钟 · 新手友好 · 380 kcal"),
        Triple("searchField 12.5", EzText.searchField, "寻觅当季食味、食材或烹饪法..."),
        Triple("body13 13/400", EzText.body13, "三文鱼吸干水分，均匀涂抹海盐与黑胡椒碎。"),
        Triple("fieldValue 12/600", EzText.fieldValue, "2块 (约350g)"),
        Triple("titleInput 18/600", EzText.titleInput, "焦糖肉桂法式吐司"),
        Triple("cardTitle 16/600", EzText.cardTitle, "法式普罗旺斯慢炖番茄牛腩"),
        Triple("cardTitleSm 13/600", EzText.cardTitleSm, "牛油果溏心蛋酸种吐司"),
        Triple("stepNumber 17 斜体", EzText.stepNumber, "Step 01"),
        Triple("statValue 18/600", EzText.statValue, "18"),
        Triple("navLabel 10/500", EzText.navLabel, "菜谱库"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(EzSpacing.Sm)) {
        rows.forEach { (label, style, sample) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    modifier = Modifier.width(120.dp),
                    style = ezText(10f, 14f, weight = FontWeight.W600),
                    color = EzColors.Outline,
                )
                Text(text = sample, style = style, color = EzColors.OnSurface)
            }
        }
    }
}

@Composable
private fun IconGrid() {
    val icons = MsGlyph.entries
    Column(verticalArrangement = Arrangement.spacedBy(EzSpacing.Sm)) {
        icons.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(EzSpacing.Xs),
            ) {
                row.forEach { icon ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(EzSpacing.Sm),
                        ) {
                            MsIcon(icon = icon, size = 24.dp, filled = false, tint = EzColors.OnSurface)
                            MsIcon(icon = icon, size = 24.dp, filled = true, tint = EzColors.Primary)
                        }
                        Text(
                            text = icon.glyph,
                            style = ezText(8f, 11f),
                            color = EzColors.Outline,
                            maxLines = 1,
                        )
                    }
                }
                // 补齐末行，保持列宽一致
                repeat(3 - row.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ColorStrip() {
    val swatches: List<Pair<String, Color>> = listOf(
        "primary" to EzColors.Primary,
        "primaryFixed" to EzColors.PrimaryFixed,
        "secondary" to EzColors.Secondary,
        "secondaryContainer" to EzColors.SecondaryContainer,
        "tertiary" to EzColors.Tertiary,
        "tertiaryFixed" to EzColors.TertiaryFixed,
        "error" to EzColors.Error,
        "surfaceContainerLow" to EzColors.SurfaceContainerLow,
        "surfaceContainer" to EzColors.SurfaceContainer,
        "surfaceContainerHighest" to EzColors.SurfaceContainerHighest,
        "onSurface" to EzColors.OnSurface,
        "onSurfaceVariant" to EzColors.OnSurfaceVariant,
        "outline" to EzColors.Outline,
        "outlineVariant" to EzColors.OutlineVariant,
        "inverseSurface" to EzColors.InverseSurface,
    )
    Column(verticalArrangement = Arrangement.spacedBy(EzSpacing.Xs)) {
        swatches.chunked(3).forEach { chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(EzSpacing.Xs)) {
                chunk.forEach { (name, color) ->
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .background(color, RoundedCornerShape(6.dp)),
                        )
                        Text(
                            text = name,
                            style = ezText(8f, 11f),
                            color = EzColors.OnSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                repeat(3 - chunk.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}
