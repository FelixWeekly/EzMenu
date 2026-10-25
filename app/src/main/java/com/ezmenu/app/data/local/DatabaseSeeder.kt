package com.ezmenu.app.data.local

import android.content.Context
import androidx.room.withTransaction
import com.ezmenu.app.data.local.entity.AppMeta
import com.ezmenu.app.data.seed.SeedLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseSeeder {
    suspend fun seed(context: Context, database: EzMenuDatabase) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val journal = database.journalDao()
            if (journal.meta("catalog.v2") == "2") return@withTransaction
            val dao = database.recipeDao()
            SeedLoader.load(context).recipes.forEachIndexed { index, seed ->
                val existing=dao.findSource("builtin:"+index)
                dao.saveFullGraph(seed.recipe.copy(id=existing?.id?:0,origin="BUILTIN", sourceKey="builtin:" + index,
                    isFavorite=false, cookCount=0, isDraft=false, author="旬食编辑部"),
                    seed.groups, seed.ingredientsByGroup, seed.steps, seed.tags)
            }
            journal.saveMeta(AppMeta("catalog.v2", "2"))
        }
    }
}
