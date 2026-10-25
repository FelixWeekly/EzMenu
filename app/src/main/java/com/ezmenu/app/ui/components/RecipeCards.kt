package com.ezmenu.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ezmenu.app.data.local.entity.TagKind
import com.ezmenu.app.domain.model.RecipeCard
import com.ezmenu.app.ui.icons.MsGlyph
import com.ezmenu.app.ui.icons.MsIcon
import com.ezmenu.app.ui.theme.EzColors
import com.ezmenu.app.ui.theme.EzShapes
import com.ezmenu.app.ui.theme.EzSpacing
import com.ezmenu.app.ui.theme.EzText
import com.ezmenu.app.ui.theme.NewsreaderFamily
import com.ezmenu.app.ui.theme.ezText

// ------------------------------------------------------------------ 收藏按钮

/**
 * 收藏心形。
 *
 * 设计稿用 `font-variation-settings: 'FILL' 0/1` 在描边/实心之间切换，
 * 并用 180ms 的 `scale(1.25)` 回弹做点击反馈 —— 这里两个都还原：
 * 字形真的换成实心版（不是换图标），按下时缩一下。
 */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    iconSize: Dp = 16.dp,
) {
    EzIconButton(
        onClick = onToggle,
        size = size,
        background = EzColors.Surface.copy(alpha = 0.85f),
        modifier = modifier,
    ) {
        MsIcon(
            icon = if (isFavorite) MsGlyph.Favorite else MsGlyph.FavoriteBorder,
            size = iconSize,
            tint = if (isFavorite) EzColors.Primary else EzColors.Outline,
            filled = isFavorite,
        )
    }
}

// ------------------------------------------------------------------ 时间胶囊

/** 图片上的白色时间胶囊：「🕐 20 min」。 */
@Composable
fun CookTimePill(
    label: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (label.isEmpty()) return
    Row(
        modifier = modifier
            .clip(EzShapes.pill)
            .background(EzColors.Surface.copy(alpha = 0.92f))
            .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        MsIcon(
            icon = MsGlyph.Schedule,
            size = if (compact) 10.dp else 12.dp,
            tint = EzColors.Tertiary,
        )
        Text(
            text = label,
            style = if (compact) EzText.heroBadge else EzText.microLabel,
            color = EzColors.OnSurface,
        )
    }
}

// ------------------------------------------------------------------ hero 卡

/**
 * 「旬味特选」大卡。
 *
 * 结构对照 `library/code.html`：155dp 高、满宽、底部重黑渐变压住标题，
 * 左上角是主厨甄选徽标、右上角收藏、左下角分类标签与菜名、右下角开煮按钮。
 */
@Composable
fun HeroRecipeCard(
    card: RecipeCard,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(155.dp)
            .clip(EzShapes.lg)
            .clickable(onClick = onOpen),
    ) {
        EzPhoto(
            ref = card.coverImageRef,
            contentDescription = card.title,
                placeholderLabel = card.title,
            modifier = Modifier.fillMaxSize(),
        )

        // 自下而上的压暗渐变：底部 85% 黑 → 中部 35% → 顶部 10%
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.10f),
                        0.45f to Color.Black.copy(alpha = 0.35f),
                        1f to Color.Black.copy(alpha = 0.85f),
                    ),
                ),
        )

        // 顶部：徽标 + 收藏
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            card.heroBadge?.let { badge ->
                Row(
                    modifier = Modifier
                        .clip(EzShapes.pill)
                        .background(EzColors.Surface.copy(alpha = 0.92f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    MsIcon(icon = MsGlyph.LocalFireDepartment, size = 11.dp, tint = EzColors.Primary, filled = true)
                    Text(text = badge, style = EzText.microLabel, color = EzColors.OnSurface)
                }
            } ?: Box(Modifier)
            FavoriteButton(
                isFavorite = card.isFavorite,
                onToggle = onToggleFavorite,
                size = 28.dp,
                iconSize = 16.dp,
            )
        }

        // 底部：标签 + 菜名 + 开煮
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                card.heroKicker?.let { kicker ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(EzColors.Primary.copy(alpha = 0.85f))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        ) {
                            Text(text = kicker, style = EzText.heroBadge, color = EzColors.OnPrimary)
                        }
                        card.heroNote?.let { note ->
                            Text(
                                text = note,
                                style = EzText.heroBadge,
                                color = EzColors.PrimaryFixed,
                            )
                        }
                    }
                }
                Text(
                    text = card.title,
                    style = ezText(16f, 21f, NewsreaderFamily, FontWeight.W600),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                modifier = Modifier
                    .clip(EzShapes.pill)
                    .background(EzColors.Surface.copy(alpha = 0.95f))
                    .border(1.dp, Color.White.copy(alpha = 0.20f), EzShapes.pill)
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = card.heroCta ?: "开煮",
                    style = EzText.chip,
                    color = EzColors.Primary,
                )
                MsIcon(icon = MsGlyph.ArrowForward, size = 12.dp, tint = EzColors.Primary)
            }
        }
    }
}

