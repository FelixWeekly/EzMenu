package com.ezmenu.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 菜谱主表。
 *
 * 编辑式内容（hero 角标、卷尾语、主厨贴士等）打包在 [EditorialMeta] 里 ——
 * 它们只在设计稿的少数几个界面出现，拆成独立的表会让查询变得很啰嗦。
 */
@Entity(tableName = "recipes", indices = [Index(value = ["sourceKey"], unique = true)])
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** 菜谱标题，如「香煎迷迭香脆皮三文鱼排」 */
    val title: String,
    /** 副标题／一句话风味故事，如「外脆里嫩 · 高蛋白低碳水轻食经典」 */
    val subtitle: String = "",
    /** 编辑页的「一句话风味独白」，比 subtitle 更长 */
    val story: String = "",

    /** 封面图引用，形如 `drawable:recipe_salmon_hero` 或用户选图后的 `content://…` */
    val coverImageRef: String? = null,

    val cookTimeMinutes: Int = 0,
    /** 份量换算的基准份量（对应设计稿的 `data-base` 所依赖的份量） */
    val baseServings: Int = 2,
    /** 难度文案，如「新手友好」「适中」 */
    val difficulty: String = "",
    val caloriesKcal: Int? = null,
    /** 作者昵称，如「林间小肆」 */
    val author: String? = null,

    /** 所属分类，对应菜谱库的分类 chip 与珍藏页的筛选 pill */
    val category: String = "全部",

    /** hero 卡左上角的徽标，如「主厨甄选 · 20分钟」 */
    val heroBadge: String? = null,
    /** hero 卡上的分类标签，如「慢炖经典」 */
    val heroKicker: String? = null,
    /** hero 卡右上角的辅助文案，如「慢火熟成 20m」 */
    val heroNote: String? = null,
    /** hero 卡按钮文案，如「开煮」 */
    val heroCta: String? = null,
    /** 卡片底部的一句话点评，如「轻松快手」「纯素友好」 */
    val footerNote: String? = null,
    /** 需要的厨具，如「平底铸铁锅」 */
    val equipment: String? = null,

    val isFavorite: Boolean = false,
    val isDraft: Boolean = false,
    /** 累计下厨次数，驱动珍藏页的「已做过 N 次」 */
    val cookCount: Int = 0,

    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    @ColumnInfo(defaultValue = "'USER'") val origin: String = "USER",
    val sourceKey: String? = null,
    val trashedAt: Long? = null,

    @Embedded val editorial: EditorialMeta = EditorialMeta(),
)

/**
 * 编辑式文案集合。
 *
 * 这些内容在设计稿里分布在不同界面：主厨贴士在详情页，火候提示在烹饪模式，
 * 时令见解在菜谱库底部。合并在一起避免为几个字符串再开三张表。
 */
data class EditorialMeta(
    /** 主厨贴士标题，如「主厨关键贴士 (Chef's Tips)」 */
    val chefTipTitle: String? = null,
    val chefTipBody: String? = null,
    /** 烹饪模式里的火候摘要，如「中大火烧热，转文火慢煎」 */
    val heatNote: String? = null,
    /** 烹饪模式里的警示条，如「鱼皮朝下贴锅底，3分钟内请勿翻动」 */
    val attentionNote: String? = null,
    /** 菜谱库底部的时令见解 */
    val seasonalNote: String? = null,
)

/**
 * 食材分组（主料生鲜 / 配料调味）。
 *
 * 设计稿的详情页把食材分成两组并各自带小标题，所以分组必须是独立实体，
 * 不能只是一个字段。
 */
@Entity(
    tableName = "ingredient_groups",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipeId")],
)
data class IngredientGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    /** 「主料生鲜」或「配料调味」 */
    val name: String,
    val sortOrder: Int = 0,
)

/**
 * 单个食材。
 *
 * [baseAmount] 与 [rawDisplay] 是互斥的两种表达：
 *   - 可换算的（「120g」「2块」）填 [baseAmount]
 *   - 不可换算的（「适量」「少许」）只填 [rawDisplay]
 *
 * [gramsPerUnit] 支撑设计稿的「2块 (约350g)」写法：350g / 2块 = 175 g/块。
 */
@Entity(
    tableName = "ingredients",
    foreignKeys = [
        ForeignKey(
            entity = IngredientGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("groupId"), Index("recipeId")],
)
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    /** 冗余一份 recipeId，便于只按菜谱查询时不必先查分组 */
    val recipeId: Long,
    val name: String,
    val baseAmount: Double? = null,
    val unitLabel: String = "",
    val gramsPerUnit: Double? = null,
    val rawDisplay: String? = null,
    val sortOrder: Int = 0,
)
