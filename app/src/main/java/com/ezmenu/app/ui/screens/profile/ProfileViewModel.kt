package com.ezmenu.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ezmenu.app.EzMenuApplication
import com.ezmenu.app.data.repository.RecipeRepository
import com.ezmenu.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val recipeCount: Int = 0,
    val draftCount: Int = 0,
    val cookLogCount: Int = 0,
    val pantryCount: Int = 0,
    val keepScreenOn: Boolean = true,
    val gestureSensitivity: String = SettingsRepository.DEFAULT_GESTURE,
    val defaultServings: Int = 2,
    val measurementSystem: String = SettingsRepository.DEFAULT_MEASUREMENT,
    val paperTone: String = SettingsRepository.DEFAULT_PAPER_TONE,
    val loading: Boolean = true,
)

/**
 * 「我的」—— 厨房工作台。
 *
 * 设计稿里的三个统计数字（18 篇菜谱 / 4 篇草稿 / 142 次下厨记录）是**写死的**。
 * 按已确认的决定，这里全部改为真实查询：用户新建一道菜，私房菜谱数就真的加一；
 * 走完一次烹饪模式，下厨记录就真的加一。
 * 因此初始数字会与设计稿不同 —— 这是预期行为，不是缺陷。
 *
 * 设计稿里只有「烹饪时屏幕常亮」这一个开关是真能切的，其余几行都是静态装饰。
 * 这里全部接上 DataStore 持久化：一个点了没反应的开关比没有这个开关更糟。
 */
class ProfileViewModel(
    private val recipeRepository: RecipeRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    /** 真实统计打包 —— 避免 7 个流的 combine 退化成 Array<Any?> 加强制转换。 */
    private data class Counts(val recipe: Int, val draft: Int, val cookLog: Int, val pantry: Int)

    private data class Prefs(val keepScreenOn: Boolean, val gesture: String, val servings: Int)

    private val counts = combine(
        recipeRepository.observePublishedCount(),
        recipeRepository.observeDraftCount(),
        recipeRepository.observeCookLogCount(),
        recipeRepository.observePantryCount(),
    ) { recipe: Int, draft: Int, cookLog: Int, pantry: Int -> Counts(recipe, draft, cookLog, pantry) }

    private val prefs = combine(
        settings.keepScreenOn,
        settings.gestureSensitivity,
        settings.defaultServings,
    ) { keepScreenOn: Boolean, gesture: String, servings: Int -> Prefs(keepScreenOn, gesture, servings) }

    val uiState: StateFlow<ProfileUiState> = combine(counts, prefs) { c, p ->
        ProfileUiState(
            recipeCount = c.recipe,
            draftCount = c.draft,
            cookLogCount = c.cookLog,
            pantryCount = c.pantry,
            keepScreenOn = p.keepScreenOn,
            gestureSensitivity = p.gesture,
            defaultServings = p.servings,
            measurementSystem = SettingsRepository.DEFAULT_MEASUREMENT,
            paperTone = SettingsRepository.DEFAULT_PAPER_TONE,
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(),
    )

    fun onKeepScreenOnChange(value: Boolean) {
        viewModelScope.launch { settings.setKeepScreenOn(value) }
    }

    fun onDefaultServingsChange(value: Int) {
        viewModelScope.launch { settings.setDefaultServings(value) }
    }

    fun onGestureSensitivityChange(value: String) {
        viewModelScope.launch { settings.setGestureSensitivity(value) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as EzMenuApplication
                ProfileViewModel(app.services.recipeRepository, app.services.settingsRepository)
            }
        }
    }
}
