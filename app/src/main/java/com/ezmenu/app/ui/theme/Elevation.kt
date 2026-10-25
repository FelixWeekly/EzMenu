package com.ezmenu.app.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 层级与投影。
 *
 * 设计原则「Sensory Depth」要求用**暖调漫射阴影**模拟高级纸张叠在阳光下的木桌上的
 * 感觉，而不是厚重的投影。DESIGN.md 给出的是 CSS 双层阴影：
 *
 *   Level 1 卡片静置:  0 2px 8px -2px rgba(41,37,36,.04), 0 1px 4px -1px rgba(41,37,36,.02)
 *   Level 2 激活/浮层: 0 8px 24px -4px rgba(41,37,36,.08), 0 2px 6px -1px rgba(41,37,36,.04)
 *   Level 3 模态:      0 16px 40px -8px rgba(41,37,36,.16)
 *
 * ⚠️ 已知偏差：Compose 的 `Modifier.shadow()` 不支持 CSS 那样的多层阴影与负 spread，
 *    所以这里用等价的单层 elevation 近似。由于这几层阴影的 alpha 只有 2%–8%，
 *    视觉差异极小。若阶段 1 的截图比对发现层级感不足，再改用自定义
 *    `setShadowLayer` 绘制精确阴影。
 */
object EzElevation {

    /** Level 0 —— 画布底色，零投影 */
    val Flat = 0.dp

    /** Level 1 —— 卡片静置（食材卡、步骤卡、菜谱卡） */
    val Card = 2.dp

    /** Level 2 —— 激活步骤、浮动控件、吸顶栏 */
    val Floating = 8.dp

    /** Level 3 —— 模态底弹层 */
    val Modal = 16.dp
}

/**
 * 给元素加上设计稿的暖调阴影。
 *
 * @param elevation 取 [EzElevation] 的层级
 */
fun Modifier.ezShadow(
    elevation: Dp,
    shape: Shape,
    clip: Boolean = false,
): Modifier = if (elevation == 0.dp) {
    this
} else {
    this.shadow(
        elevation = elevation,
        shape = shape,
        clip = clip,
        ambientColor = EzColors.OnSurface,
        spotColor = EzColors.OnSurface,
    )
}
