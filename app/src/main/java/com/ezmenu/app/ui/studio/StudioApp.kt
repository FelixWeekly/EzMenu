@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
package com.ezmenu.app.ui.studio

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.createGraph
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import com.ezmenu.app.EzMenuApplication
import com.ezmenu.app.ui.theme.EzMenuTheme
import com.ezmenu.app.ui.icons.*
import com.ezmenu.app.ui.screens.editor.RecipeEditorScreen
import com.ezmenu.app.navigation.Routes

@Composable fun StudioApp() {
    val context=LocalContext.current
    val services=remember { (context.applicationContext as EzMenuApplication).services }
    val mode by services.settingsRepository.theme.collectAsStateWithLifecycle("system")
    val ready by services.ready.collectAsStateWithLifecycle()
    val error by services.initializationError.collectAsStateWithLifecycle()
    EzMenuTheme(mode) { CompositionLocalProvider(LocalStudio provides services) {
        if(!ready) Box(Modifier.fillMaxSize(),contentAlignment=androidx.compose.ui.Alignment.Center){CircularProgressIndicator()}
        else if(error!=null) Box(Modifier.fillMaxSize().padding(22.dp)){EmptyChapter("档案暂时无法打开",error!!)}
        else StudioNavigation()
    } }
}

@Composable private fun StudioNavigation() {
    val nav=rememberNavController()
    val snack=remember{SnackbarHostState()}
    val feedbackScope=rememberCoroutineScope { kotlinx.coroutines.Dispatchers.Main.immediate }
    val feedback: (String)->Unit={message->feedbackScope.launch{snack.currentSnackbarData?.dismiss();snack.showSnackbar(message)}}
    val entry by nav.currentBackStackEntryAsState()
    val route=entry?.destination?.route
    val tabs=listOf("library" to "食谱","discover" to "灵感","journal" to "手记","profile" to "我的")
    val icons=listOf(MsGlyph.MenuBook,MsGlyph.Explore,MsGlyph.HistoryEdu,MsGlyph.PersonOutline)
    val open: (Long)->Unit = { nav.navigate("recipe/"+it) }
    val editor: (Long?)->Unit = { nav.navigate(Routes.editor(it)) }
    val cook: (Long,Int)->Unit = { id,n -> nav.navigate("cook/"+id+"?servings="+n) }
    val graph=remember(nav) { nav.createGraph(startDestination="library") {
                composable("library") { CompositionLocalProvider(LocalCoverVisibility provides this) { LibraryChapter(open,editor,{nav.navigate("discover")},cook) } }
                composable("discover") { InspirationChapter(open) }
                composable("journal") { JournalChapter(onOpen={nav.navigate("entry/"+it)}) }
                composable("profile") { ProfileChapter({nav.navigate("settings")},{nav.navigate("archive")},{nav.navigate("trash")},{nav.navigate("drafts")},{nav.navigate("about")}) }
                composable("settings") { SettingsChapter { nav.popBackStack() } }
                composable("archive") { ArchiveChapter { nav.popBackStack() } }
                composable("about") { AboutChapter { nav.popBackStack() } }
                composable("trash") { TrashChapter({nav.popBackStack()},open) }
                composable("drafts") { DraftsChapter({nav.popBackStack()},editor) }
                composable("recipe/{recipeId}",listOf(navArgument("recipeId"){type=NavType.LongType})) { e ->
                    CompositionLocalProvider(LocalCoverVisibility provides this) { RecipeChapter(e.arguments!!.getLong("recipeId"),{nav.popBackStack()},editor,cook,{nav.navigate("entry/"+it)},{nav.navigate("share/recipe/"+it)}) }
                }
                composable("entry/{id}",listOf(navArgument("id"){type=NavType.LongType})) { e ->
                    JournalEntryChapter(e.arguments!!.getLong("id"),{nav.popBackStack()},{nav.navigate("share/journal/"+it)})
                }
                composable("share/{kind}/{id}",listOf(navArgument("kind"){type=NavType.StringType},navArgument("id"){type=NavType.LongType})) { e ->
                    ShareChapter(e.arguments!!.getString("kind")!!,e.arguments!!.getLong("id")){nav.popBackStack()}
                }
                composable(Routes.EDITOR_NEW) { RecipeEditorScreen({nav.popBackStack()},{result -> nav.finishStudioEditor(result) },{nav.navigate("drafts")}) }
                composable(Routes.EDITOR_PATTERN,listOf(navArgument("recipeId"){type=NavType.LongType})) { RecipeEditorScreen({nav.popBackStack()},{result -> nav.finishStudioEditor(result) },{nav.navigate("drafts")}) }
                composable("cook/{recipeId}?servings={servings}",listOf(navArgument("recipeId"){type=NavType.LongType},navArgument("servings"){type=NavType.IntType;defaultValue=2})) { e ->
                    CookingChapter(e.arguments!!.getLong("recipeId"),e.arguments!!.getInt("servings"),{nav.popBackStack()}) { id ->
                        nav.navigate("entry/"+id){popUpTo(nav.currentDestination!!.id){inclusive=true};launchSingleTop=true}
                    }
                }
    } }
    CompositionLocalProvider(LocalStudioFeedback provides feedback) {
    Scaffold(containerColor=MaterialTheme.colorScheme.surface,
        snackbarHost={
            val footer=route==Routes.EDITOR_NEW||route==Routes.EDITOR_PATTERN||route?.startsWith("cook/")==true
            val gap=if(footer)if(androidx.compose.ui.platform.LocalDensity.current.fontScale>1.3f)160.dp else 96.dp else 16.dp
            SnackbarHost(snack,Modifier.padding(bottom=gap)){data->Snackbar(data,shape=RoundedCornerShape(14.dp))}
        },
        bottomBar={if(route in tabs.map{it.first}) ChapterNavigation(route,tabs,icons){tab->
            nav.navigate(tab){popUpTo("library"){saveState=true};launchSingleTop=true;restoreState=true}
        }}

    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            SharedTransitionLayout {
                CompositionLocalProvider(LocalCoverTransition provides this){
                    NavHost(nav,graph,
                        enterTransition={if(targetState.destination.route=="recipe/{recipeId}")fadeIn(tween(340,easing=EditorialEasing)) else fadeIn(tween(200))+slideInHorizontally(tween(260,easing=EditorialEasing)){it/20}},
                        exitTransition={fadeOut(tween(160))},
                        popEnterTransition={fadeIn(tween(300,easing=EditorialEasing))},
                        popExitTransition={fadeOut(tween(240))+slideOutHorizontally(tween(280,easing=EditorialEasing)){it/24}})
                }
            }
        }
    }
    }
}

