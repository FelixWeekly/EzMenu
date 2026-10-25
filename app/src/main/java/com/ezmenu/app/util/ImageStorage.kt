package com.ezmenu.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 导入时采样解码并归一化方向，持久化引用与应用安装目录无关。 */
object ImageStorage {
    private const val MAX_EDGE = 1600

    suspend fun copyIntoAppStorage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "covers")
        val target = File(dir, "cover_" + UUID.randomUUID() + ".jpg")
        val bitmaps = mutableListOf<Bitmap>()
        try {
            check(dir.isDirectory || dir.mkdirs())
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
            require(bounds.outWidth > 0 && bounds.outHeight > 0)
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_EDGE) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = requireNotNull(context.contentResolver.openInputStream(uri).use {
                BitmapFactory.decodeStream(it, null, options)
            })
            bitmaps += decoded
            val orientation = runCatching { context.contentResolver.openInputStream(uri).use { input ->
                ExifInterface(requireNotNull(input)).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            }
            val oriented = if (matrix.isIdentity) decoded else
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also { bitmaps += it }
            val longest = maxOf(oriented.width, oriented.height)
            val scaled = if (longest <= MAX_EDGE) oriented else {
                val ratio = MAX_EDGE.toFloat() / longest
                oriented.scale((oriented.width * ratio).toInt().coerceAtLeast(1),
                    (oriented.height * ratio).toInt().coerceAtLeast(1)).also { bitmaps += it }
            }
            FileOutputStream(target).use { check(scaled.compress(Bitmap.CompressFormat.JPEG, 88, it)) }
            "cover:" + target.name
        } catch (e: CancellationException) {
            target.delete()
            throw e
        } catch (e: Exception) {
            target.delete()
            Log.e("ImageStorage", "封面保存失败", e)
            null
        } finally {
            bitmaps.distinct().forEach { it.recycle() }
        }
    }
}
