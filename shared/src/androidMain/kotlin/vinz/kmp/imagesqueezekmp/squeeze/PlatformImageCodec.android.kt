package vinz.kmp.imagesqueezekmp.squeeze

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

internal actual class PlatformBitmap(val value: Bitmap)

internal actual object PlatformImageCodec {
    actual suspend fun decodeSampled(source: ByteArray, reqWidth: Int, reqHeight: Int): PlatformBitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(source, 0, source.size, options)

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return null
            }

            options.apply {
                inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
                inJustDecodeBounds = false
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            BitmapFactory.decodeByteArray(source, 0, source.size, options)?.let(::PlatformBitmap)
        } catch (_: Throwable) {
            null
        }
    }

    actual suspend fun applyExifRotation(source: ByteArray, bitmap: PlatformBitmap): PlatformBitmap {
        val orientation = try {
            ExifInterface(ByteArrayInputStream(source)).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } catch (_: Throwable) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val angle = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }

        return try {
            val matrix = Matrix().apply { postRotate(angle) }
            val rotated = Bitmap.createBitmap(bitmap.value, 0, 0, bitmap.value.width, bitmap.value.height, matrix, true)
            PlatformBitmap(rotated)
        } catch (_: Throwable) {
            bitmap
        }
    }

    actual suspend fun encode(bitmap: PlatformBitmap, format: CompressionFormat, quality: Int): ByteArray? {
        val compressFormat = when (format) {
            CompressionFormat.JPEG -> Bitmap.CompressFormat.JPEG
            CompressionFormat.PNG -> Bitmap.CompressFormat.PNG
            CompressionFormat.WEBP -> Bitmap.CompressFormat.WEBP
        }

        return try {
            ByteArrayOutputStream().use { output ->
                val ok = bitmap.value.compress(compressFormat, quality.coerceIn(1, 100), output)
                if (ok) output.toByteArray() else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    actual fun recycle(bitmap: PlatformBitmap) {
        if (!bitmap.value.isRecycled) {
            bitmap.value.recycle()
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}
