package com.ezmenu.app.di

import android.content.Context
import com.ezmenu.app.data.local.EzMenuDatabase
import com.ezmenu.app.data.repository.RecipeRepository
import com.ezmenu.app.data.repository.SettingsRepository

/**
 * 手写依赖装配，替代 Hilt。
 *
 * 这个应用只有两个仓储、一个数据库、一个 DataStore，用 Hilt 要额外引入
 * hilt-android-gradle-plugin 与 KSP 处理器 —— 多一个 Gradle 插件就多一处
 * 依赖解析风险，而收益在这个规模下接近于零。
 *
 * 生命周期跟随 Application，用 by lazy 保证单例且线程安全。
 */
class ServiceLocator(context: Context) {

    private val appContext = context.applicationContext

    val database: EzMenuDatabase by lazy { EzMenuDatabase.build(appContext) }

    val recipeRepository: RecipeRepository by lazy { RecipeRepository(database.recipeDao(), database.journalDao()) }

    val archiveRepository by lazy { com.ezmenu.app.data.repository.ArchiveRepository(appContext,database,settingsRepository) }
    val ready = kotlinx.coroutines.flow.MutableStateFlow(false)
    val initializationError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val cookingSessions by lazy { com.ezmenu.app.data.repository.CookingSessionRepository(database, com.ezmenu.app.data.repository.CookingSessionRepository.clock(appContext), com.ezmenu.app.util.CookingAlarms(appContext)) }
    val journalRepository by lazy { com.ezmenu.app.data.repository.JournalRepository(database.journalDao()) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
}
