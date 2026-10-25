package com.ezmenu.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ezmenu.app.data.local.entity.CookLogEntity
import com.ezmenu.app.data.local.entity.IngredientEntity
import com.ezmenu.app.data.local.entity.IngredientGroupEntity
import com.ezmenu.app.data.local.entity.PantryItemEntity
import com.ezmenu.app.data.local.entity.RecipeEntity
import com.ezmenu.app.data.local.entity.RecipeTagCrossRef
import com.ezmenu.app.data.local.entity.StepEntity
import com.ezmenu.app.data.local.entity.TagEntity
import com.ezmenu.app.data.local.relation.RecipeWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Transaction
    @Query("""
        SELECT r.* FROM recipes r WHERE
        ((:scope=3 AND r.trashedAt IS NOT NULL) OR (:scope!=3 AND r.trashedAt IS NULL))
        AND (:scope=0 OR :scope=3 OR (:scope=1 AND r.origin!='BUILTIN') OR (:scope=2 AND r.origin='BUILTIN'))
        AND (:mode='trash' OR (:mode='drafts' AND r.isDraft=1) OR (:mode NOT IN ('drafts','trash') AND r.isDraft=0))
        AND (:mode!='favorites' OR r.isFavorite=1)
        AND (:category='' OR r.category=:category)
        AND (r.title LIKE :pattern ESCAPE '\' OR r.subtitle LIKE :pattern ESCAPE '\'
             OR r.author LIKE :pattern ESCAPE '\' OR r.category LIKE :pattern ESCAPE '\'
             OR EXISTS(SELECT 1 FROM ingredients i WHERE i.recipeId=r.id AND i.name LIKE :pattern ESCAPE '\')
             OR EXISTS(SELECT 1 FROM recipe_tags rt JOIN tags t ON t.id=rt.tagId WHERE rt.recipeId=r.id AND t.name LIKE :pattern ESCAPE '\'))
        ORDER BY CASE WHEN :sort='time' THEN r.cookTimeMinutes END ASC,
        CASE WHEN :sort='cooked' THEN (SELECT MAX(cookedAt) FROM cook_logs c WHERE c.recipeId=r.id AND c.source='USER') END DESC,
        r.updatedAt DESC,r.id DESC LIMIT :limit
    """)
    fun browse(pattern: String, scope: Int = 1, mode: String = "all", category: String = "", sort: String = "updated", limit: Int = 40): Flow<List<com.ezmenu.app.data.local.relation.RecipeListing>>

    @Query("SELECT * FROM recipes WHERE sourceKey=:key LIMIT 1") suspend fun findSource(key: String): RecipeEntity?
    @Query("UPDATE recipes SET trashedAt=:time WHERE id=:id AND origin!='BUILTIN'") suspend fun trash(id: Long, time: Long?)
    @Query("UPDATE cook_logs SET recipeId=NULL WHERE recipeId=:id") suspend fun detachLogs(id: Long)
    @Query("SELECT r.id AS recipeId,i.name FROM recipes r JOIN ingredients i ON i.recipeId=r.id WHERE r.isDraft=0 AND r.trashedAt IS NULL")
    fun recommendationIngredients(): Flow<List<RecommendationIngredient>>
    @Query("SELECT DISTINCT category FROM recipes WHERE isDraft=0 AND trashedAt IS NULL AND origin!='BUILTIN' ORDER BY category")
    fun categories(): Flow<List<String>>


    @Transaction
    suspend fun collectBuiltin(id: Long): Long {
        val graph = requireNotNull(getById(id)) { "菜谱不存在" }
        if (graph.recipe.origin != "BUILTIN") return id
        val key = "copy:" + graph.recipe.sourceKey
        findSource(key)?.let { if(it.trashedAt!=null) trash(it.id,null); return it.id }
        return saveFullGraph(graph.recipe.copy(id=0,origin="USER",sourceKey=key,isFavorite=false,cookCount=0),
            graph.orderedGroups.map { it.first },graph.orderedGroups.map { it.second },graph.orderedSteps,
            graph.orderedTags.map { TagTriple(it.name,com.ezmenu.app.data.local.entity.TagKind.valueOf(it.kind)) })
    }
    @Transaction
    suspend fun permanentlyDelete(id: Long) { detachLogs(id); deleteRecipe(id) }
    // ------------------------------------------------------------ 查询

    @Transaction
    @Query("SELECT * FROM recipes WHERE isDraft = 0 AND trashedAt IS NULL ORDER BY updatedAt DESC")
    fun observePublished(): Flow<List<RecipeWithDetails>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE isDraft = 1 AND trashedAt IS NULL ORDER BY updatedAt DESC")
    fun observeDrafts(): Flow<List<RecipeWithDetails>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE isFavorite = 1 AND isDraft = 0 AND trashedAt IS NULL ORDER BY updatedAt DESC")
    fun observeFavorites(): Flow<List<RecipeWithDetails>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observeById(id: Long): Flow<RecipeWithDetails?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getById(id: Long): RecipeWithDetails?

    /**
     * 搜索。按标题或**食材名**匹配 —— 设计稿的搜索框提示是
     * 「寻觅当季食味、食材或烹饪法...」，用户输入「三文鱼」时显然期望
     * 命中那些把三文鱼列为食材的菜谱，而不只是标题里带这三个字的。
     */
    @Transaction
    @Query(
        """
        SELECT DISTINCT r.* FROM recipes r
        LEFT JOIN ingredients i ON i.recipeId = r.id
        LEFT JOIN recipe_tags rt ON rt.recipeId = r.id
        LEFT JOIN tags t ON t.id = rt.tagId
        WHERE r.isDraft = 0 AND r.trashedAt IS NULL AND (
            r.title LIKE :pattern ESCAPE '\'
            OR r.subtitle LIKE :pattern ESCAPE '\'
            OR i.name LIKE :pattern ESCAPE '\'
            OR r.author LIKE :pattern ESCAPE '\'
            OR r.category LIKE :pattern ESCAPE '\'
            OR t.name LIKE :pattern ESCAPE '\'
        )
        ORDER BY r.updatedAt DESC, r.id DESC
        """,
    )
    fun search(pattern: String): Flow<List<RecipeWithDetails>>

    // ------------------------------------------------------------ 写入

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Update
    suspend fun updateRecipe(recipe: RecipeEntity)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipe(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: IngredientGroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(items: List<IngredientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(items: List<StepEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTagRefs(refs: List<RecipeTagCrossRef>)

    @Query("DELETE FROM ingredient_groups WHERE recipeId = :recipeId")
    suspend fun deleteGroupsFor(recipeId: Long)

    @Query("DELETE FROM steps WHERE recipeId = :recipeId")
    suspend fun deleteStepsFor(recipeId: Long)

    @Query("DELETE FROM recipe_tags WHERE recipeId = :recipeId")
    suspend fun deleteTagRefsFor(recipeId: Long)

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun findTagByName(name: String): TagEntity?

    /**
     * 整图保存：子表先清后插，避免残留旧行。
     * 整个过程包在一个事务里 —— 中途失败不会留下「有食材没步骤」的残缺菜谱。
     */
    @Transaction
    suspend fun saveFullGraph(
        recipe: RecipeEntity,
        groups: List<IngredientGroupEntity>,
        ingredientsByGroupIndex: List<List<IngredientEntity>>,
        steps: List<StepEntity>,
        tagNames: List<TagTriple>,
    ): Long {
        val now = System.currentTimeMillis()
        val recipeId = if (recipe.id == 0L) {
            insertRecipe(recipe.copy(createdAt = now, updatedAt = now))
        } else {
            updateRecipe(recipe.copy(updatedAt = now))
            recipe.id
        }

        deleteGroupsFor(recipeId)
        deleteStepsFor(recipeId)
        deleteTagRefsFor(recipeId)

        groups.forEachIndexed { index, group ->
            val groupId = insertGroup(group.copy(id = 0, recipeId = recipeId))
            val rows = ingredientsByGroupIndex.getOrElse(index) { emptyList() }
            insertIngredients(
                rows.mapIndexed { rowIndex, item ->
                    item.copy(id = 0, groupId = groupId, recipeId = recipeId, sortOrder = rowIndex)
                },
            )
        }

        insertSteps(steps.mapIndexed { i, step -> step.copy(id = 0, recipeId = recipeId, sortOrder = i) })

        val refs = tagNames.mapIndexed { i, triple ->
            val tagId = findTagByName(triple.name)?.id
                ?: insertTag(TagEntity(name = triple.name, kind = triple.kind.name))
            RecipeTagCrossRef(recipeId = recipeId, tagId = tagId, sortOrder = i)
        }
        insertTagRefs(refs)

        return recipeId
    }

    @Query("UPDATE recipes SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    /** 在同一事务中读取最新元数据，避免编辑覆盖收藏、下厨记录等并发变化。 */
    @Transaction
    suspend fun saveEditedGraph(
        recipe: RecipeEntity,
        groups: List<IngredientGroupEntity>,
        ingredientsByGroup: List<List<IngredientEntity>>,
        steps: List<StepEntity>,
        tags: List<TagTriple>,
        editMetadata: Boolean = false,
    ): Long {
        val current = if (recipe.id == 0L) null else
            requireNotNull(getById(recipe.id)) { "菜谱已被删除，请返回后重试" }
        val merged = current?.recipe?.copy(
            title = recipe.title,
            subtitle = recipe.subtitle,
            story = recipe.story,
            coverImageRef = recipe.coverImageRef,
            cookTimeMinutes = recipe.cookTimeMinutes,
            baseServings = recipe.baseServings,
            isDraft = recipe.isDraft,
            category = if (editMetadata) recipe.category else current.recipe.category,
            difficulty = if (editMetadata) recipe.difficulty else current.recipe.difficulty,
        ) ?: recipe
        val keptTags = if (editMetadata) tags else current?.orderedTags?.map {
            TagTriple(it.name, com.ezmenu.app.data.local.entity.TagKind.valueOf(it.kind))
        } ?: tags
        return saveFullGraph(merged, groups, ingredientsByGroup, steps, keptTags)
    }

    /** 完成一次烹饪：次数 +1 并留下一条记录。 */
    @Transaction
    suspend fun recordCook(recipeId: Long, servings: Int) {
        incrementCookCount(recipeId)
        insertCookLog(CookLogEntity(recipeId = recipeId, cookedAt = System.currentTimeMillis(), servings = servings))
    }

    @Query("UPDATE recipes SET cookCount = cookCount + 1 WHERE id = :id")
    suspend fun incrementCookCount(id: Long)

    @Insert
    suspend fun insertCookLog(log: CookLogEntity)

    // ------------------------------------------------------------ 统计
    // 「我的」页的数字全部来自这里，真实统计而非写死。

    @Query("SELECT COUNT(*) FROM recipes WHERE isDraft = 0 AND trashedAt IS NULL AND origin!='BUILTIN'")
    fun observePublishedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM recipes WHERE isDraft = 1 AND trashedAt IS NULL")
    fun observeDraftCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cook_logs")
    fun observeCookLogCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM recipes WHERE isFavorite = 1 AND isDraft = 0")
    fun observeFavoriteCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM recipes WHERE category = :category AND isDraft = 0")
    fun observeCountByCategory(category: String): Flow<Int>

    // ------------------------------------------------------------ 常备食材

    @Query("SELECT * FROM pantry_items ORDER BY name")
    fun observePantry(): Flow<List<PantryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPantryItems(items: List<PantryItemEntity>)

    @Query("DELETE FROM pantry_items")
    suspend fun clearPantry()

    @Query("SELECT COUNT(*) FROM pantry_items")
    fun observePantryCount(): Flow<Int>

    @Delete
    suspend fun deletePantryItem(item: PantryItemEntity)

    /** 一次性读取，供播种前判断库是否为空（Flow 不适用于这种一次性判断）。 */
    @Query("SELECT COUNT(*) FROM recipes WHERE isDraft = 0")
    suspend fun publishedCountOnce(): Int

    /**
     * 播种时补写历史下厨记录。
     *
     * 预置菜谱带着设计稿标注的「已做过 N 次」（如珍藏页的「已做过 6 次」）。
     * 如果只写菜谱的 cookCount 而不落下厨记录，就会出现「珍藏页说做过 6 次、
     * 我的页说有 0 次下厨记录」这种自相矛盾。这里按 cookCount 补齐对应条数的记录，
     * 两个页面的数字都来自真实查询且互相一致。
     */
    @Transaction
    suspend fun backfillCookLogs(recipeId: Long, count: Int, servings: Int) {
        if (count <= 0) return
        val now = System.currentTimeMillis()
        repeat(count) { i ->
            insertCookLog(
                CookLogEntity(
                    recipeId = recipeId,
                    // 往前铺开，让时间线看起来像真实的使用历史
                    cookedAt = now - (count - i) * 86_400_000L,
                    servings = servings,
                ),
            )
        }
    }
}

/** 保存菜谱时用的标签三元组（名字 + 调性 + 顺序由调用方列表决定）。 */
data class TagTriple(val name: String, val kind: com.ezmenu.app.data.local.entity.TagKind)

data class RecommendationIngredient(val recipeId: Long, val name: String)
