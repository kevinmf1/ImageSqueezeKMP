package vinz.kmp.imagesqueezekmp.squeeze

import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

internal actual class PlatformBitmap(val value: BufferedImage)

internal actual object PlatformImageCodec {
    actual suspend fun decodeSampled(source: ByteArray, reqWidth: Int, reqHeight: Int): PlatformBitmap? {
        val input = ByteArrayInputStream(source)
        val image = ImageIO.read(input) ?: return null

        val inSampleSize = calculateInSampleSize(image.width, image.height, reqWidth, reqHeight)
        val targetWidth = (image.width / inSampleSize).coerceAtLeast(1)
        val targetHeight = (image.height / inSampleSize).coerceAtLeast(1)

        if (targetWidth == image.width && targetHeight == image.height) {
            return PlatformBitmap(image)
        }

        val scaled = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
        val g2d: Graphics2D = scaled.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.drawImage(image, 0, 0, targetWidth, targetHeight, null)
        g2d.dispose()
        return PlatformBitmap(scaled)
    }

    actual suspend fun applyExifRotation(source: ByteArray, bitmap: PlatformBitmap): PlatformBitmap {
        return bitmap
    }

    actual suspend fun encode(bitmap: PlatformBitmap, format: CompressionFormat, quality: Int): ByteArray? {
        return when (format) {
            CompressionFormat.JPEG -> encodeJpeg(bitmap.value, quality)
            CompressionFormat.PNG -> encodeViaImageIo(bitmap.value, "png")
            CompressionFormat.WEBP -> encodeViaImageIo(bitmap.value, "webp")
        }
    }

    actual fun recycle(bitmap: PlatformBitmap) {
        // No-op on JVM; GC handles memory lifecycle.
    }

    private fun encodeJpeg(bitmap: BufferedImage, quality: Int): ByteArray? {
        val rgbImage = if (bitmap.type == BufferedImage.TYPE_INT_RGB) {
            bitmap
        } else {
            val converted = BufferedImage(bitmap.width, bitmap.height, BufferedImage.TYPE_INT_RGB)
            val g = converted.createGraphics()
            g.drawImage(bitmap, 0, 0, null)
            g.dispose()
            converted
        }

        val writer = ImageIO.getImageWritersByFormatName("jpeg").asSequence().firstOrNull() ?: return null
        return try {
            ByteArrayOutputStream().use { output ->
                ImageIO.createImageOutputStream(output).use { ios ->
                    writer.output = ios
                    val params = writer.defaultWriteParam
                    if (params.canWriteCompressed()) {
                        params.compressionMode = ImageWriteParam.MODE_EXPLICIT
                        params.compressionQuality = quality.coerceIn(1, 100) / 100f
                    }
                    writer.write(null, IIOImage(rgbImage, null, null), params)
                    writer.dispose()
                    output.toByteArray()
                }
            }
        } catch (_: Throwable) {
            writer.dispose()
            null
        }
    }

    private fun encodeViaImageIo(bitmap: BufferedImage, format: String): ByteArray? {
        return try {
            ByteArrayOutputStream().use { output ->
                val ok = ImageIO.write(bitmap, format, output)
                if (ok) output.toByteArray() else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
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
