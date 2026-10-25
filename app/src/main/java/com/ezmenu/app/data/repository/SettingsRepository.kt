package com.ezmenu.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ezmenu_settings")

/**
 * 用户偏好。
 *
 * 对应「我的」页的「下厨模式与感官辅助」那组设置。设计稿里那几行只有
 * 「烹饪时屏幕常亮」是真的可切换，其余都是静态的 —— 这里全部接上持久化，
 * 因为一个点了没反应的开关比没有这个开关更糟。
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val NICKNAME = stringPreferencesKey("nickname")
        val LEFTOVERS = stringPreferencesKey("leftovers")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val GESTURE_SENSITIVITY = stringPreferencesKey("gesture_sensitivity")
        val DEFAULT_SERVINGS = intPreferencesKey("default_servings")
        val MEASUREMENT_SYSTEM = stringPreferencesKey("measurement_system")
        val PAPER_TONE = stringPreferencesKey("paper_tone")
    }

    val theme = context.dataStore.data.map { it[Keys.THEME] ?: "system" }
    val nickname = context.dataStore.data.map { it[Keys.NICKNAME] ?: "我的厨房" }
    val leftovers = context.dataStore.data.map { JournalRepository.photos(it[Keys.LEFTOVERS] ?: "[]") }
    suspend fun setTheme(value: String) = context.dataStore.edit { it[Keys.THEME] = value }
    suspend fun setNickname(value: String) = context.dataStore.edit { it[Keys.NICKNAME] = value.trim().ifBlank { "我的厨房" } }
    suspend fun setLeftovers(value: List<String>) = context.dataStore.edit { it[Keys.LEFTOVERS] = org.json.JSONArray(value).toString() }
    suspend fun exportSettings(): org.json.JSONObject {
        val p = context.dataStore.data.first()
        return org.json.JSONObject().apply { p.asMap().forEach { (k,v) -> put(k.name,v) } }
    }
    suspend fun importSettings(json: org.json.JSONObject) = context.dataStore.edit { p ->
        p.clear(); json.keys().forEach { k -> when(val v=json.get(k)) {
            is Boolean -> p[booleanPreferencesKey(k)] = v
            is Int -> p[intPreferencesKey(k)] = v
            is String -> p[stringPreferencesKey(k)] = v
        } }
    }

    /** 烹饪时屏幕常亮 */
    val keepScreenOn: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.KEEP_SCREEN_ON] ?: true }

    /** 手势灵敏度，文案直接存显示值（如「高灵敏 (水湿手友好)」） */
    val gestureSensitivity: Flow<String> =
        context.dataStore.data.map { it[Keys.GESTURE_SENSITIVITY] ?: DEFAULT_GESTURE }

    /** 默认菜谱算量基准（1 / 2 / 4 人份） */
    val defaultServings: Flow<Int> =
        context.dataStore.data.map { it[Keys.DEFAULT_SERVINGS] ?: 2 }

    val measurementSystem: Flow<String> =
        context.dataStore.data.map { it[Keys.MEASUREMENT_SYSTEM] ?: DEFAULT_MEASUREMENT }

    val paperTone: Flow<String> =
        context.dataStore.data.map { it[Keys.PAPER_TONE] ?: DEFAULT_PAPER_TONE }

    suspend fun setKeepScreenOn(value: Boolean) =
        context.dataStore.edit { it[Keys.KEEP_SCREEN_ON] = value }

    suspend fun setGestureSensitivity(value: String) =
        context.dataStore.edit { it[Keys.GESTURE_SENSITIVITY] = value }

    suspend fun setDefaultServings(value: Int) =
        context.dataStore.edit { it[Keys.DEFAULT_SERVINGS] = value }

    suspend fun setMeasurementSystem(value: String) =
        context.dataStore.edit { it[Keys.MEASUREMENT_SYSTEM] = value }

    suspend fun setPaperTone(value: String) =
        context.dataStore.edit { it[Keys.PAPER_TONE] = value }

    companion object {
        /** 设计稿里的默认值，逐字取自「我的」页面 */
        const val DEFAULT_GESTURE = "高灵敏 (水湿手友好)"
        const val DEFAULT_MEASUREMENT = "公制 (g / ml / ℃)"
        const val DEFAULT_PAPER_TONE = "自然米白 (暖调)"
    }
}
