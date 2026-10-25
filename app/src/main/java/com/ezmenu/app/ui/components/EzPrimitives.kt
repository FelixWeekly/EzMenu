package com.ezmenu.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ezmenu.app.data.local.entity.TagKind
import com.ezmenu.app.ui.theme.EzColors
import com.ezmenu.app.ui.theme.EzShapes
import com.ezmenu.app.ui.theme.EzSpacing
import com.ezmenu.app.ui.theme.EzText
import com.ezmenu.app.ui.theme.ezText

/**
 * 小节标题：「中文主标题 + 全大写英文副标」是这套设计的固定句式。
 *
 * 例如「旬味特选 / TODAY'S PICK」「灵感餐单 / DAILY INSPIRATION」。
 * 英文副标一律 Manrope 全大写、加字距、用 outline 色 —— 这是设计里区分
 * 编辑式标题与功能性标签的关键手法。
 */
@Composable
fun EzSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitleEn: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        // 必须占满宽度，否则 SpaceBetween 没有可分配的剩余空间，
        // 右侧的 trailing 动作会紧贴英文副标
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = ezText(16f, 21f, com.ezmenu.app.ui.theme.NewsreaderFamily, FontWeight.W600),
                color = EzColors.OnSurface,
            )
            if (subtitleEn != null) {
                Text(
                    text = subtitleEn.uppercase(),
                    style = ezText(10f, 14f, weight = FontWeight.W700, letterSpacingEm = 0.14f),
                    color = EzColors.Outline,
                )
            }
        }
        trailing?.invoke()
    }
}

/**
 * 筛选 chip。
 *
 * 选中态是实心主色 + 白字，未选中是白底 + 暖灰描边 —— 设计稿里菜谱库的分类 chip
 * 和珍藏页的筛选 pill 共用这一套（仅内边距略有差别）。
 */
@Composable
fun EzFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = EzText.chip,
) {
    val bg = if (selected) EzColors.Primary else EzColors.SurfaceContainerLowest
    val fg = if (selected) EzColors.OnPrimary else EzColors.OnSurfaceVariant

    Box(
        modifier = modifier
            .clip(EzShapes.pill)
            .background(bg)
            .then(
                if (selected) Modifier
                else Modifier.border(BorderStroke(1.dp, EzColors.SurfaceContainerHigh), EzShapes.pill),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(text = text, style = style, color = fg)
    }
}

/**
 * 分类标签（卡片角上的小色块）。
 *
 * 三套配色对应 [TagKind]：膳食标签走罗勒绿、场景标签走烘焙琥珀、时令标签走焦糖橙。
 * 设计稿的卡片角标正是这样区分调性的。
 */
@Composable
fun EzTagPill(
    text: String,
    kind: TagKind,
    modifier: Modifier = Modifier,
) {
    val (bg, fg) = when (kind) {
        TagKind.DIET -> EzColors.SecondaryContainer.copy(alpha = 0.8f) to EzColors.OnSecondaryContainer
        TagKind.SCENE -> EzColors.TertiaryFixed to EzColors.OnTertiaryFixed
        TagKind.SEASON -> EzColors.PrimaryFixed to EzColors.OnPrimaryFixedVariant
        TagKind.CATEGORY -> EzColors.SurfaceContainer to EzColors.OnSurfaceVariant
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text = text, style = EzText.heroBadge, color = fg)
    }
}

/**
 * 白底卡片。
 *
 * 设计原则「Sensory Depth」要求卡片是**描边 + 暖调漫射阴影**，而不是厚重投影 ——
 * 模拟高级纸张叠在木桌上的感觉。这里统一 1dp 描边 + 极低 elevation。
 */
@Composable
fun EzCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = EzShapes.lg,
    background: Color = EzColors.SurfaceContainerLowest,
    borderColor: Color = EzColors.OutlineVariant.copy(alpha = 0.35f),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(BorderStroke(1.dp, borderColor), shape),
    ) {
        content()
    }
}

/**
 * 圆形图标按钮。
 *
 * 按下时缩到 0.92 —— 设计稿里几乎所有可点元素都有 `active:scale-95` 这类反馈。
 * 厨房场景下手指可能潮湿，明确的按压反馈比悬停态更重要。
 *
 * @param size 视觉尺寸。设计稿里常用 32–36dp，但可点区域在 [EzSpacing.MinTouchTarget]
 *   可控范围内会保持至少 48dp（见 `minTouchTarget` 参数）。
 */
@Composable
fun EzIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    background: Color = Color.Transparent,
    enabled: Boolean = true,
    minTouchTarget: Boolean = false,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // 设计稿里图标按钮多为 32–36dp，低于 48dp 的可点区域下限。
    // 需要厨房场景下可靠点击的地方传 minTouchTarget = true。
    val touchSize = if (minTouchTarget) maxOf(size, EzSpacing.MinTouchTarget) else size

    Box(
        modifier = modifier
            .size(touchSize)
            .scale(if (pressed && enabled) 0.92f else 1f)
            .clip(EzShapes.pill)
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

