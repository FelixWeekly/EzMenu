package com.ezmenu.app

import android.app.Application
import com.ezmenu.app.data.local.DatabaseSeeder
import com.ezmenu.app.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EzMenuApplication : Application() {

    /** 供 ViewModel 通过 `AndroidViewModel` / 工厂取用。 */
    lateinit var services: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        services = ServiceLocator(this)

        // 首启播种放在应用级协程里，不阻塞 Activity 启动 ——
        // 界面会在数据到达时自行刷新，用户不会看到冷启动卡顿。
        CoroutineScope(SupervisorJob()).launch {
            try {
                services.archiveRepository.recoverIfNeeded()
                DatabaseSeeder.seed(this@EzMenuApplication, services.database)
                services.cookingSessions.recoverCurrent()
            } catch (e: Exception) { services.initializationError.value = e.message ?: "本地档案加载失败" }
            services.ready.value = true
        }
    }
}
