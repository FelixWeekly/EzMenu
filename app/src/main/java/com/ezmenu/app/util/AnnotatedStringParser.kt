package com.ezmenu.app.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * 把 `**这样**` 包起来的片段渲染成强调样式。
 *
 * 烹饪模式的步骤说明里，关键数值与动作（「15ml 橄榄油」「5秒」「03:00」）都用
 * 主色加粗标出 —— 做饭时手忙脚乱，一眼扫到该看哪里很重要。
 * 设计稿里这些是写死在 HTML 里的 `<span class="font-semibold text-primary">`，
 * 存进数据库时改用轻量标记，渲染时再转成 [AnnotatedString]。
 */
object RichText {

    private const val MARKER = "**"

    fun parse(
        text: String,
        accentColor: Color,
        accentWeight: FontWeight = FontWeight.W600,
    ): AnnotatedString {
        if (!text.contains(MARKER)) return AnnotatedString(text)

        return buildAnnotatedString {
            var index = 0
            while (index < text.length) {
                val start = text.indexOf(MARKER, index)
                if (start < 0) {
                    // 没有更多标记了，剩下的原样输出
                    append(text.substring(index))
                    break
                }
                val end = text.indexOf(MARKER, start + MARKER.length)
                if (end < 0) {
                    // 落单的标记（用户手滑多打一个），当普通字符处理，不要把后半句吞掉
                    append(text.substring(index))
                    break
                }
                append(text.substring(index, start))
                withStyle(SpanStyle(color = accentColor, fontWeight = accentWeight)) {
                    append(text.substring(start + MARKER.length, end))
                }
                index = end + MARKER.length
            }
        }
    }
}