private fun androidx.navigation.NavHostController.finishStudioEditor(result: com.ezmenu.app.domain.SavedRecipeResult) {
    val previous=previousBackStackEntry
    val target=if(result.isDraft)"drafts" else "recipe/"+result.recipeId
    if((result.isDraft && previous?.destination?.route=="drafts") ||
       (!result.isDraft && previous?.destination?.route=="recipe/{recipeId}" && previous.arguments?.getLong("recipeId")==result.recipeId)) {
        popBackStack()
    } else navigate(target) { popUpTo(currentDestination!!.id){inclusive=true};launchSingleTop=true }
}

@Composable private fun ChapterNavigation(route: String?,tabs: List<Pair<String,String>>,icons: List<MsGlyph>,onSelect: (String)->Unit){
    Surface(color=MaterialTheme.colorScheme.surfaceContainerLowest){Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars)){
        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.4f))
        BoxWithConstraints(Modifier.fillMaxWidth()){
            val slot=maxWidth/tabs.size
            val selected=tabs.indexOfFirst{it.first==route}.coerceAtLeast(0)
            val offset by animateDpAsState(slot*selected,tween(280,easing=EditorialEasing),label="navigation-position")
            Box(Modifier.offset{IntOffset((offset+(slot-54.dp)/2).roundToPx(),8.dp.roundToPx())}.size(54.dp,30.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha=.65f)))
            Row(Modifier.fillMaxWidth()){
                tabs.forEachIndexed{i,tab->
                    val color by animateColorAsState(if(i==selected)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,tween(180),label="navigation-color")
                    Column(Modifier.weight(1f).testTag("nav-"+tab.first).selectable(i==selected,role=Role.Tab,onClick={onSelect(tab.first)}).padding(vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){
                        MsIcon(icons[i],size=22.dp,tint=color)
                        Text(tab.second,style=MaterialTheme.typography.labelMedium,color=color)
                    }
                }
            }
        }
    }}
}
