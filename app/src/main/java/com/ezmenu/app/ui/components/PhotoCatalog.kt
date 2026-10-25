package com.ezmenu.app.ui.components

import com.ezmenu.app.R

object PhotoCatalog {
    fun resolve(ref: String?): Int? = when(ref?.removePrefix("drawable:")) {
        "recipe_salmon_hero", "recipe_salmon_card", "step_salmon_plate" -> R.drawable.editorial_salmon
        "recipe_beef_stew" -> R.drawable.editorial_beef
        "recipe_karaage" -> R.drawable.editorial_karaage
        "recipe_avocado_toast" -> R.drawable.editorial_toast
        "recipe_mushroom_soup" -> R.drawable.editorial_soup
        "recipe_lamb_chops" -> R.drawable.editorial_lamb
        "recipe_burgundy" -> R.drawable.editorial_burgundy
        "recipe_truffle_pasta" -> R.drawable.editorial_pasta
        "recipe_brisket_pot" -> R.drawable.editorial_brisket
        "recipe_duck_confit" -> R.drawable.editorial_duck
        "recipe_french_toast" -> R.drawable.editorial_french_toast
        "recipe_spring_soup" -> R.drawable.editorial_spring
        "recipe_asparagus_salad" -> R.drawable.editorial_salad
        "step_salmon_marinate" -> R.drawable.editorial_marinate
        else -> null
    }
    fun isLocalUri(ref: String?): Boolean = ref != null && (ref.startsWith("content://") || ref.startsWith("file://"))
}
