package com.ezmenu.app.ui.dev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ezmenu.app.ui.theme.EzColors
import com.ezmenu.app.ui.theme.EzMenuTheme

/**
 * 设计系统核对屏的入口，**仅存在于 debug 构建**。
 *
 * 应用本身不引用它，需要时用 adb 直接拉起：
 * ```
 * adb shell am start -n com.ezmenu.app/.ui.dev.SpecimenActivity
 * ```
 *
 * 它存在的价值是当回归护栏：字体合并、图标子集化、可变字重这几件事一旦被改坏，
 * 表现往往是「看着还行但其实不对」（比如粗体变成了合成描边、中文悄悄回退成黑体），
 * 只有把这屏拉出来看一眼才能立刻发现。
 */
class SpecimenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            EzMenuTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = EzColors.Surface) {
                    DesignSpecimenScreen()
                }
            }
        }
    }
}
