package com.ezmenu.app.ui.studio

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import com.ezmenu.app.ui.icons.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.ezmenu.app.data.repository.ArchiveRepository
import com.ezmenu.app.domain.model.RecipeSnapshot
import com.ezmenu.app.util.RecipeExport
import kotlinx.coroutines.*
import java.io.File

@Composable fun ProfileChapter(onSettings: ()->Unit,onArchive: ()->Unit,onTrash: ()->Unit,onDrafts: ()->Unit,onAbout: ()->Unit) {
    val s=LocalStudio.current
    val nickname by s.settingsRepository.nickname.collectAsStateWithLifecycle("我的厨房")
    val count by remember{s.database.journalDao().personalCount()}.collectAsStateWithLifecycle(0)
    val legacy by remember{s.database.journalDao().legacyCount()}.collectAsStateWithLifecycle(0)
    val recipes by remember{s.recipeRepository.observePublishedCount()}.collectAsStateWithLifecycle(0)
    LazyColumn(Modifier.testTag("profile-list"),contentPadding=PaddingValues(StudioMargin),verticalArrangement=Arrangement.spacedBy(24.dp)){
        item{ChapterHeading("我的"){StudioAction(MsGlyph.Tune,"设置",onSettings)}}
        item{Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)){
            Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.primaryContainer){Box(Modifier.size(68.dp),contentAlignment=Alignment.Center){MsIcon(MsGlyph.Kitchen,size=32.dp,tint=MaterialTheme.colorScheme.primary)}}
            Text(nickname,style=MaterialTheme.typography.headlineMedium)
        }}
        item{Surface(shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surfaceContainerLowest){
            Row(Modifier.fillMaxWidth().padding(vertical=22.dp)){
                ProfileStat(recipes,"食谱",Modifier.weight(1f))
                VerticalDivider(Modifier.height(48.dp),color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.35f))
                ProfileStat(count,"下厨",Modifier.weight(1f))
                if(legacy>0){VerticalDivider(Modifier.height(48.dp),color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.35f));ProfileStat(legacy,"旧版记录",Modifier.weight(1f))}
            }
        }}
        item{Surface(shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surfaceContainerLowest){Column{
            ProfileRow(MsGlyph.Drafts,"草稿箱",onDrafts)
            HorizontalDivider(Modifier.padding(start=64.dp),color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.25f))
            ProfileRow(MsGlyph.Delete,"回收站",onTrash)
            HorizontalDivider(Modifier.padding(start=64.dp),color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.25f))
            ProfileRow(MsGlyph.DownloadForOffline,"备份与恢复",onArchive)
        }}}
        item{Surface(shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surfaceContainerLowest){Column{
            ProfileRow(MsGlyph.LightMode,"厨房与外观设置",onSettings)
            HorizontalDivider(Modifier.padding(start=64.dp),color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.25f))
            ProfileRow(MsGlyph.Info,"关于旬食",onAbout)
        }}}
        item{Box(Modifier.fillMaxWidth().padding(vertical=10.dp),contentAlignment=Alignment.Center){Text("旬食 · EzMenu",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=.6f))}}
    }
}
@Composable private fun ProfileStat(value: Int,label: String,modifier: Modifier=Modifier){
    Column(modifier,horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)){
        Text(value.toString(),style=MaterialTheme.typography.displaySmall.copy(fontSize=28.sp),color=MaterialTheme.colorScheme.primary)
        Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable private fun ProfileRow(icon: MsGlyph,title: String,onClick: ()->Unit){
    Surface(onClick,color=MaterialTheme.colorScheme.surfaceContainerLowest){Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){
        Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.surfaceContainerLow){Box(Modifier.size(36.dp),contentAlignment=Alignment.Center){MsIcon(icon,tint=MaterialTheme.colorScheme.primary)}}
        Text(title,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
        MsIcon(MsGlyph.ChevronRight,tint=MaterialTheme.colorScheme.onSurfaceVariant)
    }}
}
@Composable fun SettingsChapter(onBack: ()->Unit) {
    val feedback=LocalStudioFeedback.current
    val s=LocalStudio.current;val scope=rememberCoroutineScope { kotlinx.coroutines.Dispatchers.Main.immediate }
    val mode by s.settingsRepository.theme.collectAsStateWithLifecycle("system")
    val savedName by s.settingsRepository.nickname.collectAsStateWithLifecycle("我的厨房")
    val n by s.settingsRepository.defaultServings.collectAsStateWithLifecycle(2)
    val keep by s.settingsRepository.keepScreenOn.collectAsStateWithLifecycle(true)
    var name by rememberSaveable{mutableStateOf("")};LaunchedEffect(savedName){name=savedName}
    Column{StudioTopBar("厨房与外观",onBack);LazyColumn(contentPadding=PaddingValues(StudioMargin),verticalArrangement=Arrangement.spacedBy(20.dp)){
        item{Paper{
            Text("厨房昵称",style=MaterialTheme.typography.headlineSmall)
            OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("厨房昵称")},singleLine=true)
            Button({scope.launch{s.settingsRepository.setNickname(name);feedback("昵称已保存")}}){Text("保存昵称")}
        }}
        item{Paper{
            Text("外观",style=MaterialTheme.typography.headlineSmall)
            listOf("system" to "跟随系统","light" to "浅色","dark" to "深色").forEach{(key,label)->Row(Modifier.fillMaxWidth()){
                RadioButton(mode==key,{scope.launch{s.settingsRepository.setTheme(key)}});Text(label,Modifier.padding(top=13.dp))
            }}
        }}
        item{Paper{
            Text("默认份量",style=MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){OutlinedButton({scope.launch{s.settingsRepository.setDefaultServings((n-1).coerceAtLeast(1))}},enabled=n>1){Text("−")};Text(n.toString()+" 人份",Modifier.padding(top=12.dp));OutlinedButton({scope.launch{s.settingsRepository.setDefaultServings((n+1).coerceAtMost(10))}},enabled=n<10){Text("＋")}}
        }}
        item{Paper{Row(Modifier.fillMaxWidth()){Text("下厨时屏幕常亮",Modifier.weight(1f).padding(top=14.dp));Switch(keep,{scope.launch{s.settingsRepository.setKeepScreenOn(it)}})}}}
    }}
}

