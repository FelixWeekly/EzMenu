package com.ezmenu.app.data.repository

import com.ezmenu.app.data.local.dao.JournalDao
import com.ezmenu.app.domain.model.RecipeSnapshot
import org.json.JSONArray

class JournalRepository(private val dao: JournalDao) {
    fun timeline(recipeId: Long? = null, limit: Int = 60) = dao.timeline(recipeId, limit)
    fun entry(id: Long) = dao.observeEntry(id)
    fun latest(recipeId: Long) = dao.latest(recipeId)
    suspend fun saveReflection(id: Long, note: String, nextTime: String, photos: List<String>) {
        require(photos.size <= 3) { "最多保存三张照片" }
        requireNotNull(dao.entry(id)) { "手记不存在" }
        dao.reflection(id, note.trim(), nextTime.trim(), JSONArray(photos).toString())
    }
    companion object {
        fun photos(json: String): List<String> = runCatching {
            val a = JSONArray(json); (0 until a.length()).map { a.getString(it) }
        }.getOrDefault(emptyList())
        fun snapshot(json: String?) = json?.let { runCatching { RecipeSnapshot.decode(it) }.getOrNull() }
    }
}
