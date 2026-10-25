package com.ezmenu.app

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.ezmenu.app.data.local.entity.RecipeEntity
import com.ezmenu.app.data.local.entity.StepEntity
import com.ezmenu.app.util.CookingAlarms
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CookingAlarmRegressionTest {
    @Test fun backgroundReminderOrDeniedPermissionFallback(): Unit=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val s=(context as EzMenuApplication).services
        val id=s.database.recipeDao().saveFullGraph(RecipeEntity(title="后台计时回归"),emptyList(),emptyList(),
            listOf(StepEntity(recipeId=0,title="后台计时",body="验证系统到时提醒",timerSeconds=2)),emptyList())
        var sessionId: String?=null
        val alarms=CookingAlarms(context)
        try {
            assertNull("Use a dedicated test emulator with no active cooking session",s.database.journalDao().active())
            val session=s.cookingSessions.start(id,2);sessionId=session.id
            s.cookingSessions.changeTimer(session.id,0,"toggle")
            if(alarms.enabled()) {
                withTimeout(20000){while(s.database.journalDao().timers(session.id).single().state!="DONE")delay(100)}
                val expected=(session.id+0).hashCode()
                withTimeout(10000){while(context.getSystemService(NotificationManager::class.java).activeNotifications.none{it.id==expected})delay(100)}
            } else {
                delay(2200);s.cookingSessions.finishDueTimers()
                assertEquals("DONE",s.database.journalDao().timers(session.id).single().state)
            }
        } finally {
            sessionId?.let{sid->
                s.cookingSessions.abandon(sid)
                context.getSystemService(NotificationManager::class.java).cancel((sid+0).hashCode())
                s.database.openHelper.writableDatabase.execSQL("DELETE FROM cooking_timers WHERE sessionId=?",arrayOf(sid))
                s.database.openHelper.writableDatabase.execSQL("DELETE FROM cooking_sessions WHERE id=?",arrayOf(sid))
            }
            s.database.recipeDao().deleteRecipe(id)
        }
    }
}
