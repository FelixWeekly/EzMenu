package com.ezmenu.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ezmenu.app.data.local.entity.*
import com.ezmenu.app.data.local.dao.JournalDao
import com.ezmenu.app.data.local.dao.RecipeDao
import com.ezmenu.app.data.local.entity.CookLogEntity
import com.ezmenu.app.data.local.entity.IngredientEntity
import com.ezmenu.app.data.local.entity.IngredientGroupEntity
import com.ezmenu.app.data.local.entity.PantryItemEntity
import com.ezmenu.app.data.local.entity.RecipeEntity
import com.ezmenu.app.data.local.entity.RecipeTagCrossRef
import com.ezmenu.app.data.local.entity.StepEntity
import com.ezmenu.app.data.local.entity.TagEntity

@Database(
    entities = [
        RecipeEntity::class,
        IngredientGroupEntity::class,
        IngredientEntity::class,
        StepEntity::class,
        TagEntity::class,
        RecipeTagCrossRef::class,
        CookLogEntity::class,
        PantryItemEntity::class,
        EditorDraftEntity::class, CookingSession::class, CookingTimer::class, AppMeta::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class EzMenuDatabase : RoomDatabase() {

    abstract fun recipeDao(): RecipeDao
    abstract fun journalDao(): JournalDao

    companion object {
        private const val DB_NAME = "ezmenu.db"

        fun build(context: Context): EzMenuDatabase =
            Room.databaseBuilder(context, EzMenuDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