@Composable fun ArchiveChapter(onBack: ()->Unit) {
    val s=LocalStudio.current;val context=LocalContext.current;val scope=rememberCoroutineScope { kotlinx.coroutines.Dispatchers.Main.immediate }
    var busy by remember{mutableStateOf(false)};var message by remember{mutableStateOf<String?>(null)}
    var importFile by remember{mutableStateOf<File?>(null)};var inspection by remember{mutableStateOf<ArchiveRepository.Inspection?>(null)}
    var exported by remember{mutableStateOf<File?>(null)}
    val save=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")){uri->if(uri!=null)scope.launch{busy=true;try{withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri).use{out->requireNotNull(out);exported!!.inputStream().use{it.copyTo(out)}}};message="备份已导出"}catch(e:Exception){message="导出失败，请重试"}finally{busy=false}}}
    val pick=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->if(uri!=null)scope.launch{busy=true;try{val file=File(context.cacheDir,"incoming.ezmenu");withContext(Dispatchers.IO){context.contentResolver.openInputStream(uri).use{input->requireNotNull(input);file.outputStream().use{input.copyTo(it)}}};inspection=s.archiveRepository.inspect(file);importFile=file}catch(e:Exception){message=e.message?:"备份无法读取"}finally{busy=false}}}
    Column{StudioTopBar("备份与恢复",onBack);LazyColumn(contentPadding=PaddingValues(StudioMargin),verticalArrangement=Arrangement.spacedBy(20.dp)){
        item{Paper{
            Text("导出完整备份",style=MaterialTheme.typography.headlineSmall)
            Text("包含食谱、手记、草稿、照片与设置。")
            Button({scope.launch{busy=true;try{exported=s.archiveRepository.export(File(context.cacheDir,"exports/ezmenu-backup.ezmenu"));save.launch("旬食备份.ezmenu")}catch(e:Exception){message="备份失败："+e.message}finally{busy=false}}},enabled=!busy){Text("创建备份")}
        }}
        item{Paper{
            Text("从备份恢复",style=MaterialTheme.typography.headlineSmall)
            Text("恢复前会检查文件，并保留本机副本。")
            OutlinedButton({pick.launch("*/*")},enabled=!busy){Text("选择备份文件")}
        }}
        if(busy)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
        message?.let{item{Text(it,color=MaterialTheme.colorScheme.primary)}}
    }}
    inspection?.let{info->AlertDialog(onDismissRequest={if(!busy)inspection=null},title={Text("恢复这份档案？")},text={Text(info.recipes.toString()+" 道食谱、"+info.entries+" 条手记、"+info.photos+" 张照片。恢复后会替换本机档案，并结束正在进行的烹饪。")},confirmButton={TextButton({scope.launch{busy=true;try{s.archiveRepository.restore(requireNotNull(importFile));message="档案已恢复";inspection=null}catch(e:Exception){message="恢复失败，已尝试还原原档案："+e.message;inspection=null}finally{busy=false}}},enabled=!busy){Text("确认恢复")}},dismissButton={TextButton({inspection=null},enabled=!busy){Text("取消")}})}
}

