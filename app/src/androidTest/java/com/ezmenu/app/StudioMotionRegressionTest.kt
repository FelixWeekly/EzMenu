package com.ezmenu.app

import android.os.ParcelFileDescriptor
import android.provider.Settings
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import com.ezmenu.app.data.local.entity.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.junit.*

class StudioMotionRegressionTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var app: EzMenuApplication
    private var originalScale: String?=null
    private var id=0L
    private val title="动效回归"+System.nanoTime()

    @Before fun setup(): Unit=runBlocking {
        app=ApplicationProvider.getApplicationContext()
        withTimeout(20000){app.services.ready.first{it}}
        originalScale=Settings.Global.getString(app.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE)
        id=app.services.database.recipeDao().saveFullGraph(RecipeEntity(title=title,coverImageRef="drawable:recipe_beef_stew"),
            listOf(IngredientGroupEntity(recipeId=0,name="主料")),listOf(listOf(IngredientEntity(recipeId=0,groupId=0,name="牛腩",baseAmount=300.0,unitLabel="g"))),
            listOf(StepEntity(recipeId=0,title="备料",body="食材洗净切块。"),StepEntity(recipeId=0,title="慢炖",body="炖至牛腩软烂。")),emptyList())
    }
    private fun shell(command: String){
        ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)).use{it.readBytes()}
    }
    @After fun cleanup(): Unit=runBlocking {
        originalScale?.toFloatOrNull()?.let{shell("settings put global animator_duration_scale "+it)}?:shell("settings delete global animator_duration_scale")
        app.services.cookingSessions.recoverCurrent()?.takeIf{it.recipeId==id}?.let{app.services.cookingSessions.abandon(it.id)}
        app.services.database.openHelper.writableDatabase.execSQL("DELETE FROM cook_logs WHERE recipeId=?",arrayOf(id))
        app.services.database.openHelper.writableDatabase.execSQL("DELETE FROM cooking_timers WHERE sessionId IN (SELECT id FROM cooking_sessions WHERE recipeId=?)",arrayOf(id))
        app.services.database.openHelper.writableDatabase.execSQL("DELETE FROM cooking_sessions WHERE recipeId=?",arrayOf(id))
        app.services.database.journalDao().deleteRecipeDraft(id)
        app.services.database.recipeDao().deleteRecipe(id)
    }
    private fun waitText(value: String){compose.waitUntil(20000){compose.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty()}}
    private fun exercise(scale: Float){
        shell("settings put global animator_duration_scale "+scale)
        compose.activityRule.scenario.recreate()
        compose.waitUntil(20000){compose.onAllNodesWithTag("brand-name").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("brand-name").assertTextEquals("旬食")
        compose.onNodeWithTag("library-grid").performScrollToNode(hasText(title))
        compose.onNodeWithText(title).performClick();waitText("食谱详情")
        compose.onNodeWithContentDescription("收藏").performClick()
        compose.waitUntil(10000){runBlocking{app.services.database.recipeDao().getById(id)!!.recipe.isFavorite}}
        compose.onNodeWithContentDescription("取消收藏").performClick()
        compose.waitUntil(10000){runBlocking{!app.services.database.recipeDao().getById(id)!!.recipe.isFavorite}}
        compose.onNodeWithContentDescription("编辑").performClick();waitText("修改食谱")
        compose.onNodeWithTag("editor-list").performScrollToNode(hasTestTag("recipe-title"))
        compose.onNodeWithTag("recipe-title").performTextReplacement(title+"已改")
        Espresso.closeSoftKeyboard();compose.onNodeWithText("保存修改").performClick();waitText("食谱详情");waitText(title+"已改")
        compose.onNodeWithTag("recipe-content").performScrollToNode(hasText("开始烹饪"))
        compose.onNodeWithText("开始烹饪").performClick();waitText("第 1 / 2 步")
        compose.onNodeWithText("下一步").performClick();waitText("第 2 / 2 步")
        compose.onNodeWithText("上一步").performClick();waitText("第 1 / 2 步")
        compose.onNodeWithText("下一步").performClick();waitText("第 2 / 2 步")
        compose.onNodeWithText("完成烹饪").performClick();waitText("料理手记")
        runBlocking{Assert.assertEquals(1,app.services.database.recipeDao().getById(id)!!.recipe.cookCount)}
    }
    @Test fun animatedNavigationAndFeedbackPreserveChanges(){exercise(1f)}
    @Test fun disabledSystemAnimationsKeepEveryActionUsable(){exercise(0f)}
}