// ------------------------------------------------------------------ 网格卡

/**
 * 「灵感餐单」两列网格卡：4:3 图 + 时间胶囊 + 收藏 + 标签 + 菜名 + 作者/点评。
 */
@Composable
fun RecipeGridCard(
    card: RecipeCard,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(EzShapes.lg)
            .background(EzColors.SurfaceContainerLowest)
            .border(1.dp, EzColors.OutlineVariant.copy(alpha = 0.30f), EzShapes.lg)
            .clickable(onClick = onOpen),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
            EzPhoto(
                ref = card.coverImageRef,
                contentDescription = card.title,
                placeholderLabel = card.title,
                modifier = Modifier.fillMaxSize(),
            )
            CookTimePill(
                label = card.cookTimeLabel,
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                compact = true,
            )
            FavoriteButton(
                isFavorite = card.isFavorite,
                onToggle = onToggleFavorite,
                size = 24.dp,
                iconSize = 13.dp,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
            )
        }

        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            card.tags.firstOrNull()?.let { tag ->
                EzTagPill(
                    text = tag.name,
                    kind = runCatching { TagKind.valueOf(tag.kind) }.getOrDefault(TagKind.DIET),
                )
            }
            Text(
                text = card.title,
                style = ezText(13f, 17f, NewsreaderFamily, FontWeight.W600),
                color = EzColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                card.author?.let {
                    Text(text = it, style = EzText.microLabel, color = EzColors.Outline, maxLines = 1)
                } ?: Box(Modifier)
                card.footerNote?.let {
                    Text(text = it, style = EzText.microLabel, color = EzColors.Secondary, maxLines = 1)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ 横排卡

/**
 * 横向列表卡：96dp 方图 + 右侧文字。珍藏页的后两张卡与「余料寻味」的建议卡用这个。
 */
@Composable
fun RecipeRowCard(
    card: RecipeCard,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    metaOverride: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    EzCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EzPhoto(
                ref = card.coverImageRef,
                contentDescription = card.title,
                placeholderLabel = card.title,
                modifier = Modifier.size(72.dp).clip(EzShapes.md),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                card.tags.firstOrNull()?.let { tag ->
                    EzTagPill(
                        text = tag.name,
                        kind = runCatching { TagKind.valueOf(tag.kind) }.getOrDefault(TagKind.DIET),
                    )
                }
                Text(
                    text = card.title,
                    style = ezText(15f, 20f, NewsreaderFamily, FontWeight.W600),
                    color = EzColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = metaOverride ?: card.subtitle,
                    style = EzText.body13,
                    color = EzColors.OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = card.footerNote ?: "",
                        style = EzText.microLabel,
                        color = EzColors.Outline,
                        maxLines = 1,
                    )
                    trailing?.invoke()
                }
            }
        }
    }
}

/** 「开煮 / 浏览 / 品鉴」这类卡片内的文字动作。 */
@Composable
fun CardTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = text, style = EzText.chip, color = EzColors.Primary)
        MsIcon(icon = MsGlyph.ArrowForward, size = 12.dp, tint = EzColors.Primary)
    }
}

/** 两列网格的间距常量，供各屏统一使用。 */
val GridGutter = 10.dp

/** 菜谱库内容区的左右外边距（设计稿 `px-3.5`）。 */
val ScreenPadding = 14.dp

/** 发现/珍藏/我的 三屏的内容区外边距（设计稿 `px-margin-mobile`）。 */
val ScreenMarginWide = 20.dp
