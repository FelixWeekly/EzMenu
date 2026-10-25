package com.ezmenu.app

import android.content.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.*
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ezmenu.app.data.local.*
import com.ezmenu.app.data.local.entity.*
import com.ezmenu.app.data.repository.*
import com.ezmenu.app.domain.TimerClock
import com.ezmenu.app.domain.model.RecipeSnapshot
import com.ezmenu.app.navigation.Routes
import com.ezmenu.app.ui.screens.editor.RecipeEditorViewModel
import com.ezmenu.app.util.RecipeExport
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class StudioDataRegressionTest {
    private lateinit var context: Context
    private lateinit var db: EzMenuDatabase
    private val models=mutableListOf<RecipeEditorViewModel>()
    private class Clock : TimerClock {
        var elapsed=1000L;var wall=1700000000000L;var boot=1
        override fun elapsedMillis()=elapsed;override fun wallMillis()=wall;override fun bootCount()=boot
    }
    @Before fun setup(){context=ApplicationProvider.getApplicationContext();db=Room.inMemoryDatabaseBuilder(context,EzMenuDatabase::class.java).build()}
    @After fun cleanup(){models.forEach{it.viewModelScope.cancel()};db.close()}
    private suspend fun recipe(origin: String="USER"): Long = db.recipeDao().saveFullGraph(RecipeEntity(title="手记回归炖菜",baseServings=4,coverImageRef="drawable:recipe_beef_stew",cookTimeMinutes=90,origin=origin,sourceKey=if(origin=="BUILTIN")"builtin-test" else null),
        listOf(IngredientGroupEntity(recipeId=0,name="主料")),listOf(listOf(IngredientEntity(groupId=0,recipeId=0,name="牛肉",baseAmount=600.0,unitLabel="g"))),
        listOf(StepEntity(recipeId=0,title="煎香",body="煎至表面上色",timerSeconds=30),StepEntity(recipeId=0,title="炖煮",body="炖至软烂",timerSeconds=60)),emptyList())

    @Test fun historicalRecipeSurvivesEditsAndPermanentDeletion(): Unit=runBlocking {
        val id=recipe();val repo=CookingSessionRepository(db,Clock());val session=repo.start(id,2)
        val logId=repo.complete(session.id)
        val graph=db.recipeDao().getById(id)!!;db.recipeDao().updateRecipe(graph.recipe.copy(title="已经改过",baseServings=9))
        db.recipeDao().trash(id,1);db.recipeDao().permanentlyDelete(id)
        val log=db.journalDao().entry(logId)!!;val historical=RecipeSnapshot.decode(log.snapshot!!)
        assertEquals("手记回归炖菜",historical.graph.recipe.title);assertEquals(4,historical.graph.recipe.baseServings)
        assertEquals(2,historical.servings);assertEquals(600.0,historical.graph.ingredients.single().baseAmount!!,0.0)
        assertNull(log.recipeId);assertEquals("煎香",historical.graph.orderedSteps.first().title)
    }
    @Test fun completionIsIdempotentEvenUnderConcurrentRetries(): Unit=runBlocking {
        val id=recipe();val repo=CookingSessionRepository(db,Clock());val session=repo.start(id,3)
        val ids=coroutineScope{(1..15).map{async(Dispatchers.Default){repo.complete(session.id)}}.awaitAll()}
        assertEquals(1,ids.distinct().size);assertEquals(1,db.journalDao().personalCount().first())
        assertEquals(1,db.recipeDao().getById(id)!!.recipe.cookCount)
    }
    @Test fun independentStepTimersPersistAcrossNavigationAndProcessRecreation(): Unit=runBlocking {
        val clock=Clock();val repo=CookingSessionRepository(db,clock);val session=repo.start(recipe(),2)
        repo.changeTimer(session.id,0,"toggle");clock.elapsed+=5000
        repo.step(session.id,1);repo.changeTimer(session.id,1,"toggle");clock.elapsed+=10000
        val recreated=CookingSessionRepository(db,clock);val restored=recreated.recoverCurrent()!!
        assertEquals(1,restored.stepIndex);val timers=db.journalDao().timers(session.id)
        assertEquals(15000,recreated.remaining(timers.first{it.stepIndex==0}));assertEquals(50000,recreated.remaining(timers.first{it.stepIndex==1}))
        clock.boot++;val interrupted=recreated.recoverCurrent()!!;assertEquals("INTERRUPTED",interrupted.status)
        assertTrue(db.journalDao().timers(session.id).all{it.state=="PAUSED"&&it.deadlineElapsed==null})
    }
    @Test fun onlyOneSessionCanBeActiveAndBuiltinsCollectOnlyOnce(): Unit=runBlocking {
        val original=recipe("BUILTIN");val copy=db.recipeDao().collectBuiltin(original)
        assertNotEquals(original,copy);assertEquals(copy,db.recipeDao().collectBuiltin(original))
        assertEquals("USER",db.recipeDao().getById(copy)!!.recipe.origin)
        val repo=CookingSessionRepository(db,Clock());val session=repo.start(copy,2)
        assertEquals(session.id,repo.start(copy,4).id)
        try{repo.start(recipe(),2);fail("must reject concurrent dishes")}catch(expected:ExistingCookingSession){assertEquals(session.id,expected.session.id)}
    }
    @Test fun freshCatalogNeverInventsPersonalActivity(): Unit=runBlocking {
        DatabaseSeeder.seed(context,db);DatabaseSeeder.seed(context,db)
        assertEquals(16,db.recipeDao().browse("%",2,limit=100).first().size)
        assertEquals(0,db.journalDao().personalCount().first());assertEquals(0,db.recipeDao().observePantryCount().first())
        assertEquals(0,db.recipeDao().observePublishedCount().first())
    }
    @Test fun editorAutosaveIsSeparateFromPublishedRecipeAndRestores(): Unit=runBlocking {
        val id=recipe();val repo=RecipeRepository(db.recipeDao(),db.journalDao());val settings=(context as EzMenuApplication).services.settingsRepository
        fun editor()=RecipeEditorViewModel(repo,SavedStateHandle(mapOf(Routes.ARG_RECIPE_ID to id)),settings).also{models+=it}
        val first=editor();withTimeout(10000){first.uiState.first{!it.loading}};first.onTitleChange("草稿中的新标题");first.flushDraft()
        assertEquals("手记回归炖菜",db.recipeDao().getById(id)!!.recipe.title)
        first.viewModelScope.cancel();val second=editor();withTimeout(10000){second.uiState.first{it.title=="草稿中的新标题"}}
        second.discardDraft();assertNull(db.journalDao().draft("recipe:"+id));assertEquals("手记回归炖菜",db.recipeDao().getById(id)!!.recipe.title)
    }
    @Test fun backupRoundTripAndCorruptionPreserveCurrentData(): Unit=runBlocking {
        val temp=File(context.cacheDir,"archive-test-"+UUID.randomUUID()).also{it.mkdirs()}
        val wrapped=object:ContextWrapper(context){override fun getFilesDir()=File(temp,"files").also{it.mkdirs()}}
        val settings=(context as EzMenuApplication).services.settingsRepository;val original=settings.exportSettings()
        try{
            val id=recipe();val sessions=CookingSessionRepository(db,Clock());sessions.complete(sessions.start(id,2).id)
            val photo=File(wrapped.filesDir,"covers/probe.jpg");photo.parentFile!!.mkdirs();photo.writeBytes(byteArrayOf(1,2,3))
            val repo=ArchiveRepository(wrapped,db,settings);val file=repo.export(File(temp,"archive.ezmenu"));assertEquals(1,repo.inspect(file).entries)
            val graph=db.recipeDao().getById(id)!!;db.recipeDao().updateRecipe(graph.recipe.copy(title="临时改名"));repo.restore(file)
            assertEquals("手记回归炖菜",db.recipeDao().getById(id)!!.recipe.title)
            val invalid=File(temp,"invalid.ezmenu");invalid.writeText("corrupted")
            try{repo.restore(invalid);fail("must reject corrupt archive")}catch(expected:Exception){assertEquals(1,db.journalDao().personalCount().first())}
            assertArrayEquals(byteArrayOf(1,2,3),photo.readBytes())
        }finally{settings.importSettings(original);temp.deleteRecursively()}
    }
    @Test fun versionOneMigrationPreservesEveryOldRecord(): Unit=runBlocking {
        val name="migration-"+UUID.randomUUID()+".db"
        val old=context.openOrCreateDatabase(name,0,null)
        val testContext=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context
        val schema=JSONObject(testContext.assets.open("v1-schema.json").bufferedReader().readText()).getJSONObject("database")
        val entities=schema.getJSONArray("entities")
        for(i in 0 until entities.length()){val entity=entities.getJSONObject(i);old.execSQL(entity.getString("createSql").replace("$"+"{TABLE_NAME}",entity.getString("tableName")));val indices=(entity.optJSONArray("indices")?:JSONArray());for(j in 0 until indices.length())old.execSQL(indices.getJSONObject(j).getString("createSql").replace("$"+"{TABLE_NAME}",entity.getString("tableName")))}
        val recipe=entities.getJSONObject(0).getJSONArray("fields");val values=ContentValues()
        for(i in 0 until recipe.length()){val f=recipe.getJSONObject(i);val key=f.getString("columnName");if(!f.optBoolean("notNull"))values.putNull(key)else if(f.getString("affinity")=="TEXT")values.put(key,if(key=="title")"迁移旧菜谱" else "")else values.put(key,if(key=="id")11 else if(key=="isFavorite")1 else if(key=="baseServings")4 else 0)}
        old.insertOrThrow("recipes",null,values);old.execSQL("INSERT INTO cook_logs(id,recipeId,cookedAt,servings) VALUES(1,11,1700000000000,4)");old.version=1;old.close()
        val migrated=Room.databaseBuilder(context,EzMenuDatabase::class.java,name).addMigrations(MIGRATION_1_2).build()
        try{val r=migrated.recipeDao().getById(11)!!.recipe;assertEquals("LEGACY",r.origin);assertTrue(r.isFavorite);assertEquals(4,r.baseServings)
            val log=migrated.journalDao().entry(1)!!;assertEquals("LEGACY",log.source);assertNull(log.snapshot);assertEquals("迁移旧菜谱",log.recipeTitle)
        }finally{migrated.close();context.deleteDatabase(name)}
    }
    @Test fun failedPhotoRestoreRollsBackTheCurrentCookingSession(): Unit=runBlocking {
        val temp=File(context.cacheDir,"restore-io-"+UUID.randomUUID()).also{it.mkdirs()}
        val wrapped=object:ContextWrapper(context){override fun getFilesDir()=File(temp,"files").also{it.mkdirs()}}
        val settings=(context as EzMenuApplication).services.settingsRepository;val original=settings.exportSettings()
        try{
            val id=recipe();val sessions=CookingSessionRepository(db,Clock());val active=sessions.start(id,2)
            val photo=File(wrapped.filesDir,"covers/probe.jpg");photo.parentFile!!.mkdirs();photo.writeBytes(byteArrayOf(1,2,3))
            val repo=ArchiveRepository(wrapped,db,settings);val archive=repo.export(File(temp,"restore.ezmenu"))
            check(photo.delete());check(photo.mkdir())
            try{repo.restore(archive);fail("write failure should roll back")}catch(expected:Exception){
                assertEquals(active.id,db.journalDao().active()!!.id)
                assertEquals("手记回归炖菜",db.recipeDao().getById(id)!!.recipe.title)
            }
        }finally{settings.importSettings(original);temp.deleteRecursively()}
    }
    @Test fun interruptedRestoreRollsBackOnNextStartup(): Unit=runBlocking {
        val temp=File(context.cacheDir,"recover-test-"+UUID.randomUUID()).also{it.mkdirs()}
        val wrapped=object:ContextWrapper(context){override fun getFilesDir()=File(temp,"files").also{it.mkdirs()}}
        val settings=(context as EzMenuApplication).services.settingsRepository;val original=settings.exportSettings()
        try{val id=recipe();val repo=ArchiveRepository(wrapped,db,settings)
            repo.export(File(wrapped.filesDir,"restore-rollback.ezmenu"));File(wrapped.filesDir,"restore.pending").writeText("rollback")
            val graph=db.recipeDao().getById(id)!!;db.recipeDao().updateRecipe(graph.recipe.copy(title="恢复中断的半成品"))
            repo.recoverIfNeeded();assertEquals("手记回归炖菜",db.recipeDao().getById(id)!!.recipe.title);assertFalse(File(wrapped.filesDir,"restore.pending").exists())
        }finally{settings.importSettings(original);temp.deleteRecursively()}
    }
    @Test fun scaledListingsAvoidLoadingRecipeGraphs(): Unit=runBlocking {
        val start=android.os.SystemClock.elapsedRealtime()
        db.withTransaction{
            repeat(1000){db.recipeDao().insertRecipe(RecipeEntity(title="规模食谱"+it,origin="USER",updatedAt=it.toLong()))}
            repeat(5000){db.journalDao().insertEntry(CookLogEntity(recipeId=null,cookedAt=it.toLong(),recipeTitle="规模手记"+it))}
        }
        val samples=mutableListOf<Long>()
        repeat(10){val begin=android.os.SystemClock.elapsedRealtime();assertEquals(40,db.recipeDao().browse("%",limit=40).first().size);assertEquals(60,db.journalDao().timeline().first().size);samples+=android.os.SystemClock.elapsedRealtime()-begin}
        val graphSamples=mutableListOf<Long>()
        repeat(10){val begin=android.os.SystemClock.elapsedRealtime();db.recipeDao().observePublished().first();graphSamples+=android.os.SystemClock.elapsedRealtime()-begin}
        File(context.cacheDir,"studio-performance.json").writeText(JSONObject().put("recipes",1000).put("entries",5000).put("fixtureMs",android.os.SystemClock.elapsedRealtime()-start).put("querySamplesMs",JSONArray(samples)).put("originalGraphQuerySamplesMs",JSONArray(graphSamples)).put("device",android.os.Build.MODEL).put("sdk",android.os.Build.VERSION.SDK_INT).toString())
    }
    @Test fun exportsProduceReadableFilesWithChineseAndImages(): Unit=runBlocking {
        val id=recipe();val graph=db.recipeDao().getById(id)!!;val snapshot=RecipeSnapshot(graph.copy(steps=graph.steps+List(35){index->StepEntity(recipeId=id,sortOrder=index+2,title="长内容步骤"+index,body="这是验证中文长内容自然分页的做法说明。".repeat(8))}),2);val exporter=RecipeExport(context)
        val png=exporter.image(snapshot);val pdf=exporter.pdf(snapshot);val md=exporter.markdown(snapshot)
        assertTrue(png.length()>1000);assertTrue(pdf.readBytes().take(4).toByteArray().contentEquals("%PDF".toByteArray()))
        java.util.zip.ZipFile(md).use{zip->val text=zip.getInputStream(zip.getEntry("recipe.md")).bufferedReader().readText();assertTrue(text.contains("300g"));assertTrue(text.contains("炖煮"))}
        png.copyTo(File(context.cacheDir,"studio-export.png"),overwrite=true);pdf.copyTo(File(context.cacheDir,"studio-export.pdf"),overwrite=true)
    }
}
