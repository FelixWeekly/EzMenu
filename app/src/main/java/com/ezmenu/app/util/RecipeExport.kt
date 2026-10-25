package com.ezmenu.app.util

import android.content.Context
import android.graphics.*
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import android.graphics.pdf.PdfDocument
import android.text.*
import androidx.core.content.res.ResourcesCompat
import com.ezmenu.app.R
import com.ezmenu.app.ui.components.PhotoCatalog
import com.ezmenu.app.data.local.entity.CookLogEntity
import com.ezmenu.app.domain.IngredientAmount
import com.ezmenu.app.domain.QuantityFormatter
import com.ezmenu.app.domain.model.RecipeSnapshot
import com.ezmenu.app.data.repository.JournalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

class RecipeExport(private val context: Context) {
    private fun file(ext: String)=File(context.cacheDir,"exports/"+UUID.randomUUID()+"."+ext).also{it.parentFile?.mkdirs()}
    private fun photo(ref: String?): Bitmap? = if(ref?.startsWith("drawable:")==true)PhotoCatalog.resolve(ref)?.let{BitmapFactory.decodeResource(context.resources,it)} else CoverReferences.resolve(context.filesDir,ref)?.let{BitmapFactory.decodeFile(it.path)}
    private fun amount(s: RecipeSnapshot,i: com.ezmenu.app.data.local.entity.IngredientEntity)=QuantityFormatter.format(IngredientAmount(i.baseAmount,i.unitLabel,i.gramsPerUnit,i.rawDisplay),s.graph.recipe.baseServings,s.servings)
    fun text(s: RecipeSnapshot,log: CookLogEntity?=null): String=buildString {
        appendLine("# "+s.graph.recipe.title);appendLine();appendLine(s.graph.recipe.story.ifBlank{s.graph.recipe.subtitle});appendLine()
        appendLine(s.servings.toString()+" 人份 · "+s.graph.recipe.cookTimeMinutes+" 分钟")
        log?.let{appendLine();appendLine("## 料理手记");appendLine(it.note);appendLine("下次调整："+it.nextTime)}
        appendLine();appendLine("## 食材")
        s.graph.orderedGroups.forEach{(g,items)->appendLine();appendLine("### "+g.name);items.forEach{appendLine("- "+it.name+" · "+amount(s,it))}}
        appendLine();appendLine("## 做法")
        s.graph.orderedSteps.forEachIndexed{index,step->appendLine();appendLine("### "+(index+1)+". "+step.title);appendLine(step.body);step.noteText?.let{appendLine("提示："+it)};step.timerSeconds?.let{appendLine("计时："+it+" 秒")}}
        appendLine();appendLine("—— 旬食 · EzMenu")
    }
    private fun drawLines(canvas: Canvas,text: String,x: Float,y: Float,width: Int,size: Float,color: Int,maxLines: Int=Int.MAX_VALUE): Float {
        val paint=TextPaint(Paint.ANTI_ALIAS_FLAG).apply{textSize=size;this.color=color;typeface=ResourcesCompat.getFont(context,if(size>=48f)R.font.serif_cjk_400 else R.font.reading_sans_400)}
        val layout=StaticLayout.Builder.obtain(text,0,text.length,paint,width).setLineSpacing(8f,1f).setMaxLines(maxLines).setEllipsize(android.text.TextUtils.TruncateAt.END).build()
        canvas.withTranslation(x,y) { canvas.clipRect(0,0,width,(layout.getLineBottom(minOf(maxLines,layout.lineCount)-1)).coerceAtLeast(0));layout.draw(canvas) }
        return layout.height.toFloat()
    }
    suspend fun image(s: RecipeSnapshot,log: CookLogEntity?=null): File=withContext(Dispatchers.IO){
        val b=createBitmap(1080,1350);val c=Canvas(b);c.drawColor(0xFFF9F6F0.toInt())
        val p=Paint(Paint.ANTI_ALIAS_FLAG);p.color=0xFF98502F.toInt();p.textSize=38f;p.typeface=ResourcesCompat.getFont(context,R.font.serif_cjk_500);c.drawText("旬食",64f,82f,p)
        val ref=log?.let{JournalRepository.photos(it.photosJson).firstOrNull()}?:s.graph.recipe.coverImageRef
        photo(ref)?.let{c.drawBitmap(it,null,Rect(64,130,1016,780),p);it.recycle()}
        drawLines(c,s.graph.recipe.title,64f,824f,952,64f,0xFF2C2B27.toInt(),2)
        drawLines(c,log?.note?.ifBlank{s.graph.recipe.subtitle}?:s.graph.recipe.subtitle,64f,1008f,952,31f,0xFF777066.toInt(),3)
        p.color=0xFF98502F.toInt();p.textSize=28f;c.drawText(s.servings.toString()+" 人份   ·   "+s.graph.recipe.cookTimeMinutes+" 分钟",64f,1210f,p)
        p.textSize=23f;p.color=0xFF927661.toInt();c.drawText("EzMenu",64f,1290f,p)
        val out=file("png");try{out.outputStream().use{check(b.compress(Bitmap.CompressFormat.PNG,100,it))};out}finally{b.recycle()}
    }
    suspend fun pdf(s: RecipeSnapshot,log: CookLogEntity?=null): File=withContext(Dispatchers.IO){
        val out=file("pdf");val doc=PdfDocument();var pageNumber=0;var page: PdfDocument.Page?=null;var y=0f
        fun next(){page?.let{doc.finishPage(it)};pageNumber++;page=doc.startPage(PdfDocument.PageInfo.Builder(595,842,pageNumber).create());page!!.canvas.drawColor(Color.WHITE);y=44f}
        next()
        val paint=TextPaint(Paint.ANTI_ALIAS_FLAG).apply{textSize=14f;color=0xFF352A22.toInt()}
        try{
            text(s,log).lines().forEach{line->
                val heading=line.startsWith("#");val txt=line.trimStart('#',' ')
                paint.textSize=if(line.startsWith("# "))26f else if(heading)18f else 14f
                paint.typeface=if(heading)ResourcesCompat.getFont(context,R.font.serif_cjk_500) else ResourcesCompat.getFont(context,R.font.reading_sans_400)
                val layout=StaticLayout.Builder.obtain(txt,0,txt.length,paint,507).setLineSpacing(5f,1f).build()
                for(i in 0 until layout.lineCount){val h=layout.getLineBottom(i)-layout.getLineTop(i);if(y+h>792)next()
                    page!!.canvas.drawText(txt.substring(layout.getLineStart(i),layout.getLineEnd(i)).trimEnd(),44f,y-paint.fontMetrics.ascent,paint);y+=h
                }
                if(line.isBlank())y+=8f
            }
            val refs=(listOfNotNull(s.graph.recipe.coverImageRef)+s.graph.orderedSteps.mapNotNull{it.imageRef}+(log?.let{JournalRepository.photos(it.photosJson)}?:emptyList())).distinct()
            refs.forEach{ref->photo(ref)?.let{bitmap->val h=minOf(340f,507f*bitmap.height/bitmap.width);if(y+h>792)next();page!!.canvas.drawBitmap(bitmap,null,RectF(44f,y,551f,y+h),null);y+=h+18;bitmap.recycle()}}
            page?.let{doc.finishPage(it)};out.outputStream().use{doc.writeTo(it)};out
        }finally{doc.close()}
    }
    suspend fun markdown(s: RecipeSnapshot,log: CookLogEntity?=null): File=withContext(Dispatchers.IO){
        val out=file("zip");ZipOutputStream(out.outputStream()).use{zip->
            val md=StringBuilder(text(s,log));val refs=(listOfNotNull(s.graph.recipe.coverImageRef)+s.graph.steps.mapNotNull{it.imageRef}+(log?.let{JournalRepository.photos(it.photosJson)}?:emptyList())).distinct()
            refs.forEachIndexed{index,ref->photo(ref)?.let{b->val name="images/"+index+".jpg";zip.putNextEntry(ZipEntry(name));check(b.compress(Bitmap.CompressFormat.JPEG,88,zip));zip.closeEntry();b.recycle();md.append("\n![料理图片]("+name+")\n")}}
            zip.putNextEntry(ZipEntry("recipe.md"));zip.write(md.toString().toByteArray());zip.closeEntry()
        };out
    }
}
