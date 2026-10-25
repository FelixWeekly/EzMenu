package com.ezmenu.app

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ezmenu.app.data.local.entity.RecipeEntity
import com.ezmenu.app.data.local.entity.StepEntity
import com.ezmenu.app.data.local.entity.EditorDraftEntity
import com.ezmenu.app.ui.screens.editor.EditorDraftCodec
import com.ezmenu.app.ui.screens.editor.EditorUiState
import com.ezmenu.app.util.CoverReferences
import com.ezmenu.app.util.ImageStorage
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/** 可分别执行两个阶段，并在中间 force-stop，验证真正的进程重启。 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class RecipeRestartRegressionTest {
    private val app get() = ApplicationProvider.getApplicationContext<Context>() as EzMenuApplication
    private val fixture get() = File(app.cacheDir, "regression-restart.json")

    private suspend fun clean(data: JSONObject) {
        data.optString("sessionId").takeIf{it.isNotBlank()}?.let{app.services.cookingSessions.abandon(it)}
        data.optLong("recipeId").takeIf{it>0}?.let{app.services.database.journalDao().deleteRecipeDraft(it)}
        app.services.settingsRepository.setDefaultServings(data.getInt("previousServings"))
        data.optLong("recipeId").takeIf { it > 0 }?.let { app.services.recipeRepository.delete(it) }
        CoverReferences.resolve(app.filesDir, data.optString("cover"))?.delete()
        fixture.delete()
    }

    @Test fun a_preparePersistentRecipeAndPreference(): Unit = runBlocking {
        if (fixture.exists()) clean(JSONObject(fixture.readText()))
        val data = JSONObject().put("previousServings", app.services.settingsRepository.defaultServings.first())
            .put("pid", Process.myPid())
        fixture.writeText(data.toString())
        val source = File(app.cacheDir, "regression-restart-source.png")
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        try {
            source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val ref = ImageStorage.copyIntoAppStorage(app, Uri.fromFile(source))!!
            data.put("cover", ref)
            fixture.writeText(data.toString())
            val id = app.services.database.recipeDao().saveFullGraph(
                RecipeEntity(title = "重启持久化回归", isDraft = false, coverImageRef = ref),
                emptyList(), emptyList(), listOf(StepEntity(recipeId=0,title="重启计时",body="恢复原有步骤",timerSeconds=120)), emptyList())
            data.put("recipeId", id)
            fixture.writeText(data.toString())
            val session=app.services.cookingSessions.start(id,3)
            app.services.cookingSessions.changeTimer(session.id,0,"toggle")
            app.services.database.journalDao().saveDraft(EditorDraftEntity("recipe:"+id,id,EditorDraftCodec.encode(EditorUiState(recipeId=id,title="重启前的编辑内容")),"重启前的编辑内容",System.currentTimeMillis()))
            data.put("sessionId",session.id);fixture.writeText(data.toString())
            app.services.settingsRepository.setDefaultServings(4)
        } catch (e: Exception) {
            clean(data)
            throw e
        } finally { bitmap.recycle(); source.delete() }
    }

    @Test fun b_verifyPersistentRecipeAndPreference(): Unit = runBlocking {
        assertTrue("先运行 a_preparePersistentRecipeAndPreference", fixture.exists())
        val data = JSONObject(fixture.readText())
        try {
            if (InstrumentationRegistry.getArguments().getString("verifyProcessRestart") == "true") {
                assertNotEquals(data.getInt("pid"), Process.myPid())
            }
            assertEquals(4, app.services.settingsRepository.defaultServings.first())
            val session=app.services.cookingSessions.recoverCurrent()!!
            assertEquals(data.getString("sessionId"),session.id);assertEquals(3,session.servings)
            val timer=app.services.database.journalDao().timers(session.id).single()
            assertTrue(app.services.cookingSessions.remaining(timer) in 0..120000L)
            val draft=app.services.database.journalDao().draft("recipe:"+data.getLong("recipeId"))!!
            assertEquals("重启前的编辑内容",EditorDraftCodec.decode(draft.payload,EditorUiState()).title)
            val recipe = app.services.recipeRepository.getForEditing(data.getLong("recipeId"))!!.recipe
            assertEquals(data.getString("cover"), recipe.coverImageRef)
            val file = CoverReferences.resolve(app.filesDir, recipe.coverImageRef)!!
            assertTrue(file.length() > 0)
            assertEquals(file, CoverReferences.resolve(app.filesDir, "/data/previous/files/covers/" + file.name))
        } finally { clean(data) }
    }
}
