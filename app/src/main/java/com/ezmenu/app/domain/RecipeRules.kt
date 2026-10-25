package com.ezmenu.app.domain

import java.util.Locale

/** LIKE 的参数按用户输入的字面内容匹配，不把 % 和 _ 当成通配符。 */
fun searchPattern(query: String): String = "%" + query.trim()
    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"

fun initialServings(saved: Int?, preferred: Int): Int =
    ServingsRules.clampDetail(saved ?: preferred)

fun cookingStepIndex(requested: Int, count: Int): Int? =
    if (count <= 0) null else requested.coerceIn(0, count - 1)

data class SavedRecipeResult(val recipeId: Long, val isDraft: Boolean)

data class RecipeIngredients(val id: Long, val names: List<String>)

/** 本地、确定性的余料推荐；没有命中时不使用无关菜谱兜底。 */
object IngredientRecommendations {
    private fun normalize(value: String): String = value.trim().lowercase(Locale.ROOT)
        .replace("番茄", "西红柿").replace("嫩豆腐", "豆腐")

    fun rank(candidates: List<RecipeIngredients>, leftovers: List<String>): List<Long> {
        val selected = leftovers.map(::normalize).filter { it.isNotBlank() }.distinct()
        if (selected.isEmpty()) return emptyList()
        data class Score(val id: Long, val hits: Int, val missing: Int)
        return candidates.map { candidate ->
            val names = candidate.names.map(::normalize).filter { it.isNotBlank() }.distinct()
            fun matches(a: String, b: String) = a.contains(b) || b.contains(a)
            Score(
                candidate.id,
                selected.count { item -> names.any { matches(it, item) } },
                names.count { name -> selected.none { matches(name, it) } },
            )
        }.filter { it.hits > 0 }
            .sortedWith(compareByDescending<Score> { it.hits }.thenBy { it.missing }.thenBy { it.id })
            .map { it.id }
    }
}
