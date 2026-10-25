package com.ezmenu.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 份量换算的单测。
 *
 * 这是全应用唯一有真正算法逻辑的地方，也是设计稿里**唯一有 bug** 的地方
 * （原型的 `/2` 把基准份量写死了，且「优质柠檬」的初始渲染与计算值自相矛盾），
 * 所以覆盖要密。
 *
 * 对照表直接来自 `reference/recipe_details/screen.png` 与 `code.html` 里的
 * `data-base` / `data-unit` 属性。
 */
class QuantityFormatterTest {

    // ---- 详情页「香煎迷迭香脆皮三文鱼排」的 6 项食材，基准 2 人份 ----

    private val salmon = IngredientAmount(baseAmount = 2.0, unitLabel = "块", gramsPerUnit = 175.0)
    private val asparagus = IngredientAmount(baseAmount = 120.0, unitLabel = "g")
    private val rosemary = IngredientAmount(baseAmount = 2.0, unitLabel = "枝")
    private val lemon = IngredientAmount(baseAmount = 1.0, unitLabel = "个")
    private val oliveOil = IngredientAmount(baseAmount = 15.0, unitLabel = "ml")
    private val salt = IngredientAmount(rawDisplay = "适量")

    @Test
    fun `三文鱼排 - 各份量下的块数与克重`() {
        // 这一列是设计稿里明确写着的，必须逐格吻合
        assertEquals("1块 (约175g)", QuantityFormatter.format(salmon, 2, 1))
        assertEquals("2块 (约350g)", QuantityFormatter.format(salmon, 2, 2))
        assertEquals("3块 (约525g)", QuantityFormatter.format(salmon, 2, 3))
        assertEquals("4块 (约700g)", QuantityFormatter.format(salmon, 2, 4))
    }

    @Test
    fun `按克计的食材 - 整数克不带小数点`() {
        assertEquals("60g", QuantityFormatter.format(asparagus, 2, 1))
        assertEquals("120g", QuantityFormatter.format(asparagus, 2, 2))
        assertEquals("180g", QuantityFormatter.format(asparagus, 2, 3))
        assertEquals("240g", QuantityFormatter.format(asparagus, 2, 4))
    }

    @Test
    fun `量词单位 - 枝`() {
        assertEquals("1枝", QuantityFormatter.format(rosemary, 2, 1))
        assertEquals("2枝", QuantityFormatter.format(rosemary, 2, 2))
    }

    @Test
    fun `半个规则 - 量词单位的 0-5 显示为半`() {
        // 设计稿的 core case
        assertEquals("半个", QuantityFormatter.format(lemon, 2, 1))
        // 注意：原型里初始渲染硬编码「半个」但 data-base=1 在 2 人份时会算成「1个」，
        // 初始值与计算值互相矛盾。这里**以计算值为准**，这是修正而非还原。
        assertEquals("1个", QuantityFormatter.format(lemon, 2, 2))
        assertEquals("1.5个", QuantityFormatter.format(lemon, 2, 3))
        assertEquals("2个", QuantityFormatter.format(lemon, 2, 4))
    }

    @Test
    fun `度量单位的小数 - 不得把 7-5 ml 写成半 ml`() {
        // 中文说「半个」，但不说「半 ml」。度量单位必须保持数字。
        assertEquals("7.5ml", QuantityFormatter.format(oliveOil, 2, 1))
        assertEquals("15ml", QuantityFormatter.format(oliveOil, 2, 2))
        assertEquals("22.5ml", QuantityFormatter.format(oliveOil, 2, 3))
        assertEquals("30ml", QuantityFormatter.format(oliveOil, 2, 4))
    }

    @Test
    fun `不可换算项 - 原样返回 rawDisplay 且不出现 null 或 NaN`() {
        for (servings in 1..10) {
            val result = QuantityFormatter.format(salt, 2, servings)
            assertEquals("适量", result)
            assertFalse("不应出现 null: $result", result.contains("null"))
            assertFalse("不应出现 NaN: $result", result.contains("NaN"))
        }
    }

    @Test
    fun `基准份量不是 2 时也正确 - 原型把除以 2 写死了`() {
        // 原型是 (base / 2) * servings，基准份量一旦不是 2 就全错。
        // 4 人份的菜谱算 2 人份，应该是原量的一半。
        val fourPersonDish = IngredientAmount(baseAmount = 400.0, unitLabel = "g")
        assertEquals("200g", QuantityFormatter.format(fourPersonDish, 4, 2))
        assertEquals("400g", QuantityFormatter.format(fourPersonDish, 4, 4))
        assertEquals("600g", QuantityFormatter.format(fourPersonDish, 4, 6))
    }

    @Test
    fun `浮点噪声 - 不产生 7-500000001 这类尾巴`() {
        val oil = IngredientAmount(baseAmount = 5.0, unitLabel = "ml")
        assertEquals("2.5ml", QuantityFormatter.format(oil, 2, 1))
        assertEquals("7.5ml", QuantityFormatter.format(oil, 2, 3))
        assertEquals("12.5ml", QuantityFormatter.format(oil, 2, 5))

        val third = IngredientAmount(baseAmount = 1.0, unitLabel = "勺")
        // 1/3 之类除不尽的情况，保留两位小数即可，不能出现长尾
        val result = QuantityFormatter.format(third, 3, 1)
        assertTrue("结果过长: $result", result.length <= 8)
    }

    @Test
    fun `无克重信息时不追加括号提示`() {
        assertFalse(QuantityFormatter.format(rosemary, 2, 2).contains("约"))
        assertTrue(QuantityFormatter.format(salmon, 2, 2).contains("约350g"))
    }

    @Test
    fun `基准份量为 0 或负数时优雅降级`() {
        // 数据异常时不应崩溃，也不应产生 Infinity / NaN
        val result = QuantityFormatter.format(oliveOil, 0, 2)
        assertFalse(result.contains("Infinity"))
        assertFalse(result.contains("NaN"))
    }

    // ---------------------------- 份量范围夹紧 ----------------------------

    @Test
    fun `详情页份量夹紧到 1 到 10`() {
        assertEquals(1, ServingsRules.clampDetail(0))
        assertEquals(1, ServingsRules.clampDetail(-5))
        assertEquals(10, ServingsRules.clampDetail(11))
        assertEquals(7, ServingsRules.clampDetail(7))
    }

    @Test
    fun `编辑页份量夹紧到 1 到 20`() {
        assertEquals(1, ServingsRules.clampEditor(0))
        assertEquals(20, ServingsRules.clampEditor(99))
        assertEquals(20, ServingsRules.clampEditor(20))
    }

    @Test
    fun `加减按钮的可用性边界与设计稿一致`() {
        assertFalse(ServingsRules.canDecrement(1, ServingsRules.DETAIL_RANGE))
        assertTrue(ServingsRules.canDecrement(2, ServingsRules.DETAIL_RANGE))
        assertFalse(ServingsRules.canIncrement(10, ServingsRules.DETAIL_RANGE))
        assertTrue(ServingsRules.canIncrement(9, ServingsRules.DETAIL_RANGE))

        assertFalse(ServingsRules.canIncrement(20, ServingsRules.EDITOR_RANGE))
        assertTrue(ServingsRules.canIncrement(19, ServingsRules.EDITOR_RANGE))
    }
}
