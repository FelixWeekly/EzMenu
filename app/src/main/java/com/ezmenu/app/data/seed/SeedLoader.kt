package com.ezmenu.app.data.seed

import android.content.Context
import com.ezmenu.app.data.local.entity.EditorialMeta
import com.ezmenu.app.data.local.entity.IngredientEntity
import com.ezmenu.app.data.local.entity.IngredientGroupEntity
import com.ezmenu.app.data.local.entity.PantryItemEntity
import com.ezmenu.app.data.local.entity.RecipeEntity
import com.ezmenu.app.data.local.entity.StepEntity
import com.ezmenu.app.data.local.entity.TagKind
import com.ezmenu.app.data.local.dao.TagTriple
import org.json.JSONArray
import org.json.JSONObject

/** 一个待写入的菜谱全图。 */
data class SeedRecipe(
    val recipe: RecipeEntity,
    val groups: List<IngredientGroupEntity>,
    val ingredientsByGroup: List<List<IngredientEntity>>,
    val steps: List<StepEntity>,
    val tags: List<TagTriple>,
)

/** 首启预置数据的完整载荷。 */
data class SeedPayload(
    val recipes: List<SeedRecipe>,
    val pantry: List<PantryItemEntity>,
)

/**
 * 解析 `assets/seed/recipes.json`。
 *
 * 用 Android 内置的 `org.json` 而不是 kotlinx.serialization —— 这样不必再引入
 * 序列化 Gradle 插件与运行时依赖，而这个文件的 schema 简单且完全由我们掌控。
 */
object SeedLoader {

    private const val ASSET = "seed/recipes.json"

    fun load(context: Context): SeedPayload {
        val text = context.assets.open(ASSET).bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        return SeedPayload(
            recipes = root.getJSONArray("recipes").mapObjects(::parseRecipe),
            pantry = root.optJSONArray("pantry")?.mapObjects(::parsePantry) ?: emptyList(),
        )
    }

    private fun parseRecipe(json: JSONObject): SeedRecipe {
        val title = json.getString("title")
        val now = System.currentTimeMillis()

        val recipe = RecipeEntity(
            title = title,
            subtitle = json.optString("subtitle"),
            story = json.optString("story"),
            coverImageRef = json.optStringOrNull("cover"),
            cookTimeMinutes = json.optInt("cookTimeMinutes", 0),
            baseServings = json.optInt("baseServings", 2),
            difficulty = json.optString("difficulty"),
            caloriesKcal = json.optIntOrNull("calories"),
            author = json.optStringOrNull("author"),
            category = json.optString("category", "全部"),
            heroBadge = json.optStringOrNull("heroBadge"),
            heroKicker = json.optStringOrNull("heroKicker"),
            heroNote = json.optStringOrNull("heroNote"),
            heroCta = json.optStringOrNull("heroCta"),
            footerNote = json.optStringOrNull("footerNote"),
            equipment = json.optStringOrNull("equipment"),
            isFavorite = json.optBoolean("isFavorite", false),
            isDraft = json.optBoolean("isDraft", false),
            cookCount = json.optInt("cookCount", 0),
            createdAt = now,
            updatedAt = now,
            editorial = json.optJSONObject("editorial")?.let { e ->
                EditorialMeta(
                    chefTipTitle = e.optStringOrNull("chefTipTitle"),
                    chefTipBody = e.optStringOrNull("chefTipBody"),
                    heatNote = e.optStringOrNull("heatNote"),
                    attentionNote = e.optStringOrNull("attentionNote"),
                    seasonalNote = e.optStringOrNull("seasonalNote"),
                )
            } ?: EditorialMeta(),
        )

        val groupObjects = json.optJSONArray("groups") ?: JSONArray()
        val groups = mutableListOf<IngredientGroupEntity>()
        val ingredientsByGroup = mutableListOf<List<IngredientEntity>>()

        for (i in 0 until groupObjects.length()) {
            val g = groupObjects.getJSONObject(i)
            groups += IngredientGroupEntity(recipeId = 0, name = g.getString("name"), sortOrder = i)

            val rows = g.optJSONArray("ingredients") ?: JSONArray()
            ingredientsByGroup += rows.mapObjectsIndexed { index, ing ->
                IngredientEntity(
                    groupId = 0,
                    recipeId = 0,
                    name = ing.getString("name"),
                    baseAmount = ing.optDoubleOrNull("base"),
                    unitLabel = ing.optString("unit"),
                    gramsPerUnit = ing.optDoubleOrNull("gramsPerUnit"),
                    rawDisplay = ing.optStringOrNull("raw"),
                    sortOrder = index,
                )
            }
        }

        val steps = (json.optJSONArray("steps") ?: JSONArray()).mapObjectsIndexed { index, s ->
            StepEntity(
                recipeId = 0,
                sortOrder = index,
                title = s.optString("title"),
                body = s.optString("body"),
                imageRef = s.optStringOrNull("image"),
                chipLabel = s.optStringOrNull("chipLabel"),
                timerSeconds = s.optIntOrNull("timerSeconds"),
                timerLabel = s.optStringOrNull("timerLabel"),
                noteText = s.optStringOrNull("noteText"),
                bulletsJson = s.optJSONArray("bullets")?.toJsonString(),
                materialChipsJson = s.optJSONArray("materials")?.toJsonString(),
            )
        }

        val tags = (json.optJSONArray("tags") ?: JSONArray()).mapObjects { t ->
            TagTriple(
                name = t.getString("name"),
                kind = runCatching { TagKind.valueOf(t.optString("kind")) }.getOrDefault(TagKind.DIET),
            )
        }

        return SeedRecipe(recipe, groups, ingredientsByGroup, steps, tags)
    }

    private fun parsePantry(json: JSONObject) = PantryItemEntity(
        name = json.getString("name"),
        note = json.optStringOrNull("note"),
        addedAt = System.currentTimeMillis(),
    )
}

// ---------------------------------------------------------------- JSON 小工具

private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    (0 until length()).map { transform(getJSONObject(it)) }

private inline fun <T> JSONArray.mapObjectsIndexed(transform: (Int, JSONObject) -> T): List<T> =
    (0 until length()).map { transform(it, getJSONObject(it)) }

/** `optString` 在键缺失时会返回空串，这里统一成 `null` 以便区分「没有」和「空值」。 */
private fun JSONObject.optStringOrNull(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

private fun JSONObject.optIntOrNull(key: String): Int? =
    if (!has(key) || isNull(key)) null else optInt(key)

private fun JSONObject.optDoubleOrNull(key: String): Double? =
    if (!has(key) || isNull(key)) null else optDouble(key)

/** 把 `["a","b"]` 原样转成 JSON 字符串存库（对应实体的 `*Json` 字段）。 */
private fun JSONArray.toJsonString(): String = toString()
