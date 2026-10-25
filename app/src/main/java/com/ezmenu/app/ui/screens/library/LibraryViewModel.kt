package com.ezmenu.app.ui.screens.library

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
import kotlinx.coroutines.launch

/** 菜谱库的分类 chip，逐字取自设计稿。 */
val LIBRARY_CATEGORIES = listOf(
    "全部", "元气早餐", "轻卡减脂", "空气炸锅", "一人食记", "旬味暖炖",
)

data class LibraryUiState(
    val query: String = "",
    val category: String = "全部",
    /** 「旬味特选」大卡 */
    val featured: RecipeCard? = null,
    /** 「灵感餐单」网格 */
    val inspiration: List<RecipeCard> = emptyList(),
    val loading: Boolean = true,
)

/**
 * 菜谱库。
 *
 * 设计稿的这个界面里，分类 chip **只切换样式、不筛选**，搜索框也没有任何监听 ——
 * 那是个静态原型。这里把两者都接上真实查询：
 *   - 分类 chip → 按 `category` 字段查询
 *   - 搜索框 → 按标题 / 副标题 / **食材名**匹配（用户输入「三文鱼」时，
 *     期望命中的是把三文鱼列为食材的菜谱，而不只是标题里有这三个字的）
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LibraryViewModel(private val repository: RecipeRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val category = MutableStateFlow("全部")

    val uiState: StateFlow<LibraryUiState> = combine(
        query,
        category,
        query.flatMapLatest { repository.search(it) },
    ) { q, cat, all ->
        val filtered = all
            .filter { cat == "全部" || it.category == cat }


        // 「旬味特选」优先挑带 hero 角标的条目（设计稿里那张是「主厨甄选」），
        // 筛选后没有带角标的就退回第一条
        val featured = filtered.firstOrNull { it.heroBadge != null } ?: filtered.firstOrNull()

        LibraryUiState(
            query = q,
            category = cat,
            featured = featured,
            // 「灵感餐单」展示全部已发布菜谱（除特选那张）。
            // 没有封面的条目会渲染成排版式占位（暖调渐变 + 菜名首字），
            // 看起来是刻意的留白 —— 用户新建的菜谱因此也能立刻在首页看到。
            inspiration = filtered.filterNot { it.id == featured?.id },
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState(),
    )

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onCategorySelect(value: String) {
        category.value = value
    }

    fun toggleFavorite(recipeId: Long, favorite: Boolean) {
        viewModelScope.launch { repository.setFavorite(recipeId, favorite) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as EzMenuApplication
                LibraryViewModel(app.services.recipeRepository)
            }
        }
    }
}
