package com.ezmenu.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ezmenu.app.data.local.entity.*
import com.ezmenu.app.data.repository.RecipeRepository
import com.ezmenu.app.data.repository.SettingsRepository
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeUiRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var app: EzMenuApplication
    private lateinit var repo: RecipeRepository
    private lateinit var settings: SettingsRepository
    private var preferred = 2
    private val prefix = "回归UI" + System.nanoTime()

    @Before fun setUp(): Unit = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        withTimeout(20000) { app.services.ready.first { it } }
        repo = app.services.recipeRepository
        settings = app.services.settingsRepository
        preferred = settings.defaultServings.first()
        settings.setDefaultServings(4)
    }

    @After fun cleanUp(): Unit = runBlocking {
        (repo.observeLibrary().first() + repo.observeDrafts().first())
            .filter { it.title.startsWith(prefix) }.forEach { row ->
                val sql=app.services.database.openHelper.writableDatabase
                sql.execSQL("DELETE FROM cook_logs WHERE recipeId=?",arrayOf(row.id))
                sql.execSQL("DELETE FROM cooking_timers WHERE sessionId IN (SELECT id FROM cooking_sessions WHERE recipeId=?)",arrayOf(row.id))
                sql.execSQL("DELETE FROM cooking_sessions WHERE recipeId=?",arrayOf(row.id))
                app.services.database.journalDao().deleteRecipeDraft(row.id)
                repo.delete(row.id)
            }
        app.services.database.journalDao().draft("new")?.takeIf{it.title.startsWith(prefix)}?.let{app.services.database.journalDao().deleteDraft("new")}
        settings.setDefaultServings(preferred)
    }

    private fun awaitText(text: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun screenshot(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(app.cacheDir, "regression-$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun seed(empty: Boolean = false, origin: String = "USER"): Long = runBlocking {
        app.services.database.recipeDao().saveFullGraph(
            RecipeEntity(title = prefix + if(origin=="BUILTIN")"内置" else "", baseServings = 4, origin=origin),
            listOf(IngredientGroupEntity(recipeId = 0, name = "备料清单")),
            listOf(listOf(IngredientEntity(groupId = 0, recipeId = 0, name = "回归食材${prefix}鸡蛋", baseAmount = 600.0, unitLabel = "g"))),
            if (empty) emptyList() else listOf(StepEntity(recipeId = 0, title = "炒熟", body = "均匀翻炒")), emptyList(),
        )
    }

    @Test fun draftReopensPublishesEditsAndCompletesCookingWithCorrectNavigation() {
        try {

        compose.onNodeWithContentDescription("新建食谱").performClick()
        awaitText("新建食谱")
        compose.onNodeWithTag("editor-list").performScrollToNode(hasTestTag("recipe-title"))
        compose.onNodeWithTag("recipe-title").performTextReplacement(prefix)
        Espresso.closeSoftKeyboard()
        compose.onNodeWithText("暂存草稿").performClick()
        awaitText("草稿箱")
        awaitText(prefix)
        screenshot("drafts")
        compose.onNodeWithText(prefix).performClick()
        awaitText("新建食谱")
        awaitText(prefix)
        compose.onNodeWithTag("recipe-title").assertTextContains(prefix)
        compose.onNodeWithTag("editor-list").performScrollToNode(hasText("＋ 添加步骤"))
        compose.onNodeWithText("＋ 添加步骤").performClick()
        compose.onNodeWithTag("editor-list").performScrollToNode(hasTestTag("step-body"))
        compose.onNodeWithTag("step-body").performTextReplacement("小火翻炒至熟")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithText("收录食谱").performClick()
        awaitText("食谱详情")
        awaitText(prefix)
        screenshot("detail")
        compose.onNodeWithContentDescription("编辑").performClick()
        awaitText("修改食谱")
        compose.onNodeWithTag("editor-list").performScrollToNode(hasTestTag("recipe-title"))
        compose.onNodeWithTag("recipe-title").performTextReplacement(prefix + "已改")
        Espresso.closeSoftKeyboard()
        screenshot("editor")
        compose.onNodeWithText("保存修改").performClick()
        awaitText("食谱详情")
        awaitText(prefix + "已改")
        compose.onNodeWithText("开始烹饪").performScrollTo().performClick()
        awaitText("完成烹饪")
        compose.onNodeWithText("完成烹饪").performClick()
        awaitText("料理手记")
        runBlocking {
            val saved = repo.search(prefix).first().single()
            assertEquals(1, saved.cookCount)
            val cursor = app.services.database.openHelper.readableDatabase.query(
                "SELECT servings FROM cook_logs WHERE recipeId = ?", arrayOf(saved.id))
            cursor.use { assertTrue(it.moveToFirst()); assertEquals(4, it.getInt(0)) }
        }
        Espresso.pressBack()
        awaitText("食谱详情")
        Espresso.pressBack()
        awaitText("草稿箱")
        compose.onNodeWithText(prefix + "已改").assertDoesNotExist()
        } catch (e: Throwable) {
            File(app.cacheDir,"ui-failure.txt").writeText(e.stackTraceToString())
            runCatching { File(app.cacheDir,"ui-semantics.txt").writeText(compose.onRoot(useUnmergedTree=true).printToString());screenshot("failure") }
            throw e
        }
    }

    @Test fun searchesIngredientFromBothTabsAndRecomputesLeftoverSuggestions() {
        seed();seed(origin="BUILTIN")
        val keyword="回归食材"+prefix
        compose.onNodeWithText("搜索菜名、食材或标签").performTextInput(keyword)
        Espresso.closeSoftKeyboard();awaitText(prefix)
        compose.onNodeWithTag("nav-discover").performClick()
        compose.onNodeWithText("搜索灵感食谱").performTextInput(keyword)
        Espresso.closeSoftKeyboard();awaitText(prefix+"内置")
        compose.onNodeWithText("添加食材，如番茄、鸡蛋").performTextInput("鸡蛋")
        Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("添加食材").performClick()
        awaitText("鸡蛋");compose.onNodeWithText("鸡蛋").performClick()
        compose.waitUntil(15000){compose.onAllNodesWithText("鸡蛋").fetchSemanticsNodes().isEmpty()};screenshot("discovery")
    }

    @Test fun legacyRecipeWithoutStepsHasDisabledCookingAction() {
        seed(empty = true)
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(prefix)
        Espresso.closeSoftKeyboard()
        awaitText(prefix)
        compose.onNode(hasText(prefix) and !hasSetTextAction()).performClick()
        awaitText("食谱详情")
        compose.onNodeWithText("暂无烹饪步骤").performScrollTo().assertIsNotEnabled()
    }
}
