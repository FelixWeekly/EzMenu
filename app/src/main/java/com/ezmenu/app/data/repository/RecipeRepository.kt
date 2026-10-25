package com.ezmenu.app.data.repository

import com.ezmenu.app.data.local.dao.RecipeDao
import com.ezmenu.app.data.local.dao.TagTriple
import com.ezmenu.app.data.local.entity.IngredientEntity
import com.ezmenu.app.data.local.entity.IngredientGroupEntity
import com.ezmenu.app.data.local.entity.RecipeEntity
import com.ezmenu.app.data.local.entity.StepEntity
import com.ezmenu.app.data.local.relation.RecipeWithDetails
import com.ezmenu.app.domain.IngredientAmount
import com.ezmenu.app.domain.QuantityFormatter
import com.ezmenu.app.domain.searchPattern
import com.ezmenu.app.domain.IngredientRecommendations
import com.ezmenu.app.domain.RecipeIngredients
import com.ezmenu.app.domain.SavedRecipeResult
import com.ezmenu.app.domain.model.IngredientGroupView
import com.ezmenu.app.domain.model.IngredientView
import com.ezmenu.app.domain.model.RecipeCard
import com.ezmenu.app.domain.model.RecipeDetail
import com.ezmenu.app.domain.model.StepView
import com.ezmenu.app.domain.model.bulletsList
import com.ezmenu.app.domain.model.cookTimeLabel
import com.ezmenu.app.domain.model.materialsList
import com.ezmenu.app.domain.model.toCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 菜谱数据的唯一入口。
 *
 * 界面层只认这里的 [RecipeCard] / [RecipeDetail]，不直接碰 Room 实体 ——
 * 这样份量换算、时长文案这类派生逻辑都只在一处发生。
 */
class RecipeRepository(val dao: RecipeDao, val journalDao: com.ezmenu.app.data.local.dao.JournalDao? = null) {
    fun browse(query: String = "", scope: Int = 1, mode: String = "all", category: String = "", sort: String = "updated", limit: Int = 40) = dao.browse(searchPattern(query),scope,mode,category,sort,limit)


    fun observeLibrary(): Flow<List<RecipeCard>> =
        dao.observePublished().map { list -> list.map { it.toCard() } }

    fun observeFavorites(): Flow<List<RecipeCard>> =
        dao.observeFavorites().map { list -> list.map { it.toCard() } }

    fun observeDrafts(): Flow<List<RecipeCard>> =
        dao.observeDrafts().map { list -> list.map { it.toCard() } }

    fun search(query: String): Flow<List<RecipeCard>> =
        dao.search(searchPattern(query)).map { list -> list.map { it.toCard() } }

    /** 编辑读取原始数值，不能使用已按目标份量换算的详情投影。 */
    suspend fun getForEditing(recipeId: Long): RecipeWithDetails? = dao.getById(recipeId)

    fun observeSuggestions(leftovers: List<String>): Flow<List<RecipeCard>> =
        dao.observePublished().map { recipes ->
            val ids = IngredientRecommendations.rank(
                recipes.map { RecipeIngredients(it.recipe.id, it.ingredients.map { row -> row.name }) },
                leftovers,
            )
            val byId = recipes.associateBy { it.recipe.id }
            ids.take(2).mapNotNull { byId[it]?.toCard() }
        }

    /** 目标准备份量由界面传入；换算后的用量文案在这里一次算好。 */
    fun observeDetail(recipeId: Long, targetServings: Int): Flow<RecipeDetail?> =
        dao.observeById(recipeId).map { it?.toDetail(targetServings) }

    suspend fun getDetailOnce(recipeId: Long, targetServings: Int): RecipeDetail? =
        dao.getById(recipeId)?.toDetail(targetServings)

    suspend fun setFavorite(recipeId: Long, favorite: Boolean) = dao.setFavorite(recipeId, favorite)

