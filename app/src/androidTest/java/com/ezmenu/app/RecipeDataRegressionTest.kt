package com.ezmenu.app

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ezmenu.app.data.local.EzMenuDatabase
import com.ezmenu.app.data.local.dao.TagTriple
import com.ezmenu.app.data.local.entity.*
import com.ezmenu.app.data.repository.RecipeRepository
import com.ezmenu.app.data.repository.SettingsRepository
import com.ezmenu.app.domain.searchPattern
import com.ezmenu.app.navigation.Routes
import com.ezmenu.app.ui.screens.cooking.CookingViewModel
import com.ezmenu.app.ui.screens.detail.RecipeDetailViewModel
import com.ezmenu.app.ui.screens.editor.RecipeEditorViewModel
import com.ezmenu.app.util.CoverReferences
import com.ezmenu.app.util.ImageStorage
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeDataRegressionTest {
    private lateinit var context: Context
    private lateinit var db: EzMenuDatabase
    private lateinit var repo: RecipeRepository
    private lateinit var settings: SettingsRepository
    private var oldServings = 2
    private val models = mutableListOf<ViewModel>()

    @Before fun setUp(): Unit = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, EzMenuDatabase::class.java).build()
        repo = RecipeRepository(db.recipeDao())
        settings = (context as EzMenuApplication).services.settingsRepository
        oldServings = settings.defaultServings.first()
        settings.setDefaultServings(4)
    }

    @After fun tearDown() = runBlocking {
        models.forEach { it.viewModelScope.cancel() }
        settings.setDefaultServings(oldServings)
        db.close()
    }

    private suspend fun createRecipe(draft: Boolean = false, emptySteps: Boolean = false): Long =
        db.recipeDao().saveFullGraph(
            RecipeEntity(title = "回归炖菜", subtitle = "原副标题", story = "原故事", category = "旬味暖炖",
                baseServings = 4, cookTimeMinutes = 90, isDraft = draft, isFavorite = true,
                cookCount = 6, author = "测试厨师", editorial = EditorialMeta(chefTipBody = "保留贴士")),
            listOf(IngredientGroupEntity(recipeId = 0, name = "主料"), IngredientGroupEntity(recipeId = 0, name = "调味")),
            listOf(listOf(IngredientEntity(groupId = 0, recipeId = 0, name = "牛肉", baseAmount = 600.0, unitLabel = "g", gramsPerUnit = 1.0)),
                listOf(IngredientEntity(groupId = 0, recipeId = 0, name = "盐", rawDisplay = "少许"))),
            if (emptySteps) emptyList() else listOf(StepEntity(recipeId = 0, title = "炖煮", body = "小火慢炖",
                timerSeconds = 180, timerLabel = "炖煮计时", chipLabel = "小火",
                bulletsJson = "[\"保留要点\"]", materialChipsJson = "[\"牛肉\"]")),
            listOf(TagTriple("暖炖", TagKind.CATEGORY), TagTriple("高蛋白", TagKind.DIET)),
        )

    private fun editor(id: Long?) = RecipeEditorViewModel(repo,
        SavedStateHandle(if (id == null) emptyMap() else mapOf(Routes.ARG_RECIPE_ID to id)), settings).also { models += it }

    @Test fun editRoundTripPreservesAmountsMetadataAndConcurrentChanges() = runBlocking {
        val id = createRecipe()
        val original = repo.getForEditing(id)!!
        val vm = editor(id)
        val loaded = withTimeout(10_000) { vm.uiState.first { !it.loading } }
        assertEquals("600", loaded.ingredients.first().amountText)
        assertEquals(4, loaded.servings)
        vm.onTitleChange("新标题")
        repo.setFavorite(id, false)
        repo.recordCook(id, 3)
        vm.onPublish()
        withTimeout(10_000) { vm.uiState.first { it.savedAndExit } }
        val saved = repo.getForEditing(id)!!
        assertEquals(600.0, saved.orderedGroups.first().second.first().baseAmount!!, 0.0)
        assertEquals("少许", saved.orderedGroups.last().second.first().rawDisplay)
        assertEquals(1.0, saved.ingredients.first { it.name == "牛肉" }.gramsPerUnit!!, 0.0)
        assertFalse(saved.recipe.isFavorite)
        assertEquals(7, saved.recipe.cookCount)
        assertEquals(original.recipe.createdAt, saved.recipe.createdAt)
        assertEquals(original.recipe.subtitle, saved.recipe.subtitle)
        assertEquals(original.recipe.story, saved.recipe.story)
        assertEquals(original.recipe.editorial, saved.recipe.editorial)
        assertEquals(original.recipe.category, saved.recipe.category)
        assertEquals(original.orderedGroups.map { it.first.name }, saved.orderedGroups.map { it.first.name })
        assertEquals(original.tags.map { it.name }.toSet(), saved.tags.map { it.name }.toSet())
        assertEquals(original.steps.first().bulletsJson, saved.steps.first().bulletsJson)
        assertEquals(original.steps.first().materialChipsJson, saved.steps.first().materialChipsJson)
        assertEquals(original.steps.first().timerLabel, saved.steps.first().timerLabel)
        assertEquals(original.steps.first().chipLabel, saved.steps.first().chipLabel)
        assertEquals("300g (约300g)", repo.getDetailOnce(id, 2)!!.groups.first().items.first().amountLabel)
        assertEquals(1, db.recipeDao().observeCookLogCount().first())
    }

    @Test fun draftCanBeReopenedPublishedAndSavedOnlyOnce() = runBlocking {
        val vm = editor(null)
        withTimeout(10_000) { vm.uiState.first { !it.loading } }
        assertEquals(4, vm.uiState.value.servings)
        vm.onTitleChange("草稿回归")
        vm.onSaveDraft()
        vm.onSaveDraft()
        val result = withTimeout(10_000) { vm.uiState.first { it.savedAndExit } }.savedResult!!
        assertTrue(result.isDraft)
        vm.onExitHandled()
        vm.onSaveDraft()
        assertEquals(1, repo.observeDrafts().first().size)
        assertTrue(repo.observeLibrary().first().isEmpty())
        val reopened = editor(result.recipeId)
        withTimeout(10_000) { reopened.uiState.first { !it.loading } }
        val step = reopened.uiState.value.steps.firstOrNull()
        if (step == null) reopened.onAddStep()
        reopened.onStepChange(reopened.uiState.value.steps.first().uid) { it.copy(body = "炒熟") }
        reopened.onPublish()
        val published = withTimeout(10_000) { reopened.uiState.first { it.savedAndExit } }.savedResult!!
        assertEquals(result.recipeId, published.recipeId)
        assertFalse(published.isDraft)
        assertTrue(repo.observeDrafts().first().isEmpty())
        assertEquals(1, repo.observeLibrary().first().size)
    }

    @Test fun publishRejectsMissingTitleOrStepsAndMissingRecipeCannotOverwrite() = runBlocking {
        val vm = editor(null)
        withTimeout(10_000) { vm.uiState.first { !it.loading } }
        vm.onPublish()
        assertFalse(vm.uiState.value.savedAndExit)
        vm.onTitleChange("只有标题")
        vm.onPublish()
        assertEquals("请至少填写一个烹饪步骤", vm.uiState.value.toast)
        assertTrue(repo.observeLibrary().first().isEmpty())
        val id = createRecipe()
        val editing = editor(id)
        withTimeout(10_000) { editing.uiState.first { !it.loading } }
        repo.delete(id)
        editing.onPublish()
        withTimeout(10_000) { editing.uiState.first { !it.isSaving && it.toast != null } }
        assertFalse(editing.uiState.value.savedAndExit)
        assertEquals("回归炖菜", editing.uiState.value.title)
    }

    @Test fun databaseSearchMatchesAllFieldsAndLiteralWildcardsWithoutDraftsOrDuplicates() = runBlocking {
        val id = createRecipe()
        createRecipe(draft = true)
        for (q in listOf("牛肉", "测试厨师", "暖炖", "原副标题", "高蛋白")) {
            assertEquals(listOf(id), repo.search(q).first().map { it.id })
        }
        assertTrue(repo.search("%").first().isEmpty())
        assertTrue(repo.search("_").first().isEmpty())
        assertTrue(repo.search("\\").first().isEmpty())
        val row = repo.getForEditing(id)!!.recipe
        db.recipeDao().updateRecipe(row.copy(title = "100%_\\完成"))
        assertEquals(id, db.recipeDao().search(searchPattern("%_\\")).first().single().recipe.id)
    }

    @Test fun suggestionsReactToIngredientsAndDoNotFallBackToUnrelatedRecipes() = runBlocking {
        val id = createRecipe()
        assertEquals(listOf(id), repo.observeSuggestions(listOf("牛肉")).first().map { it.id })
        assertTrue(repo.observeSuggestions(listOf("苹果")).first().isEmpty())
        assertTrue(repo.observeSuggestions(emptyList()).first().isEmpty())
    }

    @Test fun detailUsesPreferenceThenKeepsManualSelectionAndCookingRecordsItOnce() = runBlocking {
        val id = createRecipe()
        val handle = SavedStateHandle(mapOf<String, Any>(Routes.ARG_RECIPE_ID to id))
        val detail = RecipeDetailViewModel(repo, handle, settings).also { models += it }
        assertEquals(4, withTimeout(10_000) { detail.uiState.first { !it.loading } }.servings)
        detail.onServingsDelta(-1)
        settings.setDefaultServings(1)
        assertEquals(3, withTimeout(10_000) { detail.uiState.first { it.servings == 3 } }.servings)
        assertEquals(3, handle.get<Int>("selectedServings"))
        val cook = CookingViewModel(repo, settings,
            SavedStateHandle(mapOf<String, Any>(Routes.ARG_RECIPE_ID to id, Routes.ARG_SERVINGS to 3, Routes.ARG_START_STEP to 99))).also { models += it }
        val state = withTimeout(10_000) { cook.uiState.first { !it.loading } }
        assertEquals(0, state.stepIndex)
        assertEquals(3, state.servings)
        cook.onNextStep()
        cook.onNextStep()
        withTimeout(10_000) { cook.uiState.first { it.finished } }
        cook.onNextStep()
        assertEquals(1, db.recipeDao().observeCookLogCount().first())
        val cursor = db.openHelper.readableDatabase.query("SELECT servings FROM cook_logs")
        cursor.use { assertTrue(it.moveToFirst()); assertEquals(3, it.getInt(0)) }
    }

    @Test fun emptyMissingAndLegacyCookingRoutesAreSafe() = runBlocking {
        val empty = createRecipe(emptySteps = true)
        for (id in listOf(empty, 99999L)) {
            val vm = CookingViewModel(repo, settings, SavedStateHandle(mapOf<String, Any>(Routes.ARG_RECIPE_ID to id))).also { models += it }
            val state = withTimeout(10_000) { vm.uiState.first { !it.loading } }
            assertNotNull(state.error)
            assertNull(state.currentStep)
            vm.onNextStep()
            assertFalse(vm.uiState.value.finished)
        }
        val valid = createRecipe()
        val legacy = CookingViewModel(repo, settings, SavedStateHandle(mapOf<String, Any>(Routes.ARG_RECIPE_ID to valid, Routes.ARG_SERVINGS to -1))).also { models += it }
        assertEquals(4, withTimeout(10_000) { legacy.uiState.first { !it.loading } }.servings)
        assertEquals(0, db.recipeDao().observeCookLogCount().first())
    }

    @Test fun coverImportSurvivesReferencesAndReportsInvalidInput() = runBlocking {
        val source = File(context.cacheDir, "regression-cover.png")
        val bitmap = Bitmap.createBitmap(2400, 1200, Bitmap.Config.ARGB_8888)
        try {
            source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val ref = ImageStorage.copyIntoAppStorage(context, Uri.fromFile(source))!!
            val cover = CoverReferences.resolve(context.filesDir, ref)!!
            try {
                assertTrue(ref.startsWith("cover:"))
                assertEquals(cover, CoverReferences.resolve(context.filesDir, "/data/old/files/covers/" + cover.name))
                val imported = android.graphics.BitmapFactory.decodeFile(cover.absolutePath)
                assertTrue(maxOf(imported.width, imported.height) <= 1600)
                imported.recycle()
            } finally { cover.delete() }
            assertNull(ImageStorage.copyIntoAppStorage(context, Uri.fromFile(File(context.cacheDir, "missing-regression-image"))))
        } finally { bitmap.recycle(); source.delete() }
    }
}
