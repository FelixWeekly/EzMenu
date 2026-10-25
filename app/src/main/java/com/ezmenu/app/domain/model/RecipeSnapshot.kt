package com.ezmenu.app.domain.model

import com.ezmenu.app.data.local.entity.*
import com.ezmenu.app.data.local.relation.RecipeWithDetails
import org.json.JSONArray
import org.json.JSONObject

/** Immutable, versioned recipe at the moment cooking begins. Numeric quantities stay raw. */
data class RecipeSnapshot(val graph: RecipeWithDetails, val servings: Int, val version: Int = 1) {
    fun encode(): String = JSONObject().apply {
        put("version", version); put("servings", servings)
        put("recipe", RecipeJson.encode(graph.recipe))
        put("groups", JSONArray(graph.groups.map { RecipeJson.encode(it) }))
        put("ingredients", JSONArray(graph.ingredients.map { RecipeJson.encode(it) }))
        put("steps", JSONArray(graph.steps.map { RecipeJson.encode(it) }))
        put("tags", JSONArray(graph.tags.map { RecipeJson.encode(it) }))
        put("tagRefs", JSONArray(graph.tagRefs.map { RecipeJson.encode(it) }))
    }.toString()
    companion object {
        fun decode(value: String): RecipeSnapshot {
            val o = JSONObject(value)
            require(o.getInt("version") == 1) { "不支持的配方版本" }
            fun <T> rows(key: String, decode: (JSONObject) -> T): List<T> {
                val a = o.getJSONArray(key)
                return (0 until a.length()).map { decode(a.getJSONObject(it)) }
            }
            return RecipeSnapshot(RecipeWithDetails(
                recipe = RecipeJson.decodeRecipeEntity(o.getJSONObject("recipe")),
                groups = rows("groups", RecipeJson::decodeIngredientGroupEntity),
                ingredients = rows("ingredients", RecipeJson::decodeIngredientEntity),
                steps = rows("steps", RecipeJson::decodeStepEntity),
                tags = rows("tags", RecipeJson::decodeTagEntity),
                tagRefs = rows("tagRefs", RecipeJson::decodeRecipeTagCrossRef),
            ), o.getInt("servings"))
        }
    }
}

object RecipeJson {
    fun encode(v: RecipeEntity): JSONObject = JSONObject().apply {
        put("id", v.id)
        put("title", v.title)
        put("subtitle", v.subtitle)
        put("story", v.story)
        put("coverImageRef", v.coverImageRef ?: JSONObject.NULL)
        put("cookTimeMinutes", v.cookTimeMinutes)
        put("baseServings", v.baseServings)
        put("difficulty", v.difficulty)
        put("caloriesKcal", v.caloriesKcal ?: JSONObject.NULL)
        put("author", v.author ?: JSONObject.NULL)
        put("category", v.category)
        put("heroBadge", v.heroBadge ?: JSONObject.NULL)
        put("heroKicker", v.heroKicker ?: JSONObject.NULL)
        put("heroNote", v.heroNote ?: JSONObject.NULL)
        put("heroCta", v.heroCta ?: JSONObject.NULL)
        put("footerNote", v.footerNote ?: JSONObject.NULL)
        put("equipment", v.equipment ?: JSONObject.NULL)
        put("isFavorite", v.isFavorite)
        put("isDraft", v.isDraft)
        put("cookCount", v.cookCount)
        put("createdAt", v.createdAt)
        put("updatedAt", v.updatedAt)
        put("chefTipTitle", v.editorial.chefTipTitle ?: JSONObject.NULL)
        put("chefTipBody", v.editorial.chefTipBody ?: JSONObject.NULL)
        put("heatNote", v.editorial.heatNote ?: JSONObject.NULL)
        put("attentionNote", v.editorial.attentionNote ?: JSONObject.NULL)
        put("seasonalNote", v.editorial.seasonalNote ?: JSONObject.NULL)
        put("origin", v.origin)
        put("sourceKey", v.sourceKey ?: JSONObject.NULL)
        put("trashedAt", v.trashedAt ?: JSONObject.NULL)
    }
    fun decodeRecipeEntity(o: JSONObject) = RecipeEntity(
        id = o.getLong("id"),
        title = o.getString("title"),
        subtitle = o.getString("subtitle"),
        story = o.getString("story"),
        coverImageRef = if (o.isNull("coverImageRef")) null else o.getString("coverImageRef"),
        cookTimeMinutes = o.getInt("cookTimeMinutes"),
        baseServings = o.getInt("baseServings"),
        difficulty = o.getString("difficulty"),
        caloriesKcal = if (o.isNull("caloriesKcal")) null else o.getInt("caloriesKcal"),
        author = if (o.isNull("author")) null else o.getString("author"),
        category = o.getString("category"),
        heroBadge = if (o.isNull("heroBadge")) null else o.getString("heroBadge"),
        heroKicker = if (o.isNull("heroKicker")) null else o.getString("heroKicker"),
        heroNote = if (o.isNull("heroNote")) null else o.getString("heroNote"),
        heroCta = if (o.isNull("heroCta")) null else o.getString("heroCta"),
        footerNote = if (o.isNull("footerNote")) null else o.getString("footerNote"),
        equipment = if (o.isNull("equipment")) null else o.getString("equipment"),
        isFavorite = o.getBoolean("isFavorite"),
        isDraft = o.getBoolean("isDraft"),
        cookCount = o.getInt("cookCount"),
        createdAt = o.getLong("createdAt"),
        updatedAt = o.getLong("updatedAt"),
        origin = o.getString("origin"),
        sourceKey = if (o.isNull("sourceKey")) null else o.getString("sourceKey"),
        trashedAt = if (o.isNull("trashedAt")) null else o.getLong("trashedAt"),
        editorial = EditorialMeta(
            chefTipTitle = if (o.isNull("chefTipTitle")) null else o.getString("chefTipTitle"),
            chefTipBody = if (o.isNull("chefTipBody")) null else o.getString("chefTipBody"),
            heatNote = if (o.isNull("heatNote")) null else o.getString("heatNote"),
            attentionNote = if (o.isNull("attentionNote")) null else o.getString("attentionNote"),
            seasonalNote = if (o.isNull("seasonalNote")) null else o.getString("seasonalNote"),
        ),
    )