    suspend fun recordCook(recipeId: Long, servings: Int) = dao.recordCook(recipeId, servings)

    suspend fun delete(recipeId: Long) = dao.deleteRecipe(recipeId)

    /**
     * 保存编辑器产出的完整菜谱图。
     *
     * 走 DAO 的整图事务：子表先清后插，中途失败不会留下「有食材没步骤」的残缺菜谱。
     *
     * @return 菜谱 id（新建时为新生成的 id）
     */
    suspend fun saveEdited(
        recipe: RecipeEntity,
        groups: List<IngredientGroupEntity>,
        ingredientsByGroup: List<List<IngredientEntity>>,
        steps: List<StepEntity>,
        tags: List<TagTriple>,
        editMetadata: Boolean = false,
    ): SavedRecipeResult = SavedRecipeResult(dao.saveEditedGraph(
        recipe = recipe,
        groups = groups,
        ingredientsByGroup = ingredientsByGroup,
        steps = steps,
        tags = tags,
        editMetadata = editMetadata,
    ), recipe.isDraft)

    // ------------------------------------------------------------ 统计（真实查询）

    fun observePublishedCount(): Flow<Int> = dao.observePublishedCount()
    fun observeDraftCount(): Flow<Int> = dao.observeDraftCount()
    fun observeCookLogCount(): Flow<Int> = dao.observeCookLogCount()
    fun observeFavoriteCount(): Flow<Int> = dao.observeFavoriteCount()
    fun observeCategoryCount(category: String): Flow<Int> = dao.observeCountByCategory(category)

    fun observePantryCount(): Flow<Int> = dao.observePantryCount()

    // ------------------------------------------------------------ 映射

    private fun RecipeWithDetails.toCard(): RecipeCard = recipe.toCard(orderedTags)

    fun detailFromGraph(graph: RecipeWithDetails, targetServings: Int): RecipeDetail = graph.toDetail(targetServings)

    private fun RecipeWithDetails.toDetail(targetServings: Int): RecipeDetail {
        val r = recipe
        val groups = orderedGroups.map { (group, items) ->
            IngredientGroupView(
                name = group.name,
                items = items.map { item ->
                    IngredientView(
                        id = item.id,
                        name = item.name,
                        amountLabel = QuantityFormatter.format(
                            amount = IngredientAmount(
                                baseAmount = item.baseAmount,
                                unitLabel = item.unitLabel,
                                gramsPerUnit = item.gramsPerUnit,
                                rawDisplay = item.rawDisplay,
                            ),
                            baseServings = r.baseServings,
                            targetServings = targetServings,
                        ),
                    )
                },
            )
        }

        val steps = orderedSteps.mapIndexed { index, step ->
            StepView(
                id = step.id,
                index = index,
                title = step.title,
                body = step.body,
                imageRef = step.imageRef,
                chipLabel = step.chipLabel,
                timerSeconds = step.timerSeconds,
                timerLabel = step.timerLabel,
                noteText = step.noteText,
                bullets = step.bulletsList(),
                materials = step.materialsList(),
            )
        }

        return RecipeDetail(
            id = r.id,
            title = r.title,
            subtitle = r.subtitle,
            coverImageRef = r.coverImageRef,
            cookTimeLabel = cookTimeLabel(r.cookTimeMinutes),
            difficulty = r.difficulty,
            servingsLabel = "${targetServings}人份",
            caloriesLabel = r.caloriesKcal?.let { "$it kcal" },
            heroBadge = r.heroBadge ?: r.heroKicker,
            heroKicker = r.heroKicker,
            equipment = r.equipment,
            isFavorite = r.isFavorite,
            baseServings = r.baseServings,
            tags = orderedTags,
            groups = groups,
            steps = steps,
            chefTipTitle = r.editorial.chefTipTitle,
            chefTipBody = r.editorial.chefTipBody,
            heatNote = r.editorial.heatNote,
            attentionNote = r.editorial.attentionNote,
        )
    }
}
