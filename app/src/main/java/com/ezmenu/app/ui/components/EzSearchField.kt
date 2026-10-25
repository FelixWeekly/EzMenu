package com.ezmenu.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ezmenu.app.ui.icons.MsGlyph
import com.ezmenu.app.ui.icons.MsIcon
import com.ezmenu.app.ui.theme.EzColors
import com.ezmenu.app.ui.theme.EzShapes
import com.ezmenu.app.ui.theme.EzText

/**
 * 胶囊搜索框。
 *
 * 两个界面用了不同高度：菜谱库 36dp、发现 44dp，所以高度做成参数。
 * 设计稿给菜谱库那一个配了麦克风按钮（语音识食材），发现页配的是清除按钮。
 *
 * Typography 走 Manrope —— 搜索框里的英文与数字应该和正文同一套。
 * 中文会按预期回退到系统无衬线，这正是设计要的（功能性文字用无衬线，
 * 只有标题才用衬线）。
 */
@Composable
fun EzSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    height: Dp = 36.dp,
    textStyle: TextStyle = EzText.searchField,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(EzShapes.pill)
            .background(EzColors.SurfaceContainerLowest)
            .border(1.dp, EzColors.SurfaceContainerHigh.copy(alpha = 0.6f), EzShapes.pill)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MsIcon(icon = MsGlyph.Search, size = 16.dp, tint = EzColors.Outline)

        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = textStyle,
                    color = EzColors.Outline.copy(alpha = 0.72f),
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = textStyle.copy(color = EzColors.OnSurface),
                cursorBrush = SolidColor(EzColors.Primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
        }

        trailing?.invoke()
    }
}

/** 搜索框里的清除按钮，有内容时才显示。 */
@Composable
fun SearchClearButton(visible: Boolean, onClick: () -> Unit) {
    if (!visible) return
    EzIconButton(onClick = onClick, size = 20.dp) {
        MsIcon(icon = MsGlyph.Cancel, size = 16.dp, tint = EzColors.Outline)
    }
}

/** 搜索框里的麦克风按钮（设计稿的「语音识食材」）。 */
@Composable
fun SearchMicButton(onClick: () -> Unit) {
    EzIconButton(onClick = onClick, size = 24.dp) {
        MsIcon(icon = MsGlyph.Mic, size = 15.dp, tint = EzColors.Outline)
    }
}
