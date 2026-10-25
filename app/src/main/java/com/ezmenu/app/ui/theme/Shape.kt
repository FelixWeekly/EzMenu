package com.ezmenu.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 圆角体系。
 *
 * ⚠️ 设计稿有个坑：`tailwind.config` 把 `xl` 覆写成了 1.5rem(24px)，但没覆写 `2xl`，
 *    所以 `rounded-xl` 实际是 24px，而 `rounded-2xl` 回落到 Tailwind 默认的 16px ——
 *    也就是 **2xl 反而比 xl 小**。直接照搬这套名字会让人误解。
 *
 * 这里改成语义化的 [EzShapes]，按**元素用途**取名，对照 DESIGN.md 的 Components 章节：
 *   微型形状 4 · 小组件 8 · 缩略图/输入框 12 · 主卡片 16 · hero/弹层 24 · 胶囊 full
 */
object EzShapes {
    /** 4dp —— 食材勾选框、小计数步进器（DESIGN.md 的 `rounded` small） */
    val xs = RoundedCornerShape(4.dp)

    /** 8dp —— 测量单位徽标、内联小块 */
    val sm = RoundedCornerShape(8.dp)

    /** 12dp —— 步骤缩略图、内联输入框、食材行、单位 chip */
    val md = RoundedCornerShape(12.dp)

    /** 16dp —— 主卡片：菜谱卡、食材卡、步骤卡、统计块 */
    val lg = RoundedCornerShape(16.dp)

    /** 24dp —— hero 图卡、底部弹层（顶部圆角） */
    val xl = RoundedCornerShape(24.dp)

    /** 胶囊 —— 筛选 chip、标签、主操作按钮、份量控件 */
    val pill = RoundedCornerShape(percent = 50)
}

/** 供 Material 3 组件（Button/Card/Dialog 等）默认取用。 */
val AppShapes = Shapes(
    extraSmall = EzShapes.xs,
    small = EzShapes.sm,
    medium = EzShapes.md,
    large = EzShapes.lg,
    extraLarge = EzShapes.xl,
)
