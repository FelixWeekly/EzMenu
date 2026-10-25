@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
package com.ezmenu.app.ui.studio

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

val LocalStudioFeedback=staticCompositionLocalOf<(String)->Unit>{{}}
val LocalCoverTransition=staticCompositionLocalOf<SharedTransitionScope?>{null}
val LocalCoverVisibility=staticCompositionLocalOf<AnimatedVisibilityScope?>{null}
val EditorialEasing=CubicBezierEasing(.2f,0f,0f,1f)

@Composable fun Modifier.coverTransition(id: Long): Modifier {
    val shared=LocalCoverTransition.current?:return this
    val visibility=LocalCoverVisibility.current?:return this
    return with(shared){this@coverTransition.sharedElement(rememberSharedContentState("recipe-cover/"+id),visibility,boundsTransform={_,_->tween(340,easing=EditorialEasing)})}
}
@Composable fun Modifier.pressFeedback(interactions: MutableInteractionSource): Modifier {
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if(pressed).976f else 1f,spring(dampingRatio=.8f,stiffness=650f),label="press")
    return graphicsLayer{scaleX=scale;scaleY=scale}
}
@Composable fun FavoriteAction(selected: Boolean,onClick: ()->Unit) {
    androidx.compose.material3.IconButton(onClick){
        AnimatedContent(selected,transitionSpec={scaleIn(tween(220),initialScale=.65f)+fadeIn(tween(140)) togetherWith scaleOut(tween(100),targetScale=.7f)+fadeOut(tween(90))},label="favorite") { value ->
            com.ezmenu.app.ui.icons.MsIcon(com.ezmenu.app.ui.icons.MsGlyph.Favorite,filled=value,contentDescription=if(value)"取消收藏" else "收藏",tint=if(value)androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
        }
    }
}
