package com.ezmenu.app.data.local.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.ezmenu.app.data.local.entity.IngredientEntity
import com.ezmenu.app.data.local.entity.IngredientGroupEntity
import com.ezmenu.app.data.local.entity.RecipeEntity
import com.ezmenu.app.data.local.entity.RecipeTagCrossRef
import com.ezmenu.app.data.local.entity.StepEntity
import com.ezmenu.app.data.local.entity.TagEntity

/**
 * 一个菜谱的完整图：主表 + 分组的食材 + 步骤 + 标签。
 *
 * @Relation 的查询结果不保证顺序，公开给上层的访问器统一做了排序，
 * 免得每个界面各自再排一遍。
 */
data class RecipeWithDetails(
    @Embedded val recipe: RecipeEntity,

    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val groups: List<IngredientGroupEntity> = emptyList(),

    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val ingredients: List<IngredientEntity> = emptyList(),

    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val steps: List<StepEntity> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = RecipeTagCrossRef::class,
            parentColumn = "recipeId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<TagEntity> = emptyList(),

    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val tagRefs: List<RecipeTagCrossRef> = emptyList(),
) {
    /** 分组按 [IngredientGroupEntity.sortOrder] 排好，每组带好自己的食材。 */
    val orderedGroups: List<Pair<IngredientGroupEntity, List<IngredientEntity>>>
        get() = groups
            .sortedBy { it.sortOrder }
            .map { group ->
                group to ingredients
                    .filter { it.groupId == group.id }
                    .sortedBy { it.sortOrder }
            }

    val orderedSteps: List<StepEntity>
        get() = steps.sortedBy { it.sortOrder }

    val orderedTags: List<TagEntity>
        get() {
            val order = tagRefs.associate { it.tagId to it.sortOrder }
            return tags.sortedBy { order[it.id] ?: it.sortOrder }
        }
}
