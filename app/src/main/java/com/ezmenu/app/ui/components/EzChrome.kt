package com.ezmenu.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ezmenu.app.ui.icons.MsGlyph
import com.ezmenu.app.ui.icons.MsIcon
import com.ezmenu.app.ui.theme.EzColors
import com.ezmenu.app.ui.theme.EzShapes
import com.ezmenu.app.ui.theme.EzText
import com.ezmenu.app.ui.theme.NewsreaderFamily
import com.ezmenu.app.ui.theme.ezText

// ------------------------------------------------------------------ 底部导航

/** 四个 Tab。顺序即设计稿顺序，图标也与其一致。 */
enum class EzTab(val route: String, val label: String, val glyph: MsGlyph) {
    LIBRARY("library", "菜谱库", MsGlyph.MenuBook),
    DISCOVER("discover", "发现", MsGlyph.Explore),
    FAVORITES("favorites", "珍藏", MsGlyph.BookmarkBorder),
    PROFILE("profile", "我的", MsGlyph.PersonOutline),
}

/**
 * 底部导航栏。
 *
 * **样式归一化的说明**：四份设计稿在这一处并不自洽 ——
 *   菜谱库 / 发现：栏高 56dp、图标 21px、标签 10px，选中项仍用描边字形
 *   珍藏：栏高 64dp、图标 23px、标签 11px，选中项用实心书签
 *   我的：栏高 64dp、图标 24px，选中项用实心人像
 * 四份里有两份用第一种，故**以第一种为准**，字形一律保持描边、靠颜色和字重
 * 表达选中态。这是有意的统一，不是漏做。
 */
@Composable
fun EzBottomNav(
    current: EzTab,
    onSelect: (EzTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(EzColors.Surface.copy(alpha = 0.96f))
            .border(1.dp, EzColors.SurfaceContainerHigh.copy(alpha = 0.5f), EzShapes.xs),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            EzTab.entries.forEach { tab ->
                val selected = tab == current
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(EzShapes.md)
                        .clickable { onSelect(tab) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    MsIcon(
                        icon = tab.glyph,
                        size = 21.dp,
                        tint = if (selected) EzColors.Primary else EzColors.OnSurfaceVariant,
                    )
                    Text(
                        text = tab.label,
                        style = EzText.navLabel.copy(
                            fontWeight = if (selected) FontWeight.W700 else FontWeight.W500,
                        ),
                        color = if (selected) EzColors.Primary else EzColors.OnSurfaceVariant,
                    )
                }
            }
        }
        // 让内容避开系统手势条
        Box(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }
}

// ------------------------------------------------------------------ 品牌标头

/**
 * 品牌标头。
 *
 * 设计稿里各屏的表头并不统一，这里提供两种形态：
 *   - [BrandHeaderStyle.LOGO]：图形标 + 两行品牌名 + 全大写英文副标 + 两个动作按钮（菜谱库、我的）
 *   - [BrandHeaderStyle.TEXT]：纯文字品牌 + 副标 + 单个动作按钮（发现、珍藏）
 *
 * 品牌名的英文部分用 **EzMenu** 而不是设计稿里的 "Savory" ——
 * 这是用户确认的命名决定（中文「旬食」，英文「EzMenu」）。
 */
enum class BrandHeaderStyle { LOGO, TEXT }

@Composable
fun BrandHeader(
    style: BrandHeaderStyle,
    englishName: String,
    modifier: Modifier = Modifier,
    subtitleEn: String? = null,
    onNotifications: (() -> Unit)? = null,
    onAddRecipe: (() -> Unit)? = null,
    showUnreadDot: Boolean = false,
    trailingExtra: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth().background(EzColors.Surface)) {
        Box(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = if (style == BrandHeaderStyle.LOGO) 16.dp else ScreenMarginWide),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (style == BrandHeaderStyle.LOGO) {
                    EzPhoto(
                        ref = "drawable:brand_logo",
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "旬食",
                            style = ezText(19f, 22f, NewsreaderFamily, FontWeight.W600),
                            color = EzColors.Primary,
                        )
                        Text(
                            text = englishName,
                            style = ezText(15f, 18f, NewsreaderFamily, FontWeight.W400, italic = true),
                            color = EzColors.OnSurfaceVariant,
                        )
                    }
                    subtitleEn?.let {
                        Text(
                            text = it.uppercase(),
                            style = ezText(9f, 11f, weight = FontWeight.W700, letterSpacingEm = 0.16f),
                            color = EzColors.Outline,
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                trailingExtra?.invoke()
                onNotifications?.let {
                    EzIconButton(onClick = it, size = 36.dp) {
                        Box {
                            MsIcon(icon = MsGlyph.Notifications, size = 19.dp, tint = EzColors.OnSurfaceVariant)
                            if (showUnreadDot) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 5.dp, end = 5.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(EzColors.Primary),
                                )
                            }
                        }
                    }
                }
                onAddRecipe?.let {
                    EzIconButton(onClick = it, modifier = Modifier.semantics { contentDescription = "新建菜谱" }, size = 36.dp, background = EzColors.Primary) {
                        MsIcon(icon = MsGlyph.Add, size = 18.dp, tint = EzColors.OnPrimary)
                    }
                }
            }
        }

        // 与内容区之间的细分隔线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(EzColors.SurfaceContainerHigh.copy(alpha = 0.5f)),
        )
    }
}

// ------------------------------------------------------------------ 压栈页表头

/**
 * 详情/编辑页的顶栏：返回 + 居中标题 + 右侧动作。
 *
 * 背景用半透明的 surface + 底部细线，让内容滚过时有一层薄纱感 ——
 * 设计稿里这几屏的顶栏都是 `bg-surface/90` + 毛玻璃。
 */
@Composable
fun EzTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth().background(EzColors.Surface)) {
        Box(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EzIconButton(onClick = onBack, size = 44.dp) {
                MsIcon(icon = MsGlyph.ArrowBack, size = 22.dp, tint = EzColors.OnSurface)
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
                style = ezText(17f, 22f, NewsreaderFamily, FontWeight.W600),
                color = EzColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(end = 4.dp),
            ) {
                actions()
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(EzColors.OutlineVariant.copy(alpha = 0.35f)),
        )
    }
}

