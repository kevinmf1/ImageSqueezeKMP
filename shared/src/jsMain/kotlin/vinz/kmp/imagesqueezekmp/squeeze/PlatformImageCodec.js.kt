@file:Suppress("UnsafeCastFromDynamic")

package vinz.kmp.imagesqueezekmp.squeeze

import kotlinx.browser.document
import kotlinx.coroutines.await
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.Promise

internal actual class PlatformBitmap(val value: HTMLCanvasElement)

internal actual object PlatformImageCodec {
    actual suspend fun decodeSampled(source: ByteArray, reqWidth: Int, reqHeight: Int): PlatformBitmap? {
        val bytes = source.toUint8Array()
        val blob = Blob(arrayOf(bytes), BlobPropertyBag(type = "image/*"))

        val url = js("URL.createObjectURL(blob)") as String
        return try {
            val image = loadImage(url)
            val inSample = calculateInSampleSize(image.naturalWidth, image.naturalHeight, reqWidth, reqHeight)
            val targetWidth = (image.naturalWidth / inSample).coerceAtLeast(1)
            val targetHeight = (image.naturalHeight / inSample).coerceAtLeast(1)

            val canvas = document.createElement("canvas") as HTMLCanvasElement
            canvas.width = targetWidth
            canvas.height = targetHeight
            val context = canvas.getContext("2d")?.asDynamic() ?: return null
            context.drawImage(image, 0, 0, targetWidth, targetHeight)
            PlatformBitmap(canvas)
        } catch (_: Throwable) {
            null
        } finally {
            js("URL.revokeObjectURL(url)")
        }
    }

    actual suspend fun applyExifRotation(source: ByteArray, bitmap: PlatformBitmap): PlatformBitmap {
        return bitmap
    }

    actual suspend fun encode(bitmap: PlatformBitmap, format: CompressionFormat, quality: Int): ByteArray? {
        val mime = when (format) {
            CompressionFormat.JPEG -> "image/jpeg"
            CompressionFormat.PNG -> "image/png"
            CompressionFormat.WEBP -> "image/webp"
        }

        val blob = bitmap.value.toBlobCompat(mime, quality.coerceIn(1, 100) / 100.0) ?: return null
        return blob.toByteArray()
    }

    actual fun recycle(bitmap: PlatformBitmap) {
        // No-op in browser.
    }

    private suspend fun loadImage(url: String): HTMLImageElement {
        return suspendCancellableCoroutine { continuation ->
            val image = document.createElement("img") as HTMLImageElement
            image.asDynamic().onload = {
                continuation.resume(image)
            }
            image.asDynamic().onerror = {
                continuation.resumeWithException(IllegalStateException("Cannot load image"))
            }
            image.src = url
        }
    }

    private suspend fun HTMLCanvasElement.toBlobCompat(type: String, quality: Double): Blob? {
        return suspendCancellableCoroutine { continuation ->
            this.toBlob(
                _callback = { blob -> continuation.resume(blob) },
                type = type,
                quality = quality,
            )
        }
    }

    private suspend fun Blob.toByteArray(): ByteArray {
        val arrayBuffer = asDynamic().arrayBuffer().unsafeCast<Promise<ArrayBuffer>>().await()
        val bytes = Uint8Array(arrayBuffer)
        return ByteArray(bytes.length) { index ->
            (bytes.asDynamic()[index] as Int).toByte()
        }
    }

    private fun ByteArray.toUint8Array(): Uint8Array {
        val array = Uint8Array(size)
        for (i in indices) {
            array.asDynamic()[i] = this[i].toInt() and 0xFF
        }
        return array
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
