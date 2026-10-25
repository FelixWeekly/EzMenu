package com.ezmenu.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE recipes ADD COLUMN origin TEXT NOT NULL DEFAULT 'USER'")
        db.execSQL("ALTER TABLE recipes ADD COLUMN sourceKey TEXT")
        db.execSQL("ALTER TABLE recipes ADD COLUMN trashedAt INTEGER")
        db.execSQL("UPDATE recipes SET origin = 'LEGACY'")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_recipes_sourceKey ON recipes(sourceKey)")
        db.execSQL("CREATE TABLE cook_logs_v2 (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, recipeId INTEGER, cookedAt INTEGER NOT NULL, servings INTEGER NOT NULL, sessionId TEXT, snapshot TEXT, recipeTitle TEXT NOT NULL, coverRef TEXT, note TEXT NOT NULL, nextTime TEXT NOT NULL, photosJson TEXT NOT NULL, source TEXT NOT NULL)")
        db.execSQL("INSERT INTO cook_logs_v2 (id,recipeId,cookedAt,servings,recipeTitle,coverRef,note,nextTime,photosJson,source) SELECT c.id,c.recipeId,c.cookedAt,c.servings,COALESCE(r.title,''),r.coverImageRef,'','','[]','LEGACY' FROM cook_logs c LEFT JOIN recipes r ON c.recipeId=r.id")
        db.execSQL("DROP TABLE cook_logs")
        db.execSQL("ALTER TABLE cook_logs_v2 RENAME TO cook_logs")
        db.execSQL("CREATE INDEX index_cook_logs_recipeId ON cook_logs(recipeId)")
        db.execSQL("CREATE UNIQUE INDEX index_cook_logs_sessionId ON cook_logs(sessionId)")
        db.execSQL("CREATE TABLE IF NOT EXISTS editor_drafts (key TEXT NOT NULL PRIMARY KEY, recipeId INTEGER, payload TEXT NOT NULL, title TEXT NOT NULL, updatedAt INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS cooking_sessions (id TEXT NOT NULL PRIMARY KEY, recipeId INTEGER NOT NULL, snapshot TEXT NOT NULL, servings INTEGER NOT NULL, stepIndex INTEGER NOT NULL, checkedJson TEXT NOT NULL, status TEXT NOT NULL, startedAt INTEGER NOT NULL, bootCount INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS cooking_timers (sessionId TEXT NOT NULL, stepIndex INTEGER NOT NULL, durationSeconds INTEGER NOT NULL, remainingMillis INTEGER NOT NULL, deadlineElapsed INTEGER, bootCount INTEGER NOT NULL, state TEXT NOT NULL, PRIMARY KEY(sessionId,stepIndex))")
        db.execSQL("CREATE TABLE IF NOT EXISTS app_meta (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)")
    }
}
