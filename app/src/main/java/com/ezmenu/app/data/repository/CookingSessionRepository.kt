package com.ezmenu.app.data.repository

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.room.withTransaction
import com.ezmenu.app.data.local.EzMenuDatabase
import com.ezmenu.app.data.local.entity.*
import com.ezmenu.app.domain.TimerClock
import com.ezmenu.app.domain.TimerMath
import com.ezmenu.app.domain.model.RecipeSnapshot
import com.ezmenu.app.util.CookingAlarms
import java.util.UUID

class ExistingCookingSession(val session: CookingSession) : IllegalStateException("已有进行中的烹饪，请先继续或结束它")

class CookingSessionRepository(
    private val db: EzMenuDatabase,
    private val clock: TimerClock,
    private val alarms: CookingAlarms? = null,
) {
    val dao = db.journalDao()
    fun active() = dao.observeActive()
    fun observe(id: String) = dao.observeSession(id)
    fun timers(id: String) = dao.observeTimers(id)
    fun remaining(timer: CookingTimer): Long = TimerMath.remaining(timer.remainingMillis,
        timer.deadlineElapsed,clock.elapsedMillis(),timer.state=="RUNNING" && timer.bootCount==clock.bootCount())

    suspend fun start(recipeId: Long, servings: Int, startStep: Int = 0): CookingSession = db.withTransaction {
        val active = dao.active()
        if (active != null) {
            if (active.recipeId == recipeId) return@withTransaction recover(active)
            throw ExistingCookingSession(active)
        }
        val graph = requireNotNull(db.recipeDao().getById(recipeId)) { "菜谱不存在" }
        require(graph.recipe.trashedAt == null) { "菜谱已在回收站" }
        require(graph.steps.isNotEmpty()) { "没有可引导的步骤" }
        val snapshot = RecipeSnapshot(graph,servings.coerceIn(1,10))
        val session = CookingSession(UUID.randomUUID().toString(),recipeId,snapshot.encode(),snapshot.servings,
            startStep.coerceIn(0,graph.steps.lastIndex),startedAt=clock.wallMillis(),bootCount=clock.bootCount())
        dao.saveSession(session)
        graph.orderedSteps.forEachIndexed { index, step -> step.timerSeconds?.takeIf { it>0 }?.let {
            dao.saveTimer(CookingTimer(session.id,index,it,it*1000L,bootCount=clock.bootCount()))
        } }
        session
    }
    suspend fun recoverCurrent(): CookingSession? = dao.active()?.let { recover(it) }
    private suspend fun recover(s: CookingSession): CookingSession {
        if (s.bootCount==clock.bootCount()) return s
        val recovered = s.copy(status="INTERRUPTED",bootCount=clock.bootCount())
        dao.saveSession(recovered)
        dao.timers(s.id).forEach { dao.saveTimer(it.copy(state="PAUSED",deadlineElapsed=null,bootCount=clock.bootCount())) }
        return recovered
    }
    suspend fun confirmResume(id: String) { dao.setStatus(id,"ACTIVE") }
    suspend fun step(id: String, index: Int) {
        val s = requireNotNull(dao.session(id)); val count=RecipeSnapshot.decode(s.snapshot).graph.steps.size
        dao.setStep(id,index.coerceIn(0,(count-1).coerceAtLeast(0)))
    }
    suspend fun checked(id: String, checked: Set<Long>) = dao.setChecked(id,org.json.JSONArray(checked.toList()).toString())
    suspend fun changeTimer(id: String, index: Int, action: String) {
        val updated = db.withTransaction {
            val session = requireNotNull(dao.session(id))
            require(session.status=="ACTIVE") { "请先恢复烹饪" }
            val old=dao.timers(id).firstOrNull { it.stepIndex==index } ?: return@withTransaction null
            val left=remaining(old)
            val next=when(action) {
                "toggle" -> if(old.state=="RUNNING") old.copy(remainingMillis=left,deadlineElapsed=null,state="PAUSED")
                    else { val n=left.takeIf { it>0 } ?: old.durationSeconds*1000L
                        old.copy(remainingMillis=n,deadlineElapsed=clock.elapsedMillis()+n,state="RUNNING",bootCount=clock.bootCount()) }
                "add" -> old.copy(durationSeconds=old.durationSeconds+30,remainingMillis=left+30000,
                    deadlineElapsed=if(old.state=="RUNNING") clock.elapsedMillis()+left+30000 else null)
                "reset" -> old.copy(remainingMillis=old.durationSeconds*1000L,deadlineElapsed=null,state="PAUSED")
                else -> old
            }
            dao.saveTimer(next); next
        }
        updated?.let { alarms?.schedule(it) }
    }
    suspend fun finishDueTimers() {
        dao.runningTimers().filter { it.bootCount==clock.bootCount() && remaining(it)==0L }.forEach {
            dao.saveTimer(it.copy(state="DONE",remainingMillis=0,deadlineElapsed=null))
            alarms?.notifyDone(it)
        }
    }
    suspend fun complete(id: String): Long {
        val result=db.withTransaction {
            dao.entryForSession(id)?.let { return@withTransaction it.id }
            val s=requireNotNull(dao.session(id)) { "烹饪会话不存在" }
            require(s.status=="ACTIVE") { "请先恢复烹饪" }
            val snap=RecipeSnapshot.decode(s.snapshot)
            val r=snap.graph.recipe
            val logId=dao.insertEntry(CookLogEntity(recipeId=r.id,cookedAt=clock.wallMillis(),servings=s.servings,
                sessionId=id,snapshot=s.snapshot,recipeTitle=r.title,coverRef=r.coverImageRef))
            check(logId>0)
            db.recipeDao().incrementCookCount(r.id)
            dao.setStatus(id,"COMPLETED");dao.pauseTimers(id)
            logId
        }
        dao.timers(id).forEach { alarms?.cancel(it) }
        return result
    }
    suspend fun abandon(id: String) {
        dao.timers(id).forEach { alarms?.cancel(it) }
        db.withTransaction { dao.setStatus(id,"ABANDONED");dao.pauseTimers(id) }
    }
    companion object {
        fun clock(context: Context) = object : TimerClock {
            override fun elapsedMillis()=SystemClock.elapsedRealtime()
            override fun wallMillis()=System.currentTimeMillis()
            private val installation by lazy {
                synchronized(CookingSessionRepository::class.java) {
                    val file=java.io.File(context.noBackupFilesDir,"cooking-installation")
                    if(file.isFile && file.readText().isNotBlank())file.readText()
                    else UUID.randomUUID().toString().also{file.parentFile?.mkdirs();file.writeText(it)}
                }
            }
            override fun bootCount()=(installation+":"+Settings.Global.getInt(context.contentResolver,Settings.Global.BOOT_COUNT,0)).hashCode()
        }
    }
}
