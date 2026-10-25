package com.ezmenu.app.domain

import kotlin.math.roundToInt

/**
 * 一份食材的用量描述。
 *
 * @param baseAmount 基准份量下的数量；`null` 表示不可换算（如「适量」「少许」）
 * @param unitLabel 单位（克/g/ml/块/枝/个/片/颗/勺）
 * @param gramsPerUnit 每单位的克重，用于生成「(约 350g)」这类换算提示；`null` 表示不显示
 * @param rawDisplay [baseAmount] 为 `null` 时逐字显示的文本
 */
data class IngredientAmount(
    val baseAmount: Double? = null,
    val unitLabel: String = "",
    val gramsPerUnit: Double? = null,
    val rawDisplay: String? = null,
)

/**
 * 按份量换算食材用量。
 *
 * 设计稿 `recipe_details/code.html` 里的参考实现是：
 * ```js
 * const calculated = (base / 2) * servings;
 * if (unit === '个' && calculated === 0.5)  el.textContent = '半个';
 * else if (unit.includes('块'))             el.textContent = `${calculated}块 (约${Math.round((350/2)*servings)}g)`;
 * else                                      el.textContent = `${calculated}${unit}`;
 * ```
 *
 * 实现时有几处**有意修正**（不是照搬）：
 *
 * 1. `(base / 2)` 里的 `/2` 是写死的基准份量。这里改成 [baseServings] 参数，
 *    否则基准份量不是 2 的菜谱会全错。
 * 2. `350/2` 是「2 块 ≈ 350g」推出来的每块克重。这里改成数据字段
 *    [IngredientAmount.gramsPerUnit]，不再靠 `unit.contains("块")` 猜。
 * 3. 原型的「半个」判断用 `unit === '个'`；这里保持一致的语义，但把「0.5 才叫半个」
 *    这条规则独立出来，避免 1.5 个被写成「1.5个」而 0.5 个被写成「半个」的割裂感 ——
 *    实际上原型就是这样，此处**保留**该行为以贴合设计稿。
 */
object QuantityFormatter {

    /**
     * @param baseServings 菜谱原始份量（`data-base` 对应的份量基准）
     * @param targetServings 用户当前选择的份量
     */
    fun format(
        amount: IngredientAmount,
        baseServings: Int,
        targetServings: Int,
    ): String {
        // 不可换算的项直接显示原文（「适量」），绝不返回 null/NaN
        val base = amount.baseAmount
            ?: return amount.rawDisplay ?: amount.unitLabel

        if (baseServings <= 0) return amount.rawDisplay ?: amount.unitLabel

        val scaled = base / baseServings * targetServings

        val number = formatNumber(scaled, amount.unitLabel)
        val grams = amount.gramsPerUnit?.let { perUnit ->
            val total = (scaled * perUnit).roundToInt()
            " (约${total}g)"
        } ?: ""

        return "$number${amount.unitLabel}$grams"
    }

    /**
     * 可用「半」的量词单位。
     *
     * 设计稿只对 `个` 做了 0.5 → 「半个」的特判。这里推广到全部量词单位，
     * 但**排除度量单位**：中文说「半个」「半块」「半勺」，却不说「半 g」「半 ml」，
     * 后者必须显示成 `0.5g`、`7.5ml`。
     */
    private val CLASSIFIER_UNITS = setOf(
        "个", "块", "片", "枝", "颗", "勺", "条", "只", "张", "根", "瓣", "盒", "瓶", "把",
    )

    /** 数字渲染：量词的 0.5 写成「半」，整数不带小数点，其它去掉尾随零。 */
    private fun formatNumber(value: Double, unit: String): String = when {
        value == 0.5 && unit in CLASSIFIER_UNITS -> "半"
        value % 1.0 == 0.0 -> value.toInt().toString()
        else -> {
            // 避免 7.500000001 这类浮点噪声：保留两位小数再去掉尾随零
            val rounded = (value * 100).roundToInt() / 100.0
            rounded.toString().trimEnd('0').trimEnd('.')
        }
    }
}

/**
 * 份量夹紧范围。
 *
 * 设计稿里两处的上限不同，这里如实照搬：
 *   - 详情页：`portions > 1` 才能减、`< 10` 才能加 → 1..10
 *   - 编辑页：`portions > 1` 才能减、`< 20` 才能加 → 1..20
 */
object ServingsRules {
    val DETAIL_RANGE = 1..10
    val EDITOR_RANGE = 1..20

    fun clampDetail(value: Int): Int = value.coerceIn(DETAIL_RANGE)
    fun clampEditor(value: Int): Int = value.coerceIn(EDITOR_RANGE)

    fun canDecrement(value: Int, range: IntRange): Boolean = value > range.first
    fun canIncrement(value: Int, range: IntRange): Boolean = value < range.last
}
