package com.ezmenu.app.ui.studio

import android.Manifest
import android.content.Intent
import androidx.core.net.toUri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.ezmenu.app.ui.icons.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ezmenu.app.data.local.entity.CookingSession
import com.ezmenu.app.data.repository.*
import com.ezmenu.app.domain.*
import com.ezmenu.app.domain.model.RecipeSnapshot
import com.ezmenu.app.ui.components.EzPhoto
import com.ezmenu.app.util.CookingAlarms
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

@Composable fun SnapshotContent(snapshot: RecipeSnapshot) {
    val repo=LocalStudio.current.recipeRepository
    val detail=remember(snapshot){repo.detailFromGraph(snapshot.graph,snapshot.servings)}
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Paper(Modifier.fillMaxWidth()){
            detail.groups.forEach{g->
                Text(g.name,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                g.items.forEach{i->Row(Modifier.fillMaxWidth().padding(vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)){
                    Text(i.name,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium);Text(i.amountLabel,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }}
            }
        }
        detail.steps.forEachIndexed{index,step->Paper(Modifier.fillMaxWidth()){
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                Text((index+1).toString().padStart(2,'0'),style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.primary)
                Text(step.title,style=MaterialTheme.typography.titleMedium)
            }
            Text(step.body,style=MaterialTheme.typography.bodyMedium)
            if(step.imageRef!=null)EzPhoto(step.imageRef,step.title,Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(12.dp)))
            step.noteText?.takeIf{it.isNotBlank()}?.let{Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            step.timerSeconds?.let{Text("计时 "+it/60+" 分 "+it%60+" 秒",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)}
        }}
    }
}

