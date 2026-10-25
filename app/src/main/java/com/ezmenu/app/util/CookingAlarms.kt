package com.ezmenu.app.util

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ezmenu.app.MainActivity
import com.ezmenu.app.EzMenuApplication
import com.ezmenu.app.data.local.entity.CookingTimer
import kotlinx.coroutines.*

class CookingAlarms(private val context: Context) {
    private val manager=context.getSystemService(AlarmManager::class.java)
    fun enabled(): Boolean {
        val alarmAllowed=if(Build.VERSION.SDK_INT>=31)manager.canScheduleExactAlarms()else true
        val notificationAllowed=if(Build.VERSION.SDK_INT>=33)ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED else true
        return alarmAllowed && notificationAllowed && context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    }
    private fun intent(t: CookingTimer): PendingIntent {
        val i=Intent(context, CookingAlarmReceiver::class.java).setAction("timer:"+t.sessionId+":"+t.stepIndex)
        return PendingIntent.getBroadcast(context,0,i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    fun cancel(timer: CookingTimer) { manager.cancel(intent(timer)) }
    fun schedule(timer: CookingTimer) {
        cancel(timer)
        if(timer.state!="RUNNING" || !enabled()) return
        try { manager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,requireNotNull(timer.deadlineElapsed),intent(timer)) }
        catch (_: SecurityException) { /* Permission may be revoked between checking and scheduling. */ }
    }
    fun notifyDone(timer: CookingTimer) {
        if(!enabled()) return
        val notifications=context.getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(NotificationChannel("cooking","下厨计时",NotificationManager.IMPORTANCE_HIGH))
        val open=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n=NotificationCompat.Builder(context,"cooking").setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("旬食 · 第"+(timer.stepIndex+1)+"步计时结束")
            .setContentText("回到烹饪，看看这道菜的状态吧。")
            .setContentIntent(open).setAutoCancel(true).build()
        try { notifications.notify((timer.sessionId+timer.stepIndex).hashCode(),n) } catch (_: SecurityException) { }
    }
}

class CookingAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action!=Intent.ACTION_BOOT_COMPLETED && intent.action!=AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED && intent.action?.startsWith("timer:")!=true) return
        val pending=goAsync()
        CoroutineScope(SupervisorJob()+Dispatchers.IO).launch {
            try {
                val repo=(context.applicationContext as EzMenuApplication).services.cookingSessions
                repo.recoverCurrent()
                repo.finishDueTimers()
                repo.dao.runningTimers().forEach { CookingAlarms(context).schedule(it) }
            } finally { pending.finish() }
        }
    }
}