@Composable fun TrashChapter(onBack: ()->Unit,onOpen: (Long)->Unit) {
    val s=LocalStudio.current;val scope=rememberCoroutineScope { kotlinx.coroutines.Dispatchers.Main.immediate };var removing by remember{mutableStateOf<Long?>(null)}
    val rows by remember{s.recipeRepository.browse(scope=3,mode="trash",limit=10000)}.collectAsStateWithLifecycle(emptyList())
    Column{StudioTopBar("回收站",onBack);LazyColumn(contentPadding=PaddingValues(StudioMargin),verticalArrangement=Arrangement.spacedBy(18.dp)){
        if(rows.isEmpty())item{EmptyChapter("回收站为空")}
        items(rows,key={it.recipe.id}){r->Paper{Text(r.recipe.title,style=MaterialTheme.typography.headlineSmall);Row{TextButton({scope.launch{s.database.recipeDao().trash(r.recipe.id,null)}}){Text("恢复食谱")};TextButton({removing=r.recipe.id}){Text("永久删除",color=MaterialTheme.colorScheme.error)}}}}
    }}
    removing?.let{id->AlertDialog(onDismissRequest={removing=null},title={Text("永久删除食谱？")},text={Text("这个操作无法撤销。已经保存的料理手记和当时配方仍会保留。")},confirmButton={TextButton({scope.launch{s.database.recipeDao().permanentlyDelete(id);s.database.journalDao().deleteRecipeDraft(id);removing=null}}){Text("永久删除")}},dismissButton={TextButton({removing=null}){Text("取消")}})}
}
@Composable fun AboutChapter(onBack: ()->Unit) {
    Column{StudioTopBar("关于旬食",onBack);LazyColumn(contentPadding=PaddingValues(StudioMargin),verticalArrangement=Arrangement.spacedBy(20.dp)){
        item{EditorialHeading("旬食 · EzMenu",subtitle="2.0")}
        item{Paper{Text("关于你的档案",style=MaterialTheme.typography.headlineSmall);Text("应用不接入网络或账号，数据存于本机。请定期通过备份功能保存档案。系统强制停止应用期间无法提供到时提醒。")}}
        item{Paper{Text("素材与开源组件",style=MaterialTheme.typography.headlineSmall);Text("内置食谱封面为 AI 生成示意图。中文标题字体：Noto Serif SC（SIL OFL）；拉丁字体：Newsreader、Manrope（SIL OFL）；图标：Material Symbols（Apache 2.0）。使用 AndroidX Compose、Room、DataStore 与 Coil。完整来源与许可见随项目提供的说明。")}}
    }}
}

@Composable fun ShareChapter(kind: String,id: Long,onBack: ()->Unit) {
    val s=LocalStudio.current;val context=LocalContext.current;val scope=rememberCoroutineScope { kotlinx.coroutines.Dispatchers.Main.immediate }
    var snapshot by remember{mutableStateOf<RecipeSnapshot?>(null)};var log by remember{mutableStateOf<com.ezmenu.app.data.local.entity.CookLogEntity?>(null)}
    var preview by remember{mutableStateOf<File?>(null)};var pending by remember{mutableStateOf<File?>(null)};var busy by remember{mutableStateOf(false)};var message by remember{mutableStateOf<String?>(null)}
    val exporter=remember{RecipeExport(context)}
    LaunchedEffect(kind,id){busy=true;try{snapshot=if(kind=="journal"){log=s.database.journalDao().entry(id);log?.snapshot?.let{RecipeSnapshot.decode(it)}}else s.recipeRepository.getForEditing(id)?.let{RecipeSnapshot(it,it.recipe.baseServings)}
        snapshot?.let{preview=exporter.image(it,log)}?:run{message="这条旧版记录未保存完整配方，无法导出。"}
    }catch(e:Exception){message="预览生成失败，请返回重试"}finally{busy=false}}
    val save=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")){uri->if(uri!=null)scope.launch{busy=true;try{withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri).use{output->requireNotNull(output);pending!!.inputStream().use{it.copyTo(output)}}};message="文件已保存"}catch(e:Exception){message="保存失败，请重试"}finally{busy=false}}}
    Column{StudioTopBar("导出与分享",onBack);LazyColumn(contentPadding=PaddingValues(StudioMargin),verticalArrangement=Arrangement.spacedBy(18.dp)){
        preview?.let{f->item{AsyncImage(f,"分享卡片预览",Modifier.fillMaxWidth().aspectRatio(.8f))}}
        if(busy)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
        message?.let{item{Text(it,color=MaterialTheme.colorScheme.primary)}}
        item{Button({preview?.let{file->val uri=FileProvider.getUriForFile(context,"com.ezmenu.app.files",file);context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"分享食谱"))}},enabled=preview!=null&&!busy,modifier=Modifier.fillMaxWidth()){Text("分享图片")}}
        item{OutlinedButton({pending=preview;save.launch("旬食卡片.png")},enabled=preview!=null&&!busy,modifier=Modifier.fillMaxWidth()){Text("保存卡片图片")}}
        item{OutlinedButton({scope.launch{busy=true;try{pending=exporter.pdf(requireNotNull(snapshot),log);save.launch("旬食食谱.pdf")}catch(e:Exception){message="PDF 生成失败"}finally{busy=false}}},enabled=snapshot!=null&&!busy,modifier=Modifier.fillMaxWidth()){Text("导出完整 PDF")}}
        item{OutlinedButton({scope.launch{busy=true;try{pending=exporter.markdown(requireNotNull(snapshot),log);save.launch("旬食食谱-Markdown.zip")}catch(e:Exception){message="Markdown 生成失败"}finally{busy=false}}},enabled=snapshot!=null&&!busy,modifier=Modifier.fillMaxWidth()){Text("导出 Markdown 与图片包")}}
    }}
}
