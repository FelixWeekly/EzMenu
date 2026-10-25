package com.ezmenu.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 「暖调编辑式料理」设计系统的完整色板。
 *
 * 取值来自 `reference/warm_editorial_culinary_system/DESIGN.md` 的 **frontmatter**
 * （`fav/DESIGN.md` 与 `mine/DESIGN.md` 与之字节相同，即 7 个界面共用一套）。
 *
 * ⚠️ 该 DESIGN.md 正文散文里出现的 hex（#E85D04 / #059669 / #FAFAF7 / #292524 /
 *    #E7E5E4）是**过期值**，代码一律以 frontmatter 为准。采样截图验证过：
 *    主操作按钮恰为 #9f3d00，页面底色恰为 #fff8f6。
 *
 * 设计稿只有浅色一套，因此只实现 light scheme（见 Theme.kt）。
 */
object EzColors {

    // ---- Surface 层级 ----
    val Surface = Color(0xFFFFF8F6)
    val SurfaceDim = Color(0xFFE1D8D6)
    val SurfaceBright = Color(0xFFFFF8F6)
    val SurfaceContainerLowest = Color(0xFFFFFFFF)
    val SurfaceContainerLow = Color(0xFFFBF2F0)
    val SurfaceContainer = Color(0xFFF5ECEA)
    val SurfaceContainerHigh = Color(0xFFEFE6E4)
    val SurfaceContainerHighest = Color(0xFFE9E1DF)

    // ---- 前景 ----
    val OnSurface = Color(0xFF1E1B1A)
    val OnSurfaceVariant = Color(0xFF594137)
    val InverseSurface = Color(0xFF342F2E)
    val InverseOnSurface = Color(0xFFF8EFED)

    // ---- 描边 ----
    val Outline = Color(0xFF8D7165)
    val OutlineVariant = Color(0xFFE1BFB2)

    val SurfaceTint = Color(0xFFA33E00)

    // ---- Primary（焦糖橙：主操作、进度、选中态）----
    val Primary = Color(0xFF9F3D00)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFC74E00)
    val OnPrimaryContainer = Color(0xFFFFFBFF)
    val InversePrimary = Color(0xFFFFB596)
    val PrimaryFixed = Color(0xFFFFDBCD)
    val PrimaryFixedDim = Color(0xFFFFB596)
    val OnPrimaryFixed = Color(0xFF360F00)
    val OnPrimaryFixedVariant = Color(0xFF7C2E00)

    // ---- Secondary（罗勒绿：膳食标签、正向确认、勾选态）----
    val Secondary = Color(0xFF006C4A)
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFF82F5C1)
    val OnSecondaryContainer = Color(0xFF00714E)
    val SecondaryFixed = Color(0xFF85F8C4)
    val SecondaryFixedDim = Color(0xFF68DBA9)
    val OnSecondaryFixed = Color(0xFF002114)
    val OnSecondaryFixedVariant = Color(0xFF005137)

    // ---- Tertiary（烘焙琥珀：评分、难度、策展标记）----
    val Tertiary = Color(0xFF8D4B00)
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFFB15F00)
    val OnTertiaryContainer = Color(0xFFFFFBFF)
    val TertiaryFixed = Color(0xFFFFDCC3)
    val TertiaryFixedDim = Color(0xFFFFB77D)
    val OnTertiaryFixed = Color(0xFF2F1500)
    val OnTertiaryFixedVariant = Color(0xFF6E3900)

    // ---- Error ----
    val Error = Color(0xFFBA1A1A)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFFFDAD6)
    val OnErrorContainer = Color(0xFF93000A)

    val Background = Color(0xFFFFF8F6)
    val OnBackground = Color(0xFF1E1B1A)
    val SurfaceVariant = Color(0xFFE9E1DF)
}
