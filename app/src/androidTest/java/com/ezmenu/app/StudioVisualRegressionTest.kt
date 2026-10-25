package com.ezmenu.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.room.withTransaction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import com.ezmenu.app.data.local.entity.*
import com.ezmenu.app.domain.model.RecipeSnapshot
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import java.io.File

class StudioVisualRegressionTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var app: EzMenuApplication
    private var oldTheme="system";private var recipeId=0L;private var logId=0L
    private val extraIds=mutableListOf<Long>()
    private val args=InstrumentationRegistry.getArguments()
    private val suffix get()=(args.getString("visualName")?:"default")
    @Before fun setup(): Unit=runBlocking {
        app=ApplicationProvider.getApplicationContext();withTimeout(20000){app.services.ready.first{it}}
        oldTheme=app.services.settingsRepository.theme.first();app.services.settingsRepository.setTheme(args.getString("theme")?:"light")
        recipeId=app.services.database.recipeDao().saveFullGraph(RecipeEntity(title="周末的番茄炖牛肉",story="番茄与牛肉慢炖，汤汁浓郁。",coverImageRef="drawable:recipe_beef_stew",category="家常慢炖",cookTimeMinutes=90),
            listOf(IngredientGroupEntity(recipeId=0,name="主料")),listOf(listOf(IngredientEntity(recipeId=0,groupId=0,name="牛肉",baseAmount=500.0,unitLabel="g"))),
            listOf(StepEntity(recipeId=0,title="慢炖入味",body="耐心等待，直到牛肉软烂、汤汁浓郁。",timerSeconds=60),StepEntity(recipeId=0,title="开盖收汁",body="开盖小火收浓汤汁，调味后盛盘。")),emptyList())
        listOf(Triple("香煎迷迭香三文鱼排","drawable:recipe_salmon_card",18),Triple("牛油果溏心蛋吐司","drawable:recipe_avocado_toast",10),Triple("野菌燕麦浓汤","drawable:recipe_mushroom_soup",25)).forEachIndexed{index,(title,cover,time)->
            extraIds+=app.services.database.recipeDao().saveFullGraph(RecipeEntity(title=title,coverImageRef=cover,cookTimeMinutes=time,category=if(index==1)"元气早餐" else "轻卡减脂",updatedAt=System.currentTimeMillis()-60000-index),emptyList(),emptyList(),emptyList(),emptyList())
        }
        val graph=app.services.database.recipeDao().getById(recipeId)!!
        logId=app.services.database.journalDao().insertEntry(CookLogEntity(recipeId=recipeId,cookedAt=System.currentTimeMillis(),recipeTitle=graph.recipe.title,coverRef=graph.recipe.coverImageRef,snapshot=RecipeSnapshot(graph,2).encode(),note="今天的汤汁很香，留一点拌饭刚刚好。",nextTime="番茄再多一点，最后十分钟开盖收汁。"))
    }
    @After fun cleanup(): Unit=runBlocking {
        app.services.database.openHelper.writableDatabase.execSQL("DELETE FROM cook_logs WHERE id=?",arrayOf(logId))
        app.services.cookingSessions.recoverCurrent()?.takeIf{it.recipeId==recipeId}?.let{app.services.cookingSessions.abandon(it.id)}
        app.services.database.journalDao().deleteRecipeDraft(recipeId)
        extraIds.forEach{app.services.database.recipeDao().deleteRecipe(it)}
        app.services.database.recipeDao().deleteRecipe(recipeId);app.services.settingsRepository.setTheme(oldTheme)
    }
    private fun waitText(text: String){compose.waitUntil(20000){compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()}}
    private fun shot(name: String){compose.waitForIdle();if(args.getString("cinematic")=="true"&&name=="library")File(app.cacheDir,"preview-start-ms").writeText(android.os.SystemClock.elapsedRealtime().toString());val b=compose.onRoot().captureToImage().asAndroidBitmap();File(app.cacheDir,"studio-"+suffix+"-"+name+".png").outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)};if(args.getString("cinematic")=="true"&&name=="settings"){Thread.sleep(800);File(app.cacheDir,"preview-end-ms").writeText(android.os.SystemClock.elapsedRealtime().toString())}}
    private fun previewAction(action: ()->Unit){
        if(args.getString("cinematic")!="true"){action();return}
        compose.mainClock.autoAdvance=false
        try{action();repeat(30){compose.mainClock.advanceTimeBy(16);Thread.sleep(16)}}finally{compose.mainClock.autoAdvance=true}
    }
    private fun SemanticsNodeInteraction.previewClick(){
        if(args.getString("cinematic")!="true"){performClick();return}
        previewAction{performTouchInput{down(center)};repeat(5){compose.mainClock.advanceTimeBy(16);Thread.sleep(16)};performTouchInput{advanceEventTime(80);up()}}
    }
    private fun previewBack(){previewAction{Espresso.pressBack()}}
    @Test fun scrollLargeCookbookAndJournal(): Unit=runBlocking {
        val key="perf-"+System.nanoTime()+"-"
        val db=app.services.database
        try {
            db.withTransaction {
                repeat(1000){db.recipeDao().insertRecipe(RecipeEntity(title="规模食谱"+it,origin="PERF",sourceKey=key+it,updatedAt=System.currentTimeMillis()+it))}
                repeat(5000){db.journalDao().insertEntry(CookLogEntity(recipeId=null,cookedAt=System.currentTimeMillis()+it,recipeTitle="规模手记"+it,source="PERF",sessionId=key+it))}
            }
            waitText("旬食")
            val begin=android.os.SystemClock.elapsedRealtime()
            repeat(5){compose.onNodeWithTag("library-grid").performScrollToIndex(30);compose.onNodeWithTag("library-grid").performScrollToIndex(0)}
            compose.onNodeWithTag("nav-journal").previewClick();waitText("所有食谱")
            repeat(5){compose.onNodeWithTag("journal-list").performScrollToIndex(45);compose.onNodeWithTag("journal-list").performScrollToIndex(0)}
            File(app.cacheDir,"studio-scale-ui.json").writeText("{\"recipes\":1000,\"entries\":5000,\"scrollCycles\":10,\"elapsedMs\":"+(android.os.SystemClock.elapsedRealtime()-begin)+"}")
        } finally {
            db.openHelper.writableDatabase.execSQL("DELETE FROM cook_logs WHERE source='PERF' AND sessionId LIKE ?",arrayOf(key+"%"))
            db.openHelper.writableDatabase.execSQL("DELETE FROM recipes WHERE origin='PERF' AND sourceKey LIKE ?",arrayOf(key+"%"))
        }
    }
    @Test fun visualChaptersAndSharePreviewRemainUsable(){
        waitText("旬食");compose.onNodeWithTag("library-grid").performScrollToNode(hasText("周末的番茄炖牛肉"));shot("library")
        compose.onNodeWithTag("library-grid").performScrollToNode(hasText("周末的番茄炖牛肉"))
        compose.onNodeWithText("周末的番茄炖牛肉").previewClick();waitText("食谱详情");shot("detail")
        if(args.getString("cinematic")=="true"){compose.onNodeWithContentDescription("收藏").previewClick();compose.onNodeWithContentDescription("取消收藏").previewClick()}
        compose.onNodeWithContentDescription("编辑").previewClick();waitText("修改食谱");shot("editor")
        previewBack();waitText("食谱详情")
        compose.onNodeWithTag("recipe-content").performScrollToNode(hasText("开始烹饪"))
        compose.onNodeWithText("开始烹饪").previewClick();waitText("烹饪");shot("cooking")
        if(args.getString("cinematic")=="true"){compose.onNodeWithText("下一步").previewClick();compose.onNodeWithText("上一步").previewClick()}
        previewBack();previewBack()
        compose.onNodeWithTag("nav-discover").previewClick();waitText("搜索灵感食谱");shot("inspiration")
        compose.onNodeWithTag("nav-journal").previewClick();waitText("所有食谱");shot("journal")
        compose.onNodeWithTag("journal-list").performScrollToNode(hasText("周末的番茄炖牛肉"))
        compose.onNodeWithText("周末的番茄炖牛肉").previewClick();waitText("料理手记");shot("entry")
        compose.onNodeWithContentDescription("分享").previewClick();waitText("导出与分享")
        compose.waitUntil(20000){compose.onAllNodesWithContentDescription("分享卡片预览").fetchSemanticsNodes().isNotEmpty()};shot("share")
        previewBack();previewBack()
        compose.onNodeWithTag("nav-profile").previewClick();compose.onNodeWithTag("profile-list").assertExists();shot("profile")
        compose.onNodeWithTag("profile-list").performScrollToNode(hasText("厨房与外观设置"))
        compose.onNodeWithText("厨房与外观设置").previewClick();waitText("外观");shot("settings")
    }
}
