package com.ezmenu.app.domain

import com.ezmenu.app.util.CoverReferences
import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class RecipeRulesTest {
    @Test fun searchTreatsWildcardsAsLiteralCharacters() {
        assertEquals("%a\\%\\_\\\\b%", searchPattern(" a%_\\b "))
        assertEquals("%%", searchPattern("  "))
        assertEquals("%柠檬%", searchPattern("柠檬"))
    }

    @Test fun preferredServingsApplyOnlyUntilAnExplicitSelectionExists() {
        assertEquals(4, initialServings(null, 4))
        assertEquals(3, initialServings(3, 4))
        assertEquals(1, initialServings(-1, 4))
        assertEquals(10, initialServings(null, 20))
    }

    @Test fun emptyCookingStepsAreSafeAndValidIndicesAreClamped() {
        assertNull(cookingStepIndex(0, 0))
        assertNull(cookingStepIndex(8, -1))
        assertEquals(0, cookingStepIndex(-2, 3))
        assertEquals(2, cookingStepIndex(20, 3))
        assertEquals(1, cookingStepIndex(1, 3))
    }

    @Test fun recommendationsUseHitsThenMissingIngredientsThenId() {
        val recipes = listOf(
            RecipeIngredients(4, listOf("西红柿", "鸡蛋", "盐")),
            RecipeIngredients(3, listOf("番茄", "鸡蛋")),
            RecipeIngredients(2, listOf("西红柿", "鸡蛋")),
            RecipeIngredients(1, listOf("鸡蛋")),
            RecipeIngredients(5, listOf("牛肉")),
        )
        assertEquals(listOf(2L, 3L, 4L, 1L), IngredientRecommendations.rank(recipes, listOf("番茄", "鸡蛋")))
        assertEquals(listOf(5L), IngredientRecommendations.rank(recipes, listOf("牛肉")))
        assertTrue(IngredientRecommendations.rank(recipes, emptyList()).isEmpty())
        assertTrue(IngredientRecommendations.rank(recipes, listOf("苹果")).isEmpty())
    }

    @Test fun recommendationsSupportAliasesContainmentAndDuplicateSelections() {
        val recipes = listOf(RecipeIngredients(1, listOf("熟成番茄", "北豆腐")))
        assertEquals(listOf(1L), IngredientRecommendations.rank(recipes, listOf("西红柿", "嫩豆腐", "西红柿")))
    }

    @Test fun portableAndLegacyCoverReferencesResolveAfterDirectoryMigration() {
        val temp = Files.createTempDirectory("ezmenu-cover-test").toFile()
        try {
            val files = File(temp, "new-install/files")
            val cover = File(files, "covers/photo.jpg").apply { parentFile!!.mkdirs(); writeText("image") }
            assertEquals(cover, CoverReferences.resolve(files, "cover:photo.jpg"))
            assertEquals(cover, CoverReferences.resolve(files, "/data/user/0/old.install/files/covers/photo.jpg"))
            assertEquals(cover, CoverReferences.resolve(files, "file:///data/user/0/old.install/files/covers/photo.jpg"))
            assertNull(CoverReferences.resolve(files, "cover:missing.jpg"))
            assertNull(CoverReferences.resolve(files, "cover:../photo.jpg"))
            assertNull(CoverReferences.resolve(files, "drawable:recipe_salmon_hero"))
        } finally { temp.deleteRecursively() }
    }
}
