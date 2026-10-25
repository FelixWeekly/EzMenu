package com.ezmenu.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.ezmenu.app.util.CoverReferences
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ezmenu.app.ui.icons.MsIcon
import com.ezmenu.app.ui.icons.MsGlyph


@Composable
fun EzPhoto(
    ref: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderLabel: String? = null,
) {
    val drawableId = PhotoCatalog.resolve(ref)

    when {
        isLoadableUri(ref) -> {
            val file = CoverReferences.resolve(LocalContext.current.filesDir, ref)
            Box(modifier = modifier) {
                PhotoPlaceholder(Modifier.fillMaxSize())
                if (file != null || ref?.startsWith("content://") == true) AsyncImage(
                    model = file ?: ref, contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(), contentScale = contentScale,
                )
            }
        }

        drawableId != null -> {
            AsyncImage(
                model = drawableId,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
            )
        }

        else -> PhotoPlaceholder(modifier = modifier)
    }
}


private fun isLoadableUri(ref: String?): Boolean =
    ref != null && (
        ref.startsWith("cover:") ||
            ref.startsWith("content://") ||
            ref.startsWith("file://") ||
            ref.startsWith("/")
        )


@Composable
private fun PhotoPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(
                    androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer,
                    androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                    androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MsIcon(MsGlyph.Skillet,size=42.dp,tint=androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha=.28f))
        }
    }
}
