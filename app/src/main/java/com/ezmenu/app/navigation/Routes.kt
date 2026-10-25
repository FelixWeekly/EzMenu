package com.ezmenu.app.navigation

import com.ezmenu.app.ui.components.EzTab

/**
 * 导航路由。
 *
 * 用字符串路由而不是类型安全的 `@Serializable` 路由：后者要额外引入
 * kotlin-serialization 插件与运行时依赖，而这里的路由参数只有一个 Long 和一个 Int，
 * 收益抵不上多出来的依赖面。
 */
object Routes {
    val LIBRARY = EzTab.LIBRARY.route
    val DISCOVER = EzTab.DISCOVER.route
    val FAVORITES = EzTab.FAVORITES.route
    val PROFILE = EzTab.PROFILE.route

    /** 四个 Tab 的集合 —— 底部导航栏只在这些路由上显示。 */
    val TABS = EzTab.entries.map { it.route }.toSet()

    const val DRAFTS = "drafts"

    const val DETAIL_PATTERN = "recipe/{recipeId}"
    const val EDITOR_NEW = "recipe/new"
    const val EDITOR_PATTERN = "recipe/{recipeId}/edit"
    const val COOKING_PATTERN = "recipe/{recipeId}/cook"

    const val ARG_RECIPE_ID = "recipeId"
    const val ARG_START_STEP = "step"
    const val ARG_SERVINGS = "servings"

    fun detail(recipeId: Long) = "recipe/$recipeId"

    fun editor(recipeId: Long?) = if (recipeId == null) EDITOR_NEW else "recipe/$recipeId/edit"

    /** @param startStep 从第几步开始引导（0 基），详情页点某个步骤的计时器时用得上 */
    fun cooking(recipeId: Long, startStep: Int = 0, servings: Int? = null) =
        "recipe/$recipeId/cook?step=$startStep" + (servings?.let { "&servings=$it" } ?: "")
}
