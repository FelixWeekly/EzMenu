package com.ezmenu.app.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ezmenu.app.EzMenuApplication
import com.ezmenu.app.data.repository.RecipeRepository
import com.ezmenu.app.data.repository.SettingsRepository
import com.ezmenu.app.domain.initialServings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import com.ezmenu.app.domain.ServingsRules
import com.ezmenu.app.domain.model.RecipeDetail
import com.ezmenu.app.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val detail: RecipeDetail? = null,
    val servings: Int = 2,
    val canDecrement: Boolean = false,
    val canIncrement: Boolean = true,
    /** 已勾选的食材 id —— 做饭时用来核对备料进度 */
    val checkedIngredientIds: Set<Long> = emptySet(),
    val loading: Boolean = true,
)

/**
 * 菜谱详情。
 *
 * 份量换算在这里真正生效：改动份量会重新渲染每一项食材的用量
 * （「2块 (约350g)」→「1块 (约175g)」），换算规则在 [com.ezmenu.app.domain.QuantityFormatter]
 * 里，有完整的单元测试。
 *
 * 已勾选的食材**只存在内存里**，不落库 —— 那是一次做饭过程中的临时进度，
 * 不是菜谱本身的属性。退出这屏再进来应该重新开始。
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RecipeDetailViewModel(
    private val repository: RecipeRepository,
    private val savedStateHandle: SavedStateHandle,
    settings: SettingsRepository,
) : ViewModel() {

    private val recipeId: Long = savedStateHandle[Routes.ARG_RECIPE_ID] ?: 0L
    private val servings = MutableStateFlow<Int?>(savedStateHandle.get<Int>("selectedServings"))

    init {
        viewModelScope.launch {
            if (servings.value == null) {
                servings.value = initialServings(null, settings.defaultServings.first())
                savedStateHandle["selectedServings"] = servings.value
            }
        }
    }
    private val checked = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<DetailUiState> = servings.filterNotNull().flatMapLatest { n ->
        combine(checked, repository.observeDetail(recipeId, n)) { checkedIds, detail ->
            DetailUiState(detail, n,
                ServingsRules.canDecrement(n, ServingsRules.DETAIL_RANGE),
                ServingsRules.canIncrement(n, ServingsRules.DETAIL_RANGE),
                checkedIds, loading = false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun onServingsDelta(delta: Int) {
        val current = servings.value ?: return
        servings.value = ServingsRules.clampDetail(current + delta)
        savedStateHandle["selectedServings"] = servings.value
    }

    fun onToggleIngredient(id: Long) {
        checked.value = if (id in checked.value) checked.value - id else checked.value + id
    }

    fun onToggleFavorite() {
        val current = uiState.value.detail?.isFavorite ?: return
        viewModelScope.launch { repository.setFavorite(recipeId, !current) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as EzMenuApplication
                RecipeDetailViewModel(app.services.recipeRepository, createSavedStateHandle(), app.services.settingsRepository)
            }
        }
    }
}
