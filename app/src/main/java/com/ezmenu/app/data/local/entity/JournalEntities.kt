package com.ezmenu.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "editor_drafts")
data class EditorDraftEntity(
    @PrimaryKey val key: String,
    val recipeId: Long?,
    val payload: String,
    val title: String,
    val updatedAt: Long,
)

@Entity(tableName = "cooking_sessions")
data class CookingSession(
    @PrimaryKey val id: String,
    val recipeId: Long,
    val snapshot: String,
    val servings: Int,
    val stepIndex: Int = 0,
    val checkedJson: String = "[]",
    val status: String = "ACTIVE",
    val startedAt: Long,
    val bootCount: Int,
)

@Entity(tableName = "cooking_timers", primaryKeys = ["sessionId", "stepIndex"])
data class CookingTimer(
    val sessionId: String,
    val stepIndex: Int,
    val durationSeconds: Int,
    val remainingMillis: Long,
    val deadlineElapsed: Long? = null,
    val bootCount: Int,
    val state: String = "PAUSED",
)

@Entity(tableName = "app_meta")
data class AppMeta(@PrimaryKey val key: String, val value: String)
