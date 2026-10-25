package com.ezmenu.app.ui.studio

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ezmenu.app.di.ServiceLocator
import com.ezmenu.app.ui.components.EzPhoto
import com.ezmenu.app.ui.icons.*
import com.ezmenu.app.ui.theme.NewsreaderFamily
import com.ezmenu.app.data.local.relation.RecipeListing
import java.text.SimpleDateFormat
import java.util.*

val LocalStudio = staticCompositionLocalOf<ServiceLocator> { error("Studio services unavailable") }
val StudioMargin = 20.dp

@Composable fun EditorialHeading(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier, verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(title, style=MaterialTheme.typography.headlineLarge.copy(fontSize=27.sp,lineHeight=38.sp), fontFamily=NewsreaderFamily)
        if(!subtitle.isNullOrBlank()) Text(subtitle,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun ChapterHeading(title: String, actions: @Composable RowScope.()->Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(top=6.dp,bottom=4.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(title,Modifier.weight(1f),style=MaterialTheme.typography.headlineLarge.copy(fontSize=25.sp,lineHeight=35.sp))
        actions()
    }
}
@Composable fun StudioAction(glyph: MsGlyph,label: String,onClick: ()->Unit,enabled: Boolean=true) {
    IconButton(onClick,enabled=enabled) { MsIcon(glyph,contentDescription=label,tint=if(enabled)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha=.38f)) }
}
@Composable fun SearchBox(value: String,onValue: (String)->Unit,placeholder: String,modifier: Modifier=Modifier) {
    TextField(value,onValue,modifier.fillMaxWidth().border(.75.dp,MaterialTheme.colorScheme.outlineVariant.copy(alpha=.48f),RoundedCornerShape(14.dp)),placeholder={Text(placeholder,style=MaterialTheme.typography.bodyMedium,maxLines=1,overflow=TextOverflow.Ellipsis)},
        leadingIcon={MsIcon(MsGlyph.Search,tint=MaterialTheme.colorScheme.onSurfaceVariant)},
        trailingIcon=if(value.isNotEmpty()){{StudioAction(MsGlyph.Close,"清除搜索",{onValue("")})}}else null,
        singleLine=true,shape=RoundedCornerShape(14.dp),textStyle=MaterialTheme.typography.bodyMedium,
        colors=TextFieldDefaults.colors(focusedContainerColor=MaterialTheme.colorScheme.surfaceContainerLow,unfocusedContainerColor=MaterialTheme.colorScheme.surfaceContainerLow,
            focusedIndicatorColor=Color.Transparent,unfocusedIndicatorColor=Color.Transparent))
}
@Composable fun ChoicePill(label: String,selected: Boolean,onClick: ()->Unit) {
    Surface(onClick,shape=RoundedCornerShape(50),color=animateColorAsState(if(selected)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,tween(180),label="filter").value,
        border=BorderStroke(.75.dp,if(selected)MaterialTheme.colorScheme.primary.copy(alpha=.22f)else MaterialTheme.colorScheme.outlineVariant.copy(alpha=.3f)),
        contentColor=if(selected)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) {
        Box(Modifier.defaultMinSize(minHeight=44.dp).padding(horizontal=16.dp,vertical=10.dp),contentAlignment=Alignment.Center){Text(label,style=MaterialTheme.typography.titleSmall)}
    }
}
@Composable fun SectionTitle(title: String, modifier: Modifier = Modifier, action: (@Composable ()->Unit)? = null) {
    Row(modifier.fillMaxWidth().heightIn(min=44.dp),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(3.dp,16.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        Text(title,Modifier.weight(1f).padding(start=9.dp),style=MaterialTheme.typography.headlineSmall)
        action?.invoke()
    }
}
@Composable fun Paper(modifier: Modifier = Modifier, content: @Composable ColumnScope.()->Unit) {
    Surface(modifier,shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surfaceContainerLowest,
        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant.copy(alpha=.28f))) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
    }
}
@Composable fun EmptyChapter(title: String, body: String="", action: String? = null, onAction: ()->Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=36.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Surface(shape=CircleShape,color=MaterialTheme.colorScheme.surfaceContainerLow){Box(Modifier.size(60.dp),contentAlignment=Alignment.Center){MsIcon(MsGlyph.MenuBook,tint=MaterialTheme.colorScheme.primary)}}
        Text(title,style=MaterialTheme.typography.headlineSmall)
        if(body.isNotBlank())Text(body,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(action!=null) FilledTonalButton(onClick=onAction,shape=RoundedCornerShape(12.dp)) { Text(action) }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun StudioTopBar(title: String, onBack: ()->Unit, actions: @Composable RowScope.()->Unit = {}) {
    TopAppBar(title={Text(title,style=MaterialTheme.typography.titleLarge)},navigationIcon={StudioAction(MsGlyph.ArrowBack,"返回",onBack)},actions=actions)
}
@Composable fun RecipeTile(item: RecipeListing, modifier: Modifier = Modifier, onClick: ()->Unit) {
    val r=item.recipe
    val interactions=remember{MutableInteractionSource()}
    Surface(onClick,modifier.pressFeedback(interactions),shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surfaceContainerLowest,
        border=BorderStroke(.75.dp,MaterialTheme.colorScheme.outlineVariant.copy(alpha=.35f)),shadowElevation=1.dp,interactionSource=interactions) {
        Column {
            Box {
                EzPhoto(r.coverImageRef,r.title,Modifier.fillMaxWidth().aspectRatio(1.17f).coverTransition(r.id),placeholderLabel=r.title)
                if(r.cookTimeMinutes>0)Surface(Modifier.align(Alignment.BottomStart).padding(8.dp),shape=RoundedCornerShape(8.dp),color=MaterialTheme.colorScheme.surface.copy(alpha=.93f)){
                    Row(Modifier.padding(horizontal=7.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(3.dp)){
                        MsIcon(MsGlyph.Schedule,size=12.dp,tint=MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(r.cookTimeMinutes.toString()+" 分钟",style=MaterialTheme.typography.labelSmall)
                    }
                }
                if(r.isFavorite)Surface(Modifier.align(Alignment.TopEnd).padding(8.dp),shape=CircleShape,color=MaterialTheme.colorScheme.surface.copy(alpha=.9f)){
                    Box(Modifier.size(28.dp),contentAlignment=Alignment.Center){MsIcon(MsGlyph.Favorite,size=16.dp,tint=MaterialTheme.colorScheme.primary)}
                }
            }
            Column(Modifier.padding(horizontal=11.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                Text(r.title,style=MaterialTheme.typography.headlineSmall.copy(fontSize=16.sp,lineHeight=23.sp),maxLines=2,minLines=2,overflow=TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    val category=r.category.takeUnless{it=="全部"}.orEmpty()
                    val tint=if(category.contains("轻卡")||category.contains("素"))MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    if(category.isNotBlank())Box(Modifier.padding(end=5.dp).size(4.dp).clip(CircleShape).background(tint.copy(alpha=.6f)))
                    Text(category,Modifier.weight(1f),style=MaterialTheme.typography.labelSmall,color=tint,maxLines=1,overflow=TextOverflow.Ellipsis)
                    if(r.isDraft)Text("草稿",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
@Composable fun ServingsControl(servings: Int,onChange: (Int)->Unit,modifier: Modifier=Modifier) {
    Surface(modifier,shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceContainerLow){
        Row(verticalAlignment=Alignment.CenterVertically){
            IconButton({onChange((servings-1).coerceAtLeast(1))},enabled=servings>1){MsIcon(MsGlyph.Remove,size=18.dp,contentDescription="减少份量")}
            Text(servings.toString()+" 人份",style=MaterialTheme.typography.labelLarge)
            IconButton({onChange((servings+1).coerceAtMost(10))},enabled=servings<10){MsIcon(MsGlyph.Add,size=18.dp,contentDescription="增加份量")}
        }
    }
}
fun dateLabel(time: Long, pattern: String="M月d日")=SimpleDateFormat(pattern,Locale.CHINA).format(Date(time))
