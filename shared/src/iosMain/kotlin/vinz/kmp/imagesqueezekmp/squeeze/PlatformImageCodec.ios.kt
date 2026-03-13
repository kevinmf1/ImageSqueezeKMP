package vinz.kmp.imagesqueezekmp.squeeze

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.getBytes
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.drawInRect

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal actual class PlatformBitmap(val value: UIImage)

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal actual object PlatformImageCodec {
    actual suspend fun decodeSampled(source: ByteArray, reqWidth: Int, reqHeight: Int): PlatformBitmap? {
        val image = UIImage(data = source.toNSData()) ?: return null
        val (width, height) = image.size.useContents { width to height }

        if (width <= reqWidth && height <= reqHeight) {
            return PlatformBitmap(image)
        }

        val scale = minOf(reqWidth / width, reqHeight / height)
        val targetWidth = (width * scale).coerceAtLeast(1.0)
        val targetHeight = (height * scale).coerceAtLeast(1.0)

        UIGraphicsBeginImageContextWithOptions(
            size = CGSizeMake(targetWidth, targetHeight),
            opaque = false,
            scale = 1.0,
        )
        image.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
        val resized = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        return PlatformBitmap(resized ?: image)
    }

    actual suspend fun applyExifRotation(source: ByteArray, bitmap: PlatformBitmap): PlatformBitmap {
        // UIImage decode + redraw in decodeSampled already normalizes most orientation metadata.
        return bitmap
    }

    actual suspend fun encode(bitmap: PlatformBitmap, format: CompressionFormat, quality: Int): ByteArray? {
        val data = when (format) {
            CompressionFormat.JPEG -> UIImageJPEGRepresentation(bitmap.value, quality.coerceIn(1, 100) / 100.0)
            CompressionFormat.PNG -> UIImagePNGRepresentation(bitmap.value)
            CompressionFormat.WEBP -> null
        } ?: return null

        return data.toByteArray()
    }

    actual fun recycle(bitmap: PlatformBitmap) {
        // No-op on iOS; ARC handles memory lifecycle.
    }

    private fun ByteArray.toNSData(): NSData {
        return usePinned {
            NSData.create(bytes = it.addressOf(0), length = size.toULong())
        }
    }

    private fun NSData.toByteArray(): ByteArray {
        val lengthInt = length.toInt()
        if (lengthInt == 0) return ByteArray(0)
        return ByteArray(lengthInt).apply {
            usePinned {
                this@toByteArray.getBytes(it.addressOf(0), length)
            }
        }
    }
}
