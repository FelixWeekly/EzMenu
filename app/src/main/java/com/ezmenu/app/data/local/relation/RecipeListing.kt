package com.ezmenu.app.data.local.relation

import androidx.room.*
import com.ezmenu.app.data.local.entity.*
import com.ezmenu.app.domain.model.toCard

/** Card projection: never loads recipe steps or ingredient graphs. */
data class RecipeListing(
    @Embedded val recipe: RecipeEntity,
    @Relation(parentColumn="id", entityColumn="id", associateBy=Junction(RecipeTagCrossRef::class, parentColumn="recipeId", entityColumn="tagId"))
    val tags: List<TagEntity> = emptyList(),
) { fun toCard() = recipe.toCard(tags) }
