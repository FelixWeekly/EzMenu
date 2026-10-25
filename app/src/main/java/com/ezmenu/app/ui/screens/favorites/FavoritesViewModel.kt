package com.ezmenu.app.ui.screens.favorites

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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 一个筛选 pill：名字 + 真实计数。 */
data class FilterPill(val name: String, val count: Int)

data class FavoritesUiState(
    val selectedFilter: String = ALL,
    val pills: List<FilterPill> = emptyList(),
    val totalCount: Int = 0,
    /** 杂志式排布：一张大图 + 两列方图 + 若干横排卡 */
    val hero: RecipeCard? = null,
    val gridCards: List<RecipeCard> = emptyList(),
    val rowCards: List<RecipeCard> = emptyList(),
    val loading: Boolean = true,
) {
    companion object {
        const val ALL = "全部"
    }
}

/**
 * 珍藏页。
 *
 * 设计稿这个界面**完全没有 JavaScript**，筛选 pill 是纯静态的。
 * 这里把筛选接上真实数据：pill 从收藏菜谱实际的标签里推导，
 * 计数是真实统计，点一下真的会筛。
 *
 * 概览胶囊的数字同样来自真实查询 —— 用户收藏得多了，数字就真的变。
 */
class FavoritesViewModel(private val repository: RecipeRepository) : ViewModel() {

    private val filter = MutableStateFlow(FavoritesUiState.ALL)

    val uiState: StateFlow<FavoritesUiState> = combine(
        filter,
        repository.observeFavorites(),
    ) { selected, all ->
        val pills = buildPills(all)

        val visible = if (selected == FavoritesUiState.ALL) {
            all
        } else {
            all.filter { card -> card.tags.any { it.name == selected } }
        }

        FavoritesUiState(
            selectedFilter = selected,
            pills = pills,
            totalCount = all.size,
            // 杂志式排布：第一张做大图，接着两张进两列网格，其余走横排
            hero = visible.getOrNull(0),
            gridCards = visible.drop(1).take(2),
            rowCards = visible.drop(3),
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FavoritesUiState(),
    )

    fun onFilterSelect(name: String) {
        filter.value = name
    }

    fun toggleFavorite(recipeId: Long, favorite: Boolean) {
        viewModelScope.launch { repository.setFavorite(recipeId, favorite) }
    }

    /**
     * 从收藏菜谱的标签里推导筛选 pill。
     *
     * 只保留出现 1 次以上的标签，并按出现次数排序 —— 全是只出现一次的标签会让
     * 筛选栏变成一串没有区分度的按钮。名字与计数都来自真实数据，
     * 不像设计稿那样写死成「快手便当 (8)」。
     */
    private fun buildPills(cards: List<RecipeCard>): List<FilterPill> {
        val counts = linkedMapOf<String, Int>()
        cards.forEach { card ->
            card.tags.forEach { tag ->
                counts[tag.name] = (counts[tag.name] ?: 0) + 1
            }
        }
        val ranked = counts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(5)
            .map { FilterPill(it.key, it.value) }

        return listOf(FilterPill(FavoritesUiState.ALL, cards.size)) + ranked
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as EzMenuApplication
                FavoritesViewModel(app.services.recipeRepository)
            }
        }
    }
}
