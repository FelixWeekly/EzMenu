package com.ezmenu.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 标签。
 *
 * 设计稿里的标签有三种视觉样式（见珍藏页的卡片角标），所以用 [TagKind]
 * 记录它在界面上该按哪种调性渲染，而不是在 UI 层硬编码标签名。
 */
@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: String = TagKind.DIET.name,
    val sortOrder: Int = 0,
)

/** 标签的用途分类，决定渲染样式。 */
enum class TagKind {
    /** 菜谱库的分类 chip：元气早餐 / 轻卡减脂 / 空气炸锅 / 一人食记 / 旬味暖炖 */
    CATEGORY,

    /** 膳食标签，绿色系：高蛋白 / 低卡高蛋白 / 纯素友好 */
    DIET,

    /** 场景标签，琥珀色系：空气炸锅 / 周末宴客 / 快手便当 */
    SCENE,

    /** 时令标签，主色系：早午餐 / 春笋当令 */
    SEASON,
}

/** 菜谱 ↔ 标签的多对多关联。 */
@Entity(
    tableName = "recipe_tags",
    primaryKeys = ["recipeId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipeId"), Index("tagId")],
)
data class RecipeTagCrossRef(
    val recipeId: Long,
    val tagId: Long,
    val sortOrder: Int = 0,
)

/**
 * 下厨记录，每次完成一次烹饪模式写一行。
 *
 * 「我的」页面的「次下厨记录」是这张表的 COUNT —— 用户真的做一次菜，
 * 数字就真的加一，不是写死的。
 */
@Entity(tableName = "cook_logs", indices = [Index("recipeId"), Index(value = ["sessionId"], unique = true)])
data class CookLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long?,
    val cookedAt: Long,
    val servings: Int = 2,
    val sessionId: String? = null,
    val snapshot: String? = null,
    val recipeTitle: String = "",
    val coverRef: String? = null,
    val note: String = "",
    val nextTime: String = "",
    val photosJson: String = "[]",
    val source: String = "USER",
)

/**
 * 常备食材。
 *
 * 服务两个界面：「发现」页的「余料寻味」（冰箱里有什么就推荐什么）
 * 与「我的」页的「常备香料与生鲜」统计。
 */
@Entity(tableName = "pantry_items")
data class PantryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** 补充说明，如「12种调味 · 5款冷冻」里的分类 */
    val note: String? = null,
    val addedAt: Long = 0,
)
