package com.ezmenu.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.ezmenu.app.R

val ManropeFamily = FontFamily(
    Font(R.font.reading_sans_400,FontWeight.W400),
    Font(R.font.reading_sans_500,FontWeight.W500),
    Font(R.font.reading_sans_600,FontWeight.W600),
)
val NewsreaderFamily = FontFamily(
    Font(R.font.serif_cjk_400,FontWeight.W400),
    Font(R.font.serif_cjk_500,FontWeight.W500),
    Font(R.font.serif_cjk_600,FontWeight.W600),
)
@OptIn(ExperimentalTextApi::class)
val NewsreaderThinFamily = FontFamily(Font(R.font.newsreader,weight=FontWeight.W300,variationSettings=FontVariation.Settings(FontVariation.weight(300))))
@OptIn(ExperimentalTextApi::class)
val NewsreaderItalicFamily = FontFamily(
    Font(R.font.newsreader_italic,weight=FontWeight.W400,style=FontStyle.Italic,variationSettings=FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.newsreader_italic,weight=FontWeight.W500,style=FontStyle.Italic,variationSettings=FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.newsreader_italic,weight=FontWeight.W600,style=FontStyle.Italic,variationSettings=FontVariation.Settings(FontVariation.weight(600))),
)

private val CenteredLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)


@Suppress("LongParameterList")
fun ezText(
    sizePx: Float,
    lineHeightPx: Float = sizePx * 1.35f,
    family: FontFamily = ManropeFamily,
    weight: FontWeight = FontWeight.W400,
    italic: Boolean = false,
    letterSpacingEm: Float = 0f,
): TextStyle = TextStyle(
    fontFamily = family,
    fontSize = sizePx.sp,
    lineHeight = lineHeightPx.sp,
    fontWeight = weight,
    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    letterSpacing = (sizePx * letterSpacingEm).sp,
    fontSynthesis = androidx.compose.ui.text.font.FontSynthesis.None,
    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding=false),
    lineHeightStyle = CenteredLineHeight,
)



val AppTypography = Typography(
    displayLarge = ezText(44f, 52f, NewsreaderFamily, FontWeight.W500),
    displayMedium = ezText(34f, 40f, NewsreaderFamily, FontWeight.W500),
    displaySmall = ezText(32f, 38f, NewsreaderFamily, FontWeight.W500),

    headlineLarge = ezText(26f, 35f, NewsreaderFamily, FontWeight.W400),
    headlineMedium = ezText(22f, 30f, NewsreaderFamily, FontWeight.W500),
    headlineSmall = ezText(18f, 26f, NewsreaderFamily, FontWeight.W500),

    titleLarge = ezText(17f, 24f, ManropeFamily, FontWeight.W500),
    titleMedium = ezText(15f, 22f, ManropeFamily, FontWeight.W500),
    titleSmall = ezText(13f, 19f, ManropeFamily, FontWeight.W500),

    bodyLarge = ezText(17f, 26f, ManropeFamily, FontWeight.W400),
    bodyMedium = ezText(15f, 22f, ManropeFamily, FontWeight.W400),
    bodySmall = ezText(13f, 18f, ManropeFamily, FontWeight.W400),

    labelLarge = ezText(14f, 20f, ManropeFamily, FontWeight.W500),
    labelMedium = ezText(12f, 18f, ManropeFamily, FontWeight.W400),
    labelSmall = ezText(11f, 16f, ManropeFamily, FontWeight.W400),
)


object EzText {

    
    val heroBadge = ezText(9f, 12f, weight = FontWeight.W500)

    
    val microLabel = ezText(10f, 14f, weight = FontWeight.W500)

    
    val chip = ezText(11f, 14f, weight = FontWeight.W600)

    
    val labelSm = ezText(11f, 14f, weight = FontWeight.W500)

    
    val meta = ezText(12f, 16f, weight = FontWeight.W600)

    
    val searchField = ezText(12.5f, 17f, weight = FontWeight.W400)

    
    val body13 = ezText(13f, 18f, weight = FontWeight.W400)

    
    val body14 = ezText(14f, 20f, weight = FontWeight.W600)

    
    val cardTitle = ezText(16f, 23f, NewsreaderFamily, FontWeight.W500)

    
    val cardTitleSm = ezText(13f, 17f, NewsreaderFamily, FontWeight.W600)

    
    val stepNumber = ezText(17f, 22f, NewsreaderItalicFamily, FontWeight.W400)

    
    val storyItalic = ezText(13f, 17f, NewsreaderItalicFamily, FontWeight.W400)

    
    val fieldValue = ezText(12f, 16f, weight = FontWeight.W600)

    
    val fieldText = ezText(13f, 18f, weight = FontWeight.W500)

    
    val titleInput = ezText(18f, 24f, NewsreaderFamily, FontWeight.W600)

    
    val timerDisplay = ezText(52f, 52f, NewsreaderThinFamily, FontWeight.W300)

    
    val statValue = ezText(18f, 24f, NewsreaderFamily, FontWeight.W600)

    
    val navLabel = ezText(10f, 14f, weight = FontWeight.W500)
}
