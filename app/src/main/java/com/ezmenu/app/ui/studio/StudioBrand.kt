package com.ezmenu.app.ui.studio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ezmenu.app.ui.theme.*

@Composable fun BrandMark(modifier: Modifier=Modifier){
    val ink=MaterialTheme.colorScheme.primary
    Canvas(modifier.size(38.dp)){
        val unit=size.minDimension/40f
        drawCircle(ink.copy(alpha=.1f),radius=19f*unit)
        drawArc(ink,0f,180f,false,topLeft=androidx.compose.ui.geometry.Offset(8f*unit,18f*unit),size=androidx.compose.ui.geometry.Size(24f*unit,14f*unit),style=Stroke(1.5f*unit))
        drawLine(ink,androidx.compose.ui.geometry.Offset(7f*unit,18f*unit),androidx.compose.ui.geometry.Offset(33f*unit,18f*unit),strokeWidth=1.5f*unit)
        for(x in listOf(14f,20f,26f)){
            val path=Path().apply{moveTo(x*unit,14f*unit);cubicTo((x-3)*unit,11f*unit,(x+3)*unit,8f*unit,x*unit,5f*unit)}
            drawPath(path,ink,style=Stroke(1.2f*unit))
        }
        drawLine(ink,androidx.compose.ui.geometry.Offset(15f*unit,33f*unit),androidx.compose.ui.geometry.Offset(25f*unit,33f*unit),strokeWidth=1.5f*unit)
    }
}
@Composable fun BrandHeading(actions: @Composable RowScope.()->Unit={}){
    val large=LocalDensity.current.fontScale>1.3f
    Row(Modifier.fillMaxWidth().padding(top=4.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically){
        BrandMark()
        Row(Modifier.weight(1f).padding(start=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Text("旬食",Modifier.testTag("brand-name"),style=ezText(27f,36f,NewsreaderFamily,FontWeight.W500),color=MaterialTheme.colorScheme.primary)
            if(!large)Text("EzMenu",style=ezText(13f,20f,NewsreaderItalicFamily,FontWeight.W400,italic=true),color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        actions()
    }
}
@Composable fun ChapterRule(){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
        Box(Modifier.width(24.dp).height(2.dp).background(MaterialTheme.colorScheme.primary))
        HorizontalDivider(Modifier.weight(1f),color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.45f))
    }
}
