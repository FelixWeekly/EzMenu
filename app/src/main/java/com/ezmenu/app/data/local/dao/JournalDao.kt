package com.ezmenu.app.data.local.dao

import androidx.room.*
import com.ezmenu.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM cook_logs WHERE (:recipeId IS NULL OR recipeId=:recipeId) ORDER BY cookedAt DESC,id DESC LIMIT :limit")
    fun timeline(recipeId: Long? = null, limit: Int = 60): Flow<List<CookLogEntity>>
    @Query("SELECT * FROM cook_logs WHERE id=:id") suspend fun entry(id: Long): CookLogEntity?
    @Query("SELECT * FROM cook_logs WHERE id=:id") fun observeEntry(id: Long): Flow<CookLogEntity?>
    @Query("SELECT * FROM cook_logs WHERE sessionId=:id") suspend fun entryForSession(id: String): CookLogEntity?
    @Query("SELECT * FROM cook_logs WHERE recipeId=:recipeId AND source='USER' ORDER BY cookedAt DESC,id DESC LIMIT 1")
    fun latest(recipeId: Long): Flow<CookLogEntity?>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertEntry(entry: CookLogEntity): Long
    @Query("UPDATE cook_logs SET note=:note,nextTime=:nextTime,photosJson=:photos WHERE id=:id")
    suspend fun reflection(id: Long, note: String, nextTime: String, photos: String)
    @Query("SELECT COUNT(*) FROM cook_logs WHERE source='USER'") fun personalCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM cook_logs WHERE source='LEGACY'") fun legacyCount(): Flow<Int>

    @Query("SELECT * FROM editor_drafts ORDER BY updatedAt DESC") fun drafts(): Flow<List<EditorDraftEntity>>
    @Query("SELECT * FROM editor_drafts WHERE key=:key") suspend fun draft(key: String): EditorDraftEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveDraft(draft: EditorDraftEntity)
    @Query("DELETE FROM editor_drafts WHERE key=:key") suspend fun deleteDraft(key: String)
    @Query("DELETE FROM editor_drafts WHERE recipeId=:id") suspend fun deleteRecipeDraft(id: Long)

    @Query("SELECT * FROM cooking_sessions WHERE status IN ('ACTIVE','INTERRUPTED') ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<CookingSession?>
    @Query("SELECT * FROM cooking_sessions WHERE status IN ('ACTIVE','INTERRUPTED') ORDER BY startedAt DESC LIMIT 1")
    suspend fun active(): CookingSession?
    @Query("SELECT * FROM cooking_sessions WHERE id=:id") suspend fun session(id: String): CookingSession?
    @Query("SELECT * FROM cooking_sessions WHERE id=:id") fun observeSession(id: String): Flow<CookingSession?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveSession(session: CookingSession)
    @Query("UPDATE cooking_sessions SET status=:status WHERE id=:id") suspend fun setStatus(id: String, status: String)
    @Query("UPDATE cooking_sessions SET stepIndex=:step WHERE id=:id AND status='ACTIVE'") suspend fun setStep(id: String, step: Int)
    @Query("UPDATE cooking_sessions SET checkedJson=:checked WHERE id=:id") suspend fun setChecked(id: String, checked: String)
    @Query("SELECT * FROM cooking_timers WHERE sessionId=:id ORDER BY stepIndex") fun observeTimers(id: String): Flow<List<CookingTimer>>
    @Query("SELECT * FROM cooking_timers WHERE sessionId=:id") suspend fun timers(id: String): List<CookingTimer>
    @Query("SELECT * FROM cooking_timers WHERE state='RUNNING'") suspend fun runningTimers(): List<CookingTimer>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveTimer(timer: CookingTimer)
    @Query("UPDATE cooking_timers SET state='PAUSED',deadlineElapsed=NULL WHERE sessionId=:id") suspend fun pauseTimers(id: String)
    @Query("SELECT value FROM app_meta WHERE key=:key") suspend fun meta(key: String): String?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveMeta(meta: AppMeta)
}
