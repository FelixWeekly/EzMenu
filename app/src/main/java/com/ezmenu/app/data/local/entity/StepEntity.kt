package com.ezmenu.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 烹饪步骤。
 *
 * 同一个步骤实体要同时服务三个界面，所以字段比详情页实际用到的多：
 *   - 详情页：序号 + 标题 + 正文 + 时长角标 + 配图 + 内嵌计时器
 *   - 编辑页：标题 + 正文 + 计时 pill + 提示条
 *   - 烹饪模式：编号说明列表 + 火候 + 物料 chips
 */
@Entity(
    tableName = "steps",
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
data class StepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    /** 排序位次，决定 Step 01 / Step 02 的编号 */
    val sortOrder: Int = 0,

    /** 步骤小标题，如「腌制入味」「控温慢煎」 */
    val title: String = "",
    /** 正文描述 */
    val body: String = "",

    val imageRef: String? = null,

    /** 时长角标文案，如「约 10 分钟」「火候精准」「封边出锅」 */
    val chipLabel: String? = null,
    /** 内嵌计时器秒数；`null` 表示该步骤没有计时 */
    val timerSeconds: Int? = null,
    /** 计时器行的说明，如「单面慢煎推荐时延」 */
    val timerLabel: String? = null,
    /** 编辑页步骤卡右下角的提示，如「建议吸透面包芯」 */
    val noteText: String? = null,

    /**
     * 烹饪模式里的编号说明条目，JSON 字符串数组。
     * 正文里的 `**雷点**` 标记会在渲染时转成主色强调。
     */
    val bulletsJson: String? = null,

    /** 烹饪模式底部的物料 chips，JSON 字符串数组，如 `["橄榄油 15ml","迷迭香 2枝"]` */
    val materialChipsJson: String? = null,
)
