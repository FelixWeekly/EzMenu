package com.ezmenu.app.ui.studio

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.ezmenu.app.ui.icons.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ezmenu.app.data.repository.JournalRepository
import com.ezmenu.app.ui.components.EzPhoto
import com.ezmenu.app.util.ImageStorage
import kotlinx.coroutines.launch

@Composable fun JournalChapter(onOpen: (Long)->Unit) {
    val s=LocalStudio.current;var limit by rememberSaveable{mutableIntStateOf(60)}
    var recipeId by rememberSaveable{mutableStateOf<Long?>(null)}
    val rows by remember(recipeId,limit){s.journalRepository.timeline(recipeId,limit)}.collectAsStateWithLifecycle(emptyList())
    val recipes by remember{s.recipeRepository.browse(limit=10000)}.collectAsStateWithLifecycle(emptyList())
    LazyColumn(Modifier.testTag("journal-list"),contentPadding=PaddingValues(StudioMargin),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item{ChapterHeading("手记")}
        item{var expanded by remember{mutableStateOf(false)};Box{TextButton({expanded=true}){Text(recipes.firstOrNull{it.recipe.id==recipeId}?.recipe?.title?:"所有食谱")};DropdownMenu(expanded,{expanded=false}){
            DropdownMenuItem({Text("所有食谱")},{recipeId=null;expanded=false});recipes.forEach{r->DropdownMenuItem({Text(r.recipe.title)},{recipeId=r.recipe.id;expanded=false})}
        }}}
        if(rows.isEmpty())item{EmptyChapter("暂无手记")}
        rows.groupBy{dateLabel(it.cookedAt,"yyyy年M月")}.forEach{(month,entries)->
            item(key=month){SectionTitle(month)}
            items(entries,key={it.id}){log->Paper(Modifier.fillMaxWidth().clickable{onOpen(log.id)}){
                val photo=JournalRepository.photos(log.photosJson).firstOrNull()?:log.coverRef
                EzPhoto(photo,log.recipeTitle,Modifier.fillMaxWidth().aspectRatio(1.8f).clip(RoundedCornerShape(12.dp)),placeholderLabel=log.recipeTitle)
                Text(log.recipeTitle.ifBlank{"旧版料理记录"},style=MaterialTheme.typography.headlineMedium)
                Text(dateLabel(log.cookedAt)+" · "+log.servings+" 人份"+if(log.source=="LEGACY")" · 旧版记录" else "",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                if(log.note.isNotBlank())Text(log.note,style=MaterialTheme.typography.bodyMedium,maxLines=3,overflow=TextOverflow.Ellipsis)
                if(log.nextTime.isNotBlank())Text("下次调整 · "+log.nextTime,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }}
        }
        if(rows.size>=limit)item{TextButton({limit+=60}){Text("更早的日子")}}
    }
}

@Composable fun JournalEntryChapter(id: Long,onBack: ()->Unit,onShare: (Long)->Unit) {
    val feedback=LocalStudioFeedback.current
    val s=LocalStudio.current;val context=LocalContext.current;val scope=rememberCoroutineScope { kotlinx.coroutines.Dispatchers.Main.immediate }
    val entry by remember(id){s.journalRepository.entry(id)}.collectAsStateWithLifecycle(null)
    var note by rememberSaveable(id){mutableStateOf("")};var next by rememberSaveable(id){mutableStateOf("")}
    var photos by rememberSaveable(id){mutableStateOf(listOf<String>())};var loaded by rememberSaveable(id){mutableStateOf(false)}
    var busy by remember{mutableStateOf(false)};var message by remember{mutableStateOf<String?>(null)}
    LaunchedEffect(entry?.id){entry?.let{if(!loaded){note=it.note;next=it.nextTime;photos=JournalRepository.photos(it.photosJson);loaded=true}}}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->if(uri!=null)scope.launch{busy=true;try{val ref=ImageStorage.copyIntoAppStorage(context,uri);if(ref!=null)photos=photos+ref else message="照片导入失败"}catch(e:Exception){message="照片导入失败，请重试"}finally{busy=false}}}
    Column {
        StudioTopBar("料理手记",onBack,actions={StudioAction(MsGlyph.Share,"分享",{onShare(id)},enabled=entry!=null)})
        val log=entry
        if(log==null){Box(Modifier.padding(StudioMargin)){EmptyChapter("暂时没有这篇手记","返回手记列表后重试。")};return@Column}
        LazyColumn(contentPadding=PaddingValues(StudioMargin),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            item{EditorialHeading(log.recipeTitle,subtitle=dateLabel(log.cookedAt,"yyyy年M月d日 HH:mm")+" · "+log.servings+" 人份")}
            item{SectionTitle("成品照片",action={Text(photos.size.toString()+" / 3",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)})}
            items(photos){ref->Column{EzPhoto(ref,"成品照片",Modifier.fillMaxWidth().aspectRatio(1.4f));TextButton({photos=photos-ref}){Text("移除照片")}}}
            if(photos.size<3)item{OutlinedButton({picker.launch("image/*")},enabled=!busy){Text(if(busy)"正在导入…" else "添加照片")}}
            item{OutlinedTextField(note,{note=it},Modifier.fillMaxWidth(),label={Text("心得")},minLines=3)}
            item{OutlinedTextField(next,{next=it},Modifier.fillMaxWidth(),label={Text("下次调整")},minLines=2)}
            message?.let{item{Text(it,color=MaterialTheme.colorScheme.primary)}}
            item{Button(onClick={scope.launch{busy=true;try{s.journalRepository.saveReflection(id,note,next,photos);message=null;feedback("手记已保存")}catch(e:Exception){message="保存失败，输入仍然保留"}finally{busy=false}}},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("保存手记")}}
            item{SectionTitle("当时的配方")}
            val snap=JournalRepository.snapshot(log.snapshot)
            if(snap==null)item{EmptyChapter("旧版未保存当时配方")}
            else item{SnapshotContent(snap)}
        }
    }
}
