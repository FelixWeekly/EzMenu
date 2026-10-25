package com.ezmenu.app.ui.screens.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ezmenu.app.EzMenuApplication
import com.ezmenu.app.data.repository.RecipeRepository
import com.ezmenu.app.domain.model.RecipeCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class DiscoverUiState(
    val query: String = "",
    /** 冰箱里现有的食材，可逐个移除 */
    val leftovers: List<String> = DEFAULT_LEFTOVERS,
    /** 依据余料推荐的菜谱（设计稿里是「番茄浓汤炖嫩豆腐」「无油西红柿炒蛋」） */
    val suggestions: List<RecipeCard> = emptyList(),
    /** 时令风味两张卡（腌笃鲜 / 香煎芦笋罗勒暖沙拉） */
    val seasonal: List<RecipeCard> = emptyList(),
    val searchResults: List<RecipeCard> = emptyList(),
    val loading: Boolean = true,
) {
    companion object {
        /** 设计稿默认展示的三样：鸡蛋 / 西红柿 / 嫩豆腐 */
        val DEFAULT_LEFTOVERS = listOf("鸡蛋", "西红柿", "嫩豆腐")

        /** 「换一组」按钮给出的另一组常见余料 */
        val ALTERNATE_LEFTOVERS = listOf("鸡蛋", "豆腐", "吐司", "牛腩")

        /** 时令风味固定展示这两道应季菜 */
        val SEASONAL_TITLES = listOf("腌笃鲜 · 咸香春鲜", "香煎芦笋罗勒暖沙拉")


    }
}

/**
 * 发现页。
 *
 * 设计稿这个界面里：搜索框没绑 `oninput`、清除按钮只有 `setQuery()` 能唤醒、
 * `switchSearchMode` 是引用了不存在元素的死代码。这里把搜索真正接上：
 * 输入即筛选（匹配标题、副标题与**食材名**），清空即回到灵感区块。
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DiscoverViewModel(private val repository: RecipeRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val leftovers = MutableStateFlow(DiscoverUiState.DEFAULT_LEFTOVERS)
    private var shuffleIndex = 0

    val uiState: StateFlow<DiscoverUiState> = combine(
        query,
        leftovers,
        repository.observeLibrary(),
        query.flatMapLatest { repository.search(it) },
        leftovers.flatMapLatest { repository.observeSuggestions(it) },
    ) { q, fridge, all, results, suggestions ->

        DiscoverUiState(
            query = q,
            leftovers = fridge,
            suggestions = suggestions,
            seasonal = DiscoverUiState.SEASONAL_TITLES
                .mapNotNull { title -> all.firstOrNull { it.title == title } },
            searchResults = results,
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiscoverUiState(),
    )

    fun onQueryChange(value: String) {
        query.value = value
    }

    /** 点场景方格时把该场景的关键词填进搜索框，让用户看到「点一下就有结果」。 */
    fun onScenarioSelect(keyword: String) {
        query.value = keyword
    }

    fun onShuffleLeftovers() {
        // 在两组常见余料之间轮换，让「换一组」真的有变化
        shuffleIndex = (shuffleIndex + 1) % 2
        leftovers.value = if (shuffleIndex == 0) {
            DiscoverUiState.DEFAULT_LEFTOVERS
        } else {
            DiscoverUiState.ALTERNATE_LEFTOVERS
        }
    }

    fun onRemoveLeftover(item: String) {
        leftovers.value = leftovers.value - item
    }

    fun onAddLeftover() {
        val pool = (DiscoverUiState.ALTERNATE_LEFTOVERS + DiscoverUiState.DEFAULT_LEFTOVERS).distinct()
        val next = pool.firstOrNull { it !in leftovers.value } ?: return
        leftovers.value = leftovers.value + next
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as EzMenuApplication
                DiscoverViewModel(app.services.recipeRepository)
            }
        }
    }
}