@Composable fun RecipeChapter(id: Long,onBack: ()->Unit,onEdit: (Long?)->Unit,onCook: (Long,Int)->Unit,onEntry: (Long)->Unit,onShare: (Long)->Unit) {
    val wide=LocalWindowInfo.current.containerSize.width / LocalDensity.current.density >= 840f
    val s=LocalStudio.current;val scope=rememberCoroutineScope { kotlinx.coroutines.Dispatchers.Main.immediate }
    val graph by remember(id){s.database.recipeDao().observeById(id)}.collectAsStateWithLifecycle(null)
    val latest by remember(id){s.journalRepository.latest(id)}.collectAsStateWithLifecycle(null)
    var n by rememberSaveable(id){mutableIntStateOf(0)};var checked by rememberSaveable(id){mutableStateOf(listOf<Long>())}
    var message by remember{mutableStateOf<String?>(null)};var deleting by remember{mutableStateOf(false)}
    LaunchedEffect(id){if(n==0)n=s.settingsRepository.defaultServings.first()}
    val g=graph
    if(g==null){Column{StudioTopBar("食谱详情",onBack);Box(Modifier.padding(StudioMargin)){EmptyChapter("正在加载食谱")}};return}
    val r=g.recipe;val built=r.origin=="BUILTIN"
    val detail=remember(g,n){s.recipeRepository.detailFromGraph(g,n.coerceAtLeast(1))}
    suspend fun personal(): Long = s.database.recipeDao().collectBuiltin(id)
    fun cooking(step: Int=0,timer: Boolean=false){scope.launch{try{
        val target=personal();val session=s.cookingSessions.start(target,n,step)
        s.cookingSessions.checked(session.id,checked.toSet())
        if(timer)s.cookingSessions.changeTimer(session.id,step,"toggle")
        onCook(target,n)
    }catch(e:ExistingCookingSession){onCook(e.session.recipeId,e.session.servings)}catch(e:Exception){message=e.message?:"暂时无法开始"}}}
    val intro: @Composable ()->Unit = {
        Column(Modifier.padding(horizontal=StudioMargin,vertical=20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            EditorialHeading(r.title,subtitle=r.story.ifBlank{r.subtitle})
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.35f))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){Text("耗时",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(r.cookTimeMinutes.toString()+" 分钟",style=MaterialTheme.typography.titleMedium)}
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){Text("难度",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(r.difficulty.ifBlank{"—"},style=MaterialTheme.typography.titleMedium)}
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){Text("分类",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(r.category.takeUnless{it=="全部"}?:"未分类",style=MaterialTheme.typography.titleMedium)}
            }
            Button({cooking()},Modifier.fillMaxWidth().heightIn(min=52.dp),shape=RoundedCornerShape(14.dp),enabled=detail.steps.isNotEmpty()){
                MsIcon(MsGlyph.PlayCircle);Spacer(Modifier.width(8.dp));Text(if(detail.steps.isEmpty())"暂无烹饪步骤" else "开始烹饪")
            }
            if(built)OutlinedButton({scope.launch{val copy=personal();onEdit(copy)}},Modifier.fillMaxWidth().heightIn(min=48.dp),shape=RoundedCornerShape(14.dp)){MsIcon(MsGlyph.Add);Spacer(Modifier.width(8.dp));Text("收录食谱")}
            latest?.let{log->Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surfaceContainerLow){
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("上次下厨",Modifier.weight(1f),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary);Text(dateLabel(log.cookedAt),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                    if(log.note.isNotBlank())Text(log.note,style=MaterialTheme.typography.bodyMedium)
                    if(log.nextTime.isNotBlank())Text("下次调整 · "+log.nextTime,style=MaterialTheme.typography.bodyMedium)
                    TextButton({onEntry(log.id)},contentPadding=PaddingValues(0.dp)){Text("查看手记");Spacer(Modifier.width(6.dp));MsIcon(MsGlyph.ArrowForward,size=16.dp)}
                }
            }}
            message?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        }
    }
    Column {
        StudioTopBar(if(built)"内置食谱" else "食谱详情",onBack,actions={
            if(!built)FavoriteAction(r.isFavorite){scope.launch{s.recipeRepository.setFavorite(id,!r.isFavorite)}}
            StudioAction(MsGlyph.HistoryEdu,"编辑",{scope.launch{onEdit(personal())}})
            StudioAction(MsGlyph.Share,"分享",{onShare(id)})
        })
        LazyColumn(Modifier.testTag("recipe-content"),contentPadding=PaddingValues(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            item { if(wide) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                Box(Modifier.weight(1f).padding(start=StudioMargin,top=20.dp)){EzPhoto(r.coverImageRef,r.title,Modifier.fillMaxWidth().aspectRatio(1.05f).coverTransition(id).clip(RoundedCornerShape(18.dp)),placeholderLabel=r.title)}
                Box(Modifier.weight(1f)){intro()}
            } else Column { EzPhoto(r.coverImageRef,r.title,Modifier.fillMaxWidth().aspectRatio(1.6f).coverTransition(id),placeholderLabel=r.title);intro() } }
            item{Column(Modifier.padding(horizontal=StudioMargin),verticalArrangement=Arrangement.spacedBy(12.dp)){
                SectionTitle("食材准备",action={ServingsControl(n.coerceAtLeast(1),{n=it})})
                Paper(Modifier.fillMaxWidth()){
                    detail.groups.forEachIndexed{groupIndex,group->
                        if(groupIndex>0)HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.3f))
                        Text(group.name,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                        group.items.forEach{ingredient->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                            Checkbox(ingredient.id in checked,{checked=if(ingredient.id in checked)checked-ingredient.id else checked+ingredient.id})
                            Text(ingredient.name,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
                            Text(ingredient.amountLabel,Modifier.padding(start=10.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }}
                    }
                }
                SectionTitle("烹饪步骤")
                detail.steps.forEachIndexed{index,step->Paper(Modifier.fillMaxWidth()){
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                        Text((index+1).toString().padStart(2,'0'),style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.primary)
                        Text(step.title,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                    }
                    Text(step.body,style=MaterialTheme.typography.bodyMedium)
                    if(step.imageRef!=null)EzPhoto(step.imageRef,step.title,Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(12.dp)))
                    step.noteText?.takeIf{it.isNotBlank()}?.let{Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                    if(step.timerSeconds!=null)FilledTonalButton({cooking(index,true)},shape=RoundedCornerShape(10.dp)){MsIcon(MsGlyph.Timer,size=18.dp);Spacer(Modifier.width(6.dp));Text("计时 · "+step.timerSeconds/60+" 分钟")}
                }}
                if(!built)TextButton({deleting=true}){Text("移入回收站",color=MaterialTheme.colorScheme.error)}
            }}
        }
    }
    if(deleting)AlertDialog(onDismissRequest={deleting=false},title={Text("移入回收站？")},text={Text("食谱可以恢复，已有料理手记会保留。")},confirmButton={TextButton({scope.launch{s.database.recipeDao().trash(id,System.currentTimeMillis());onBack()}}){Text("移入回收站")}},dismissButton={TextButton({deleting=false}){Text("取消")}})
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun CookingChapter(recipeId: Long,servings: Int,onBack: ()->Unit,onComplete: (Long)->Unit) {
    val s=LocalStudio.current;val scope=rememberCoroutineScope { kotlinx.coroutines.Dispatchers.Main.immediate };val context=LocalContext.current;val view=LocalView.current
    var session by remember{mutableStateOf<CookingSession?>(null)};var error by remember{mutableStateOf<String?>(null)}
    var completing by remember{mutableStateOf(false)};var tick by remember{mutableLongStateOf(0)}
    val keep by s.settingsRepository.keepScreenOn.collectAsStateWithLifecycle(true)
    DisposableEffect(keep){val old=view.keepScreenOn;view.keepScreenOn=keep;onDispose{view.keepScreenOn=old}}
    LaunchedEffect(recipeId){try{session=s.cookingSessions.start(recipeId,servings)}catch(e:ExistingCookingSession){session=e.session;error="已恢复之前正在做的菜"}catch(e:Exception){error=e.message}}
    val a=session
    val observed by remember(a?.id){a?.let{s.cookingSessions.observe(it.id)}?:kotlinx.coroutines.flow.flowOf(null)}.collectAsStateWithLifecycle(null)
    val active=observed?:a
    val timers by remember(a?.id){a?.let{s.cookingSessions.timers(it.id)}?:kotlinx.coroutines.flow.flowOf(emptyList())}.collectAsStateWithLifecycle(emptyList())
    LaunchedEffect(a?.id){if(a!=null)while(isActive){tick++;s.cookingSessions.finishDueTimers();delay(250)}}
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){ }
    Column {
        StudioTopBar("烹饪",onBack)
        if(active==null){Box(Modifier.padding(StudioMargin)){EmptyChapter(error?:"正在准备下厨","你可以返回食谱重新开始。")};return@Column}
        val snapshot=remember(active.snapshot){RecipeSnapshot.decode(active.snapshot)}
        val steps=snapshot.graph.orderedSteps;val index=active.stepIndex.coerceIn(0,steps.lastIndex);val step=steps[index]
        val timer=timers.firstOrNull{it.stepIndex==index};val remaining=remember(timer,tick){timer?.let{s.cookingSessions.remaining(it)}}
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(StudioMargin),verticalArrangement=Arrangement.spacedBy(20.dp)){
            item{Text(snapshot.graph.recipe.title,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)}
            item{LinearProgressIndicator(progress={(index+1).toFloat()/steps.size},modifier=Modifier.fillMaxWidth())}
            item{AnimatedContent(index,transitionSpec={
                val direction=if(targetState>initialState)1 else -1
                (fadeIn(tween(220))+slideInHorizontally(tween(260,easing=EditorialEasing)){it/12*direction}) togetherWith (fadeOut(tween(100))+slideOutHorizontally(tween(180)){ -it/16*direction })
            },label="cooking-step"){visibleIndex->
                val visibleStep=steps[visibleIndex]
                Column(verticalArrangement=Arrangement.spacedBy(18.dp)){
                    EditorialHeading(visibleStep.title,subtitle="第 "+(visibleIndex+1)+" / "+steps.size+" 步")
                    Paper(Modifier.fillMaxWidth()){Text(visibleStep.body,style=MaterialTheme.typography.bodyLarge);visibleStep.noteText?.let{Text(it,color=MaterialTheme.colorScheme.onSurfaceVariant)};if(visibleStep.imageRef!=null)EzPhoto(visibleStep.imageRef,visibleStep.title,Modifier.fillMaxWidth().aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)))}
                }
            }}
            if(active.status=="INTERRUPTED")item{EmptyChapter("上次烹饪被设备重启中断","计时已暂停。确认食物状态后，再恢复计时。","恢复烹饪",{scope.launch{s.cookingSessions.confirmResume(active.id)}})}
            if(timer!=null)item{Paper(Modifier.fillMaxWidth()){
                val seconds=((remaining?:0)+999)/1000
                Text((seconds/60).toString().padStart(2,'0')+":"+(seconds%60).toString().padStart(2,'0'),modifier=Modifier.align(Alignment.CenterHorizontally),style=com.ezmenu.app.ui.theme.EzText.timerDisplay,color=MaterialTheme.colorScheme.primary)
                FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Button({scope.launch{s.cookingSessions.changeTimer(active.id,index,"toggle")}},enabled=active.status=="ACTIVE"){Text(if(timer.state=="RUNNING")"暂停" else "开始")}
                    OutlinedButton({scope.launch{s.cookingSessions.changeTimer(active.id,index,"add")}}){Text("＋30秒")}
                    TextButton({scope.launch{s.cookingSessions.changeTimer(active.id,index,"reset")}}){Text("重置")}
                }
            }}
            timers.filter{it.stepIndex!=index&&it.state=="RUNNING"}.forEach{t->item{Text("第"+(t.stepIndex+1)+"步仍在计时 · "+(s.cookingSessions.remaining(t)/1000)+"秒",color=MaterialTheme.colorScheme.primary)}}
            if(!CookingAlarms(context).enabled())item{Paper{
                Text("后台提醒未开启",style=MaterialTheme.typography.titleMedium)
                Text("仅在应用内提示计时结束。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton({if(Build.VERSION.SDK_INT>=33)launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    if(Build.VERSION.SDK_INT>=31)context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,("package:"+context.packageName).toUri()))}){Text("开启后台提醒")}
            }}
            error?.let{item{Text(it,color=MaterialTheme.colorScheme.error)}}
            item{TextButton({scope.launch{s.cookingSessions.abandon(active.id);onBack()}}){Text("结束烹饪")}}
        }
        Row(Modifier.fillMaxWidth().padding(StudioMargin),horizontalArrangement=Arrangement.spacedBy(12.dp)){
            OutlinedButton({scope.launch{s.cookingSessions.step(active.id,index-1)}},Modifier.weight(1f),enabled=index>0&&!completing){Text("上一步")}
            Button({scope.launch{if(index==steps.lastIndex){if(!completing){completing=true;try{onComplete(s.cookingSessions.complete(active.id))}catch(e:Exception){error="记录保存失败，请重试";completing=false}}}else s.cookingSessions.step(active.id,index+1)}},Modifier.weight(1f),enabled=!completing&&active.status=="ACTIVE"){Text(if(index==steps.lastIndex)"完成烹饪" else "下一步")}
        }
    }
}
