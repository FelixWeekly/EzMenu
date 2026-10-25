package com.ezmenu.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 间距体系，取 DESIGN.md frontmatter 的 spacing token（rem → dp 按 1rem = 16dp）。
 *
 * 用法约定（设计稿的布局节奏）：
 *   - 屏幕左右外边距用 [ScreenMargin]（设计稿的 `margin-mobile` = 20dp）
 *   - 组件内部与相邻元素间用 [Xs]–[Md]
 *   - 章节之间用 [Xl]（设计稿用 `space-xl` 分隔菜谱章节）
 */
object EzSpacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 16.dp
    val Lg = 24.dp
    val Xl = 40.dp

    /** 屏幕外边距（mobile）—— 设计稿 `margin-mobile` = 1.25rem */
    val ScreenMargin = 20.dp

    /** 内容内间歇（mobile）—— 设计稿 `gutter-mobile` = 0.75rem */
    val Gutter = 12.dp

    /**
     * 厨房场景下的最小可点区域。
     * DESIGN.md 明确要求关键操作点（开始计时、勾选食材、下一步）保持 48–56dp，
     * 以便湿手或沾面粉时也能准确点按。
     */
    val MinTouchTarget = 48.dp
    val ComfortableTouchTarget = 56.dp
}
