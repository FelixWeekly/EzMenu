package com.ezmenu.app.ui.studio

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ezmenu.app.domain.*
import com.ezmenu.app.domain.model.RecipeSnapshot
import com.ezmenu.app.ui.components.EzPhoto
import com.ezmenu.app.ui.icons.*
import kotlinx.coroutines.launch

@Composable fun LibraryChapter(onOpen: (Long)->Unit,onEdit: (Long?)->Unit,onDiscover: ()->Unit,onCook: (Long,Int)->Unit) {
    val s=LocalStudio.current
    val wide=androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.width / androidx.compose.ui.platform.LocalDensity.current.density >= 840f
    var query by rememberSaveable{mutableStateOf("")};var mode by rememberSaveable{mutableStateOf("all")}
    var category by rememberSaveable{mutableStateOf("")};var sort by rememberSaveable{mutableStateOf("updated")};var limit by rememberSaveable{mutableIntStateOf(40)}
    val rows by remember(query,mode,category,sort,limit){s.recipeRepository.browse(query,1,mode,category,sort,limit)}.collectAsStateWithLifecycle(emptyList())
    val categories by remember{s.database.recipeDao().categories()}.collectAsStateWithLifecycle(emptyList())
    val active by remember{s.cookingSessions.active()}.collectAsStateWithLifecycle(null)
    val drafts by remember{s.database.journalDao().drafts()}.collectAsStateWithLifecycle(emptyList())
    val large=androidx.compose.ui.platform.LocalDensity.current.fontScale>1.3f
    val visible=rows.toList()
    key(wide) {
        LazyVerticalGrid(GridCells.Adaptive(if(wide)260.dp else 144.dp),Modifier.testTag("library-grid"),contentPadding=PaddingValues(StudioMargin),
            horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item(key="library-header",span={GridItemSpan(maxLineSpan)}) {
                Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    BrandHeading{
                        StudioAction(MsGlyph.Explore,"寻找灵感",onDiscover)
                        FilledIconButton({onEdit(null)},shape=RoundedCornerShape(14.dp),modifier=Modifier.size(48.dp)){MsIcon(MsGlyph.Add,contentDescription="新建食谱",tint=MaterialTheme.colorScheme.onPrimary)}
                    }
                    ChapterRule()
                    SearchBox(query,{query=it;limit=40},"搜索菜名、食材或标签")
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        listOf("all" to "全部","favorites" to "收藏","drafts" to "草稿").forEach{(key,label)->
                            Column(Modifier.padding(end=20.dp),horizontalAlignment=Alignment.CenterHorizontally){
                                TextButton({mode=key;limit=40},contentPadding=PaddingValues(horizontal=0.dp)){
                                    Text(label,style=MaterialTheme.typography.titleMedium,color=if(mode==key)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Box(Modifier.size(22.dp,2.dp).background(if(mode==key)MaterialTheme.colorScheme.primary else Color.Transparent,RoundedCornerShape(1.dp)))
                            }
                        }
                        Spacer(Modifier.weight(1f));if(!large)LibrarySort(sort){sort=it}
                    }
                    if(large)LibrarySort(sort){sort=it}
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        item{ChoicePill("所有分类",category.isBlank()){category=""}}
                        items(categories.filter{it!="全部"}){c->ChoicePill(c,category==c){category=c}}
                    }
                    active?.let{a->Surface(onClick={onCook(a.recipeId,a.servings)},shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.primaryContainer){
                        Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                            MsIcon(MsGlyph.Cooking,tint=MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)){Text("继续下厨",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary);Text(RecipeSnapshot.decode(a.snapshot).graph.recipe.title,style=MaterialTheme.typography.bodyMedium)}
                            MsIcon(MsGlyph.ArrowForward,tint=MaterialTheme.colorScheme.primary)
                        }
                    }}
                    drafts.firstOrNull()?.let{d->TextButton({onEdit(d.recipeId)},contentPadding=PaddingValues(0.dp)){MsIcon(MsGlyph.HistoryEdu);Spacer(Modifier.width(8.dp));Text("继续编写 · "+d.title.ifBlank{"未命名食谱"})}}
                }
            }
            if(visible.isEmpty())item(key="library-empty",span={GridItemSpan(maxLineSpan)}){EmptyChapter(if(query.isNotBlank())"没有找到食谱" else if(mode=="favorites")"暂无收藏" else if(mode=="drafts")"暂无草稿" else "还没有食谱",action=if(query.isBlank()&&mode=="all")"寻找灵感" else null,onAction=onDiscover)}
            items(visible,key={it.recipe.id}){r->RecipeTile(r,Modifier.animateItem(fadeInSpec=tween(180),placementSpec=tween(280),fadeOutSpec=tween(100)),onClick={if(r.recipe.isDraft)onEdit(r.recipe.id)else onOpen(r.recipe.id)})}
            if(visible.size>=limit)item(key="library-more",span={GridItemSpan(maxLineSpan)}){TextButton({limit+=40}){Text("加载更多")}}
        }
    }
}
@Composable private fun LibrarySort(sort: String,onChange: (String)->Unit) {
    var expanded by remember{mutableStateOf(false)}
    Box{TextButton({expanded=true},contentPadding=PaddingValues(horizontal=0.dp)){
        Text(when(sort){"time"->"耗时";"cooked"->"最近下厨";else->"最近修改"},style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        MsIcon(MsGlyph.ArrowDownward,size=14.dp,tint=MaterialTheme.colorScheme.onSurfaceVariant)
    }
        DropdownMenu(expanded,{expanded=false}){listOf("updated" to "最近修改","cooked" to "最近下厨","time" to "按耗时").forEach{(key,label)->DropdownMenuItem({Text(label)},{onChange(key);expanded=false})}}
    }
}

@Composable fun InspirationChapter(onOpen: (Long)->Unit) {
    val s=LocalStudio.current; val scope=rememberCoroutineScope { kotlinx.coroutines.Dispatchers.Main.immediate }
    var query by rememberSaveable{mutableStateOf("")};var input by rememberSaveable{mutableStateOf("")};var page by rememberSaveable{mutableIntStateOf(0)}
    val leftovers by s.settingsRepository.leftovers.collectAsStateWithLifecycle(emptyList())
    val rows by remember(query){s.recipeRepository.browse(query,2,limit=200)}.collectAsStateWithLifecycle(emptyList())
    val ingredientRows by remember{s.database.recipeDao().recommendationIngredients()}.collectAsStateWithLifecycle(emptyList())
    val all by remember{s.recipeRepository.browse(scope=0,limit=10000)}.collectAsStateWithLifecycle(emptyList())
    val ranked=remember(ingredientRows,leftovers){IngredientRecommendations.rank(ingredientRows.groupBy{it.recipeId}.map{(id,v)->RecipeIngredients(id,v.map{it.name})},leftovers)}
    val picks=if(ranked.isEmpty())emptyList() else List(minOf(2,ranked.size)){ranked[(page*2+it)%ranked.size]}.mapNotNull{id->all.firstOrNull{it.recipe.id==id}}
    val featured=if(query.isBlank())rows.firstOrNull()else null
    LazyVerticalGrid(GridCells.Adaptive(144.dp),contentPadding=PaddingValues(StudioMargin),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item(span={GridItemSpan(maxLineSpan)}){Column(verticalArrangement=Arrangement.spacedBy(14.dp)){
            ChapterHeading("灵感")
            SearchBox(query,{query=it},"搜索灵感食谱")
        }}
        featured?.let{item(span={GridItemSpan(maxLineSpan)}){
            Surface(onClick={onOpen(it.recipe.id)},shape=RoundedCornerShape(20.dp)){
                Box(Modifier.fillMaxWidth().aspectRatio(1.5f)){
                    EzPhoto(it.recipe.coverImageRef,it.recipe.title,Modifier.fillMaxSize(),placeholderLabel=it.recipe.title)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(alpha=.7f)),startY=150f)))
                    Column(Modifier.align(Alignment.BottomStart).padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                        Text("内置食谱",style=MaterialTheme.typography.labelSmall,color=Color.White.copy(alpha=.85f))
                        Text(it.recipe.title,style=MaterialTheme.typography.headlineMedium,color=Color.White)
                        Text(it.recipe.cookTimeMinutes.toString()+" 分钟 · "+it.recipe.category,style=MaterialTheme.typography.labelMedium,color=Color.White.copy(alpha=.85f))
                    }
                }
            }
        }}
        item(span={GridItemSpan(maxLineSpan)}){SectionTitle("食材推荐",action={if(ranked.size>2)TextButton({page++}){MsIcon(MsGlyph.ChangeCircle);Spacer(Modifier.width(4.dp));Text("换一组")}})}
        item(span={GridItemSpan(maxLineSpan)}) {Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            if(leftovers.isNotEmpty())LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items(leftovers){name->InputChip(true,{scope.launch{s.settingsRepository.setLeftovers(leftovers-name);page=0}},label={Text(name)},trailingIcon={MsIcon(MsGlyph.Close,size=14.dp)})}}
            TextField(input,{input=it},Modifier.fillMaxWidth(),placeholder={Text("添加食材，如番茄、鸡蛋",style=MaterialTheme.typography.bodyMedium,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)},
                leadingIcon={MsIcon(MsGlyph.Kitchen)},trailingIcon={IconButton(onClick={val names=input.split('，',',','、').map{it.trim()}.filter{it.isNotBlank()};scope.launch{s.settingsRepository.setLeftovers((leftovers+names).distinct());page=0};input=""},enabled=input.isNotBlank()){MsIcon(MsGlyph.Add,contentDescription="添加食材")}},singleLine=true,shape=RoundedCornerShape(14.dp),
                colors=TextFieldDefaults.colors(focusedContainerColor=MaterialTheme.colorScheme.surfaceContainerLow,unfocusedContainerColor=MaterialTheme.colorScheme.surfaceContainerLow,focusedIndicatorColor=Color.Transparent,unfocusedIndicatorColor=Color.Transparent))
        } }
        if(leftovers.isNotEmpty()&&picks.isEmpty())item(span={GridItemSpan(maxLineSpan)}){Text("暂无匹配食谱",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        items(picks,key={"pick"+it.recipe.id}){r->RecipeTile(r,Modifier.animateItem(fadeInSpec=tween(180),placementSpec=tween(280)),onClick={onOpen(r.recipe.id)})}
        item(span={GridItemSpan(maxLineSpan)}){SectionTitle("内置食谱")}
        items(rows.filter{it.recipe.id!=featured?.recipe?.id},key={it.recipe.id}){r->RecipeTile(r,onClick={onOpen(r.recipe.id)})}
        if(rows.isEmpty())item(span={GridItemSpan(maxLineSpan)}){EmptyChapter("没有找到食谱")}
    }
}

@Composable fun DraftsChapter(onBack: ()->Unit,onEdit: (Long?)->Unit) {
    val s=LocalStudio.current;val drafts by remember{s.database.journalDao().drafts()}.collectAsStateWithLifecycle(emptyList())
    val rows by remember{s.recipeRepository.browse(mode="drafts",limit=10000)}.collectAsStateWithLifecycle(emptyList())
    Column {StudioTopBar("草稿箱",onBack);LazyColumn(contentPadding=PaddingValues(StudioMargin),verticalArrangement=Arrangement.spacedBy(14.dp)){
        items(drafts,key={"auto"+it.key}){d->Paper{Text(d.title.ifBlank{"未命名食谱"},style=MaterialTheme.typography.headlineSmall);Text(dateLabel(d.updatedAt),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);TextButton({onEdit(d.recipeId)}){Text("继续编写")}}}
        items(rows,key={it.recipe.id}){r->RecipeTile(r,onClick={onEdit(r.recipe.id)})}
        if(drafts.isEmpty()&&rows.isEmpty())item{EmptyChapter("暂无草稿",action="新建食谱",onAction={onEdit(null)})}
    }}
}