    fun encode(v: IngredientGroupEntity): JSONObject = JSONObject().apply {
        put("id", v.id)
        put("recipeId", v.recipeId)
        put("name", v.name)
        put("sortOrder", v.sortOrder)
    }
    fun decodeIngredientGroupEntity(o: JSONObject) = IngredientGroupEntity(
        id = o.getLong("id"),
        recipeId = o.getLong("recipeId"),
        name = o.getString("name"),
        sortOrder = o.getInt("sortOrder"),
    )

    fun encode(v: IngredientEntity): JSONObject = JSONObject().apply {
        put("id", v.id)
        put("groupId", v.groupId)
        put("recipeId", v.recipeId)
        put("name", v.name)
        put("baseAmount", v.baseAmount ?: JSONObject.NULL)
        put("unitLabel", v.unitLabel)
        put("gramsPerUnit", v.gramsPerUnit ?: JSONObject.NULL)
        put("rawDisplay", v.rawDisplay ?: JSONObject.NULL)
        put("sortOrder", v.sortOrder)
    }
    fun decodeIngredientEntity(o: JSONObject) = IngredientEntity(
        id = o.getLong("id"),
        groupId = o.getLong("groupId"),
        recipeId = o.getLong("recipeId"),
        name = o.getString("name"),
        baseAmount = if (o.isNull("baseAmount")) null else o.getDouble("baseAmount"),
        unitLabel = o.getString("unitLabel"),
        gramsPerUnit = if (o.isNull("gramsPerUnit")) null else o.getDouble("gramsPerUnit"),
        rawDisplay = if (o.isNull("rawDisplay")) null else o.getString("rawDisplay"),
        sortOrder = o.getInt("sortOrder"),
    )

    fun encode(v: StepEntity): JSONObject = JSONObject().apply {
        put("id", v.id)
        put("recipeId", v.recipeId)
        put("sortOrder", v.sortOrder)
        put("title", v.title)
        put("body", v.body)
        put("imageRef", v.imageRef ?: JSONObject.NULL)
        put("chipLabel", v.chipLabel ?: JSONObject.NULL)
        put("timerSeconds", v.timerSeconds ?: JSONObject.NULL)
        put("timerLabel", v.timerLabel ?: JSONObject.NULL)
        put("noteText", v.noteText ?: JSONObject.NULL)
        put("bulletsJson", v.bulletsJson ?: JSONObject.NULL)
        put("materialChipsJson", v.materialChipsJson ?: JSONObject.NULL)
    }
    fun decodeStepEntity(o: JSONObject) = StepEntity(
        id = o.getLong("id"),
        recipeId = o.getLong("recipeId"),
        sortOrder = o.getInt("sortOrder"),
        title = o.getString("title"),
        body = o.getString("body"),
        imageRef = if (o.isNull("imageRef")) null else o.getString("imageRef"),
        chipLabel = if (o.isNull("chipLabel")) null else o.getString("chipLabel"),
        timerSeconds = if (o.isNull("timerSeconds")) null else o.getInt("timerSeconds"),
        timerLabel = if (o.isNull("timerLabel")) null else o.getString("timerLabel"),
        noteText = if (o.isNull("noteText")) null else o.getString("noteText"),
        bulletsJson = if (o.isNull("bulletsJson")) null else o.getString("bulletsJson"),
        materialChipsJson = if (o.isNull("materialChipsJson")) null else o.getString("materialChipsJson"),
    )

    fun encode(v: TagEntity): JSONObject = JSONObject().apply {
        put("id", v.id)
        put("name", v.name)
        put("kind", v.kind)
        put("sortOrder", v.sortOrder)
    }
    fun decodeTagEntity(o: JSONObject) = TagEntity(
        id = o.getLong("id"),
        name = o.getString("name"),
        kind = o.getString("kind"),
        sortOrder = o.getInt("sortOrder"),
    )

    fun encode(v: RecipeTagCrossRef): JSONObject = JSONObject().apply {
        put("recipeId", v.recipeId)
        put("tagId", v.tagId)
        put("sortOrder", v.sortOrder)
    }
    fun decodeRecipeTagCrossRef(o: JSONObject) = RecipeTagCrossRef(
        recipeId = o.getLong("recipeId"),
        tagId = o.getLong("tagId"),
        sortOrder = o.getInt("sortOrder"),
    )
}
