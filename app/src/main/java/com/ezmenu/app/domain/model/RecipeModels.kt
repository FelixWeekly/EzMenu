package com.ezmenu.app.domain.model

import com.ezmenu.app.data.local.entity.RecipeEntity
import com.ezmenu.app.data.local.entity.StepEntity
import com.ezmenu.app.data.local.entity.TagEntity
import com.ezmenu.app.domain.IngredientAmount

/**
 * 列表/网格里的一张菜谱卡所需的全部信息。
 *
 * 与 [RecipeEntity] 的区别：这里只保留渲染卡片要用的字段，并补上界面需要的
 * 派生文案（如「20 分钟」），避免每个列表项各自拼字符串。
 */
data class RecipeCard(
    val id: Long,
    val title: String,
    val subtitle: String,
    val coverImageRef: String?,
    val cookTimeLabel: String,
    val footerNote: String?,
    val heroBadge: String?,
    val heroKicker: String?,
    val heroNote: String?,
    val heroCta: String?,
    val difficulty: String?,
    val author: String?,
    val isFavorite: Boolean,
    val cookCount: Int,
    /** 主分类标签（用于珍藏页的筛选 pill 与菜谱库的分类 chip） */
    val category: String,
    /** 卡片上显示的标签 chips，按 [TagEntity.sortOrder] 排序 */
    val tags: List<TagEntity>,
)

/**
 * 详情页的完整视图模型。
 *
 * 食材已经按当前份量算好显示文案 —— 换算逻辑只在这里跑一次，
 * 界面层不再碰 [IngredientAmount]。
 */
data class RecipeDetail(
    val id: Long,
    val title: String,
    val subtitle: String,
    val coverImageRef: String?,
    val cookTimeLabel: String,
    val difficulty: String,
    val servingsLabel: String,
    val caloriesLabel: String?,
    val heroBadge: String?,
    val heroKicker: String?,
    val equipment: String?,
    val isFavorite: Boolean,
    val baseServings: Int,
    val tags: List<TagEntity>,
    val groups: List<IngredientGroupView>,
    val steps: List<StepView>,
    val chefTipTitle: String?,
    val chefTipBody: String?,
    val heatNote: String?,
    val attentionNote: String?,
)

data class IngredientGroupView(
    val name: String,
    val items: List<IngredientView>,
)

data class IngredientView(
    val id: Long,
    val name: String,
    /** 已按目标份量换算好的用量文案 */
    val amountLabel: String,
)

data class StepView(
    val id: Long,
    val index: Int,
    val title: String,
    val body: String,
    val imageRef: String?,
    val chipLabel: String?,
    val timerSeconds: Int?,
    val timerLabel: String?,
    val noteText: String?,
    val bullets: List<String>,
    val materials: List<String>,
)

/** 把一个菜谱实体摊平成卡片模型。 */
fun RecipeEntity.toCard(tags: List<TagEntity>): RecipeCard = RecipeCard(
    id = id,
    title = title,
    subtitle = subtitle,
    coverImageRef = coverImageRef,
    cookTimeLabel = cookTimeLabel(cookTimeMinutes),
    footerNote = footerNote,
    heroBadge = heroBadge,
    heroKicker = heroKicker,
    heroNote = heroNote,
    heroCta = heroCta,
    difficulty = difficulty.takeIf { it.isNotEmpty() },
    author = author,
    isFavorite = isFavorite,
    cookCount = cookCount,
    category = category,
    tags = tags,
)

/**
 * 时长文案。
 *
 * 设计稿里时长有几种写法：「20分钟」（详情页元信息）、「20 min」（珍藏页 hero 角标）、
 * 「15 分钟」（菜谱库网格卡）。这里统一成 [format] 的一种，需要别的写法时再传参，
 * 避免同一份数据在不同界面显示成三种样子。
 */
fun cookTimeLabel(minutes: Int, style: CookTimeStyle = CookTimeStyle.CHINESE_COMPACT): String =
    when {
        minutes <= 0 -> ""
        style == CookTimeStyle.LATIN_MIN -> "$minutes min"
        style == CookTimeStyle.CHINESE_SPACED -> "$minutes 分钟"
        else -> "${minutes}分钟"
    }

enum class CookTimeStyle { CHINESE_COMPACT, CHINESE_SPACED, LATIN_MIN }

/** 把步骤实体的 JSON 数组字段拆出来，解析失败时返回空列表而不是抛异常。 */
fun StepEntity.bulletsList(): List<String> = decodeJsonArray(bulletsJson)

fun StepEntity.materialsList(): List<String> = decodeJsonArray(materialChipsJson)

private fun decodeJsonArray(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = org.json.JSONArray(raw)
        (0 until array.length()).map { array.getString(it) }
    }.getOrDefault(emptyList())
}
