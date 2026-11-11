package com.example.homemenu.data.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/** Photos live in filesDir/photos; the DB stores only the file name. */
@Singleton
class PhotoStorage @Inject constructor(@ApplicationContext private val context: Context) {

    val photosDir: File get() = File(context.filesDir, "photos").apply { mkdirs() }

    fun fileFor(name: String?): File? = name?.let { File(photosDir, it) }?.takeIf { it.exists() }

    /** Temp file + content:// URI for ActivityResultContracts.TakePicture. */
    fun newCameraUri(): Uri {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "shot_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** Decodes, rotates per EXIF, downsizes to [MAX_EDGE] px long edge, re-encodes JPEG ≤ [MAX_BYTES]. */
    suspend fun importPhoto(uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        // С inJustDecodeBounds = true decodeStream() всегда возвращает null и только заполняет размеры
        // в bounds, поэтому проверяем открытие потока, а не результат декодирования.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        (resolver.openInputStream(uri) ?: error("Не удалось открыть фото"))
            .use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Файл не является изображением" }

        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE) }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: error("Не удалось прочитать фото")
        val rotation = runCatching {
            resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        }.getOrDefault(0)

        val scaled = scaleAndRotate(decoded, MAX_EDGE, rotation)
        val bytes = compress(scaled)
        if (scaled !== decoded) decoded.recycle()
        scaled.recycle()

        val name = "${UUID.randomUUID()}.jpg"
        File(photosDir, name).writeBytes(bytes)
        name
    }

    fun delete(name: String?) {
        name?.let { File(photosDir, it).delete() }
    }

    private fun scaleAndRotate(src: Bitmap, maxEdge: Int, rotation: Int): Bitmap {
        val longEdge = max(src.width, src.height)
        val scale = if (longEdge > maxEdge) maxEdge.toFloat() / longEdge else 1f
        if (scale == 1f && rotation == 0) return src
        val m = Matrix().apply {
            postScale(scale, scale)
            if (rotation != 0) postRotate(rotation.toFloat())
        }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    private fun compress(bitmap: Bitmap): ByteArray {
        var quality = 88
        while (true) {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val bytes = out.toByteArray()
            if (bytes.size <= MAX_BYTES || quality <= 40) return bytes
            quality -= 12
        }
    }

    companion object {
        const val MAX_EDGE = 1080
        const val MAX_BYTES = 5 * 1024 * 1024

        fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
            var sample = 1
            while (max(width, height) / (sample * 2) >= maxEdge) sample *= 2
            return sample
        }
    }
}
