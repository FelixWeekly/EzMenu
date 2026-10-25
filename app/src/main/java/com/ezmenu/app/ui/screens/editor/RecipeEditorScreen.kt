package com.ezmenu.app.ui.screens.editor

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.ezmenu.app.ui.icons.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ezmenu.app.domain.SavedRecipeResult
import com.ezmenu.app.ui.components.EzPhoto
import com.ezmenu.app.ui.studio.*
import com.ezmenu.app.util.ImageStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@Composable fun RecipeEditorScreen(
    onBack: ()->Unit,onSaved: (SavedRecipeResult)->Unit,onOpenDrafts: ()->Unit,
    modifier: Modifier=Modifier,viewModel: RecipeEditorViewModel=viewModel(factory=RecipeEditorViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val feedback=LocalStudioFeedback.current
    val context=LocalContext.current;val scope=rememberCoroutineScope { kotlinx.coroutines.Dispatchers.Main.immediate }
    var importBusy by remember{mutableStateOf(false)};var imageStep by remember{mutableStateOf<Long?>(null)}
    var discard by remember{mutableStateOf(false)};var timerStep by remember{mutableStateOf<Long?>(null)}
    var timerText by remember{mutableStateOf("")};var batchPreview by remember{mutableStateOf<List<EditableIngredient>?>(null)}
    val large=LocalDensity.current.fontScale>1.3f
    val enabled=!state.loading&&!state.isSaving&&state.loadError==null&&!importBusy
    val back: ()->Unit={scope.launch{viewModel.flushDraft();onBack()}}
    BackHandler(onBack=back)
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->if(uri!=null)scope.launch{importBusy=true;try{
        val ref=ImageStorage.copyIntoAppStorage(context,uri)
        if(ref==null)viewModel.onCoverImportFailed() else imageStep?.let{id->viewModel.onStepChange(id){it.copy(imageRef=ref)}}?:viewModel.onCoverChange(ref)
    }finally{importBusy=false}}}
    val lifecycle=androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state.savedResult){state.savedResult?.let{result ->
        lifecycle.currentStateFlow.first { it.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) }
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) { viewModel.onExitHandled();feedback(if(result.isDraft)"草稿已暂存"else"食谱已保存");onSaved(result) }
    }}
    Column(modifier.fillMaxSize().imePadding()){
        StudioTopBar(if(state.isPublished)"修改食谱" else "新建食谱",back,actions={TextButton(onOpenDrafts){Text("草稿箱")}})
        LazyColumn(Modifier.weight(1f).testTag("editor-list"),contentPadding=PaddingValues(StudioMargin),verticalArrangement=Arrangement.spacedBy(18.dp)){
            
            state.loadError?.let{item{Text(it,color=MaterialTheme.colorScheme.error)}}
            item{Paper(Modifier.fillMaxWidth()){
                val info: @Composable ()->Unit={
                    EditorField(state.title,viewModel::onTitleChange,Modifier.fillMaxWidth().testTag("recipe-title"),label={Text("食谱名称")},enabled=enabled)
                    Spacer(Modifier.height(10.dp))
                    EditorField(state.story,viewModel::onStoryChange,Modifier.fillMaxWidth(),label={Text("简介")},minLines=2,enabled=enabled)
                }
                val cover: @Composable (Modifier)->Unit={m->Box(m.clip(RoundedCornerShape(12.dp)).clickable(enabled=enabled){imageStep=null;picker.launch("image/*")}){
                    EzPhoto(state.coverRef,"食谱封面",Modifier.fillMaxSize(),placeholderLabel=state.title)
                    Surface(Modifier.align(Alignment.BottomEnd).padding(6.dp),shape=RoundedCornerShape(8.dp),color=MaterialTheme.colorScheme.surface.copy(alpha=.9f)){
                        Box(Modifier.padding(7.dp)){MsIcon(MsGlyph.PhotoCamera,contentDescription="选择封面",tint=MaterialTheme.colorScheme.primary)}
                    }
                }}
                if(large){cover(Modifier.fillMaxWidth().aspectRatio(2f));info()}else Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    cover(Modifier.width(96.dp).height(166.dp));Column(Modifier.weight(1f)){info()}
                }
                if(importBusy)LinearProgressIndicator(Modifier.fillMaxWidth())
                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.3f))
                if(large){EditorField(state.category,viewModel::onCategoryChange,Modifier.fillMaxWidth(),label={Text("分类")},singleLine=true,enabled=enabled);EditorField(state.tags.joinToString("，"),viewModel::onTagsChange,Modifier.fillMaxWidth(),label={Text("标签")},enabled=enabled)}
                else Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    EditorField(state.category,viewModel::onCategoryChange,Modifier.weight(1f),label={Text("分类")},singleLine=true,enabled=enabled)
                    EditorField(state.tags.joinToString("，"),viewModel::onTagsChange,Modifier.weight(1f),label={Text("标签")},enabled=enabled)
                }
                if(large){EditorField(state.cookMinutes.toString(),{viewModel.onCookMinutesChange(it.toIntOrNull()?.coerceIn(0,1440)?:0)},Modifier.fillMaxWidth(),label={Text("耗时（分钟）")},singleLine=true);ServingsControl(state.servings,{viewModel.onServingsDelta(it-state.servings)})}
                else Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){
                    EditorField(state.cookMinutes.toString(),{viewModel.onCookMinutesChange(it.toIntOrNull()?.coerceIn(0,1440)?:0)},Modifier.weight(1f),label={Text("耗时（分钟）")},singleLine=true)
                    ServingsControl(state.servings,{viewModel.onServingsDelta(it-state.servings)})
                }
            }}
            item{SectionTitle("食材与用量",action={TextButton(viewModel::onOpenBatchSheet){Text("批量录入")}})}
            state.groups.forEachIndexed{index,group->
                item(key="group"+group.id){EditorField(group.name,{viewModel.onGroupName(group.id,it)},Modifier.fillMaxWidth(),label={Text("食材分组")})}
                val rows=state.ingredients.filter{it.groupId==group.id||(it.groupId==0L&&index==0)}
                items(rows,key={"ingredient"+it.uid}){r->Paper{
                    EditorField(r.name,{v->viewModel.onIngredientChange(r.uid){it.copy(name=v)}},Modifier.fillMaxWidth(),label={Text("食材名称")})
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                        EditorField(r.amountText,{v->viewModel.onIngredientChange(r.uid){it.copy(amountText=v,isRawAmount=v.toDoubleOrNull()==null)}},Modifier.weight(1f),label={Text("用量")})
                        EditorField(r.unit,{v->viewModel.onIngredientChange(r.uid){it.copy(unit=v)}},Modifier.weight(1f),label={Text("单位")})
                    }
                    TextButton({viewModel.onRemoveIngredient(r.uid)}){Text("删除食材")}
                }}
                item(key="add"+group.id){OutlinedButton({viewModel.onAddIngredientToGroup(group.id)}){Text("＋ 添加食材")}}
            }
            item{TextButton({viewModel.onAddGroup("新的分组")}){Text("＋ 新建食材分组")}}
            item{SectionTitle("烹饪步骤")}
            items(state.steps,key={"step"+it.uid}){step->Paper{
                Text("第 "+(state.steps.indexOf(step)+1)+" 步",style=MaterialTheme.typography.headlineSmall)
                EditorField(step.title,{v->viewModel.onStepChange(step.uid){it.copy(title=v)}},Modifier.fillMaxWidth(),label={Text("步骤标题")})
                EditorField(step.body,{v->viewModel.onStepChange(step.uid){it.copy(body=v)}},Modifier.fillMaxWidth().testTag("step-body"),label={Text("具体做法")},minLines=3)
                EditorField(step.noteText,{v->viewModel.onStepChange(step.uid){it.copy(noteText=v)}},Modifier.fillMaxWidth(),label={Text("步骤备注")})
                if(step.imageRef!=null)EzPhoto(step.imageRef,"步骤配图",Modifier.fillMaxWidth().aspectRatio(1.6f))
                TextButton({imageStep=step.uid;picker.launch("image/*")},enabled=!importBusy){Text("步骤配图")}
                TextButton({timerStep=step.uid;timerText=step.timerSeconds?.toString().orEmpty()}){Text(step.timerSeconds?.let{"计时 "+it+" 秒"}?:"添加计时")}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
                    StudioAction(MsGlyph.ArrowUpward,"上移",{viewModel.onMoveStep(step.uid,-1)})
                    StudioAction(MsGlyph.ArrowDownward,"下移",{viewModel.onMoveStep(step.uid,1)})
                    StudioAction(MsGlyph.ContentCopy,"复制",{viewModel.onCloneStep(step.uid)})
                    StudioAction(MsGlyph.Delete,"删除",{viewModel.onRemoveStep(step.uid)})
                }
            }}
            item{OutlinedButton(viewModel::onAddStep,Modifier.fillMaxWidth()){Text("＋ 添加步骤")}}
            state.toast?.let{item{Text(it,color=MaterialTheme.colorScheme.primary)}}
            item{TextButton({discard=true}){Text("放弃编辑")}}
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=StudioMargin,vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){
            if(!state.isPublished)OutlinedButton(viewModel::onSaveDraft,Modifier.weight(1f),enabled=enabled){Text("暂存草稿")}
            Button(viewModel::onPublish,Modifier.weight(1f),enabled=enabled){AnimatedContent(state.isSaving,label="save-state"){saving->Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){if(saving)CircularProgressIndicator(Modifier.size(16.dp),strokeWidth=2.dp,color=MaterialTheme.colorScheme.onPrimary);Text(if(saving)"正在保存…" else if(state.isPublished)"保存修改" else "收录食谱")}}}
        }
    }
    if(discard)AlertDialog(onDismissRequest={discard=false},title={Text("放弃这次编辑？")},text={Text("正式食谱不受影响，未保存的编辑草稿将被移除。")},confirmButton={TextButton({scope.launch{viewModel.discardDraft();onBack()}}){Text("放弃编辑")}},dismissButton={TextButton({discard=false}){Text("继续编写")}})
    timerStep?.let{id->AlertDialog(onDismissRequest={timerStep=null},title={Text("本步计时")},text={EditorField(timerText,{timerText=it},label={Text("秒数，1–86400；留空移除")})},confirmButton={TextButton({viewModel.onSetStepTimer(id,timerText.toIntOrNull());timerStep=null},enabled=timerText.isBlank()||(timerText.toIntOrNull()?:0) in 1..86400){Text("确定")}},dismissButton={TextButton({timerStep=null}){Text("取消")}})}
    if(state.batchSheetVisible)AlertDialog(onDismissRequest={viewModel.onCloseBatchSheet();batchPreview=null},title={Text("批量录入")},text={
        val preview=batchPreview
        if(preview==null)EditorField(state.batchText,viewModel::onBatchTextChange,label={Text("每行一种，如：鸡蛋 2个")},minLines=5)
        else LazyColumn{items(preview.size){i->val row=preview[i];EditorField(row.name,{v->batchPreview=preview.mapIndexed{j,r->if(i==j)r.copy(name=v)else r}},label={Text("食材")});EditorField(row.amountText,{v->batchPreview=preview.mapIndexed{j,r->if(i==j)r.copy(amountText=v,isRawAmount=v.toDoubleOrNull()==null)else r}},label={Text("数量 / 描述")});EditorField(row.unit,{v->batchPreview=preview.mapIndexed{j,r->if(i==j)r.copy(unit=v)else r}},label={Text("单位")})}}
    },confirmButton={TextButton({val preview=batchPreview;if(preview==null)batchPreview=state.batchText.lines().map{it.trim()}.filter{it.isNotBlank()}.mapIndexed{i,line->RecipeEditorViewModel.parseIngredientLine(line,i.toLong())}else{viewModel.onApplyParsed(preview);batchPreview=null}},enabled=state.batchText.isNotBlank()){Text(if(batchPreview==null)"预览解析" else "确认录入")}},dismissButton={TextButton({viewModel.onCloseBatchSheet();batchPreview=null}){Text("取消")}})
}

@Composable private fun EditorField(value: String,onValueChange: (String)->Unit,modifier: Modifier=Modifier,label: @Composable ()->Unit,minLines: Int=1,singleLine: Boolean=false,enabled: Boolean=true){
    TextField(value,onValueChange,modifier,label=label,minLines=minLines,singleLine=singleLine,enabled=enabled,shape=RoundedCornerShape(10.dp),textStyle=MaterialTheme.typography.bodyMedium,
        colors=TextFieldDefaults.colors(focusedContainerColor=MaterialTheme.colorScheme.surfaceContainerLow,unfocusedContainerColor=MaterialTheme.colorScheme.surfaceContainerLow,
            disabledContainerColor=MaterialTheme.colorScheme.surfaceContainerLow,focusedIndicatorColor=Color.Transparent,unfocusedIndicatorColor=Color.Transparent,disabledIndicatorColor=Color.Transparent))
}
