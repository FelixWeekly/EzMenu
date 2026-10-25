package com.ezmenu.app.ui.screens.editor

import com.ezmenu.app.domain.model.RecipeJson
import org.json.*

object EditorDraftCodec {
    fun encode(s: EditorUiState): String = JSONObject().apply {
        put("title",s.title);put("story",s.story);put("cover",s.coverRef?:JSONObject.NULL);put("minutes",s.cookMinutes);put("servings",s.servings)
        put("category",s.category);put("tags",JSONArray(s.tags));put("groups",JSONArray(s.groups.map{RecipeJson.encode(it)}))
        put("ingredients",JSONArray(s.ingredients.map{r->JSONObject().apply{
            put("uid",r.uid);put("name",r.name);put("amount",r.amountText);put("unit",r.unit);put("raw",r.isRawAmount);put("group",r.groupId)
            put("original",r.original?.let{RecipeJson.encode(it)}?:JSONObject.NULL)
        }}))
        put("steps",JSONArray(s.steps.map{r->JSONObject().apply{
            put("uid",r.uid);put("title",r.title);put("body",r.body);put("timer",r.timerSeconds?:JSONObject.NULL);put("note",r.noteText);put("image",r.imageRef?:JSONObject.NULL)
            put("original",r.original?.let{RecipeJson.encode(it)}?:JSONObject.NULL)
        }}))
    }.toString()
    fun decode(value: String,base: EditorUiState): EditorUiState {
        val o=JSONObject(value)
        val ingredients=o.getJSONArray("ingredients");val steps=o.getJSONArray("steps");val groups=o.getJSONArray("groups");val tags=o.getJSONArray("tags")
        return base.copy(title=o.getString("title"),story=o.getString("story"),coverRef=if(o.isNull("cover"))null else o.getString("cover"),
            cookMinutes=o.getInt("minutes"),servings=o.getInt("servings"),category=o.getString("category"),tags=(0 until tags.length()).map{tags.getString(it)},
            groups=(0 until groups.length()).map{RecipeJson.decodeIngredientGroupEntity(groups.getJSONObject(it))},
            ingredients=(0 until ingredients.length()).map{val r=ingredients.getJSONObject(it);EditableIngredient(r.getLong("uid"),r.getString("name"),r.getString("amount"),r.getString("unit"),r.getBoolean("raw"),r.getLong("group"),if(r.isNull("original"))null else RecipeJson.decodeIngredientEntity(r.getJSONObject("original")))},
            steps=(0 until steps.length()).map{val r=steps.getJSONObject(it);EditableStep(r.getLong("uid"),r.getString("title"),r.getString("body"),if(r.isNull("timer"))null else r.getInt("timer"),r.getString("note"),if(r.isNull("image"))null else r.getString("image"),if(r.isNull("original"))null else RecipeJson.decodeStepEntity(r.getJSONObject("original")))})
    }
}
