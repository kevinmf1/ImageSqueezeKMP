package vinz.kmp.imagesqueezekmp.squeeze

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageSqueeze {
    private const val DISPLAY_TARGET_SIZE = 100

    suspend fun compress(
        source: ByteArray,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        configBlock: CompressionConfig.() -> Unit = {},
    ): SqueezeResult = withContext(dispatcher) {
        compressInternal(source, configBlock)
    }

    suspend fun compress(
        source: ByteArray,
        config: CompressionConfig,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): SqueezeResult = withContext(dispatcher) {
        compressInternal(source) {
            targetWidth = config.targetWidth
            targetHeight = config.targetHeight
            maxSizeInBytes = config.maxSizeInBytes
            minQuality = config.minQuality
            defaultQuality = config.defaultQuality
            compressFormat = config.compressFormat
            isForDisplay = config.isForDisplay
        }
    }

    private suspend fun compressInternal(
        source: ByteArray,
        configBlock: CompressionConfig.() -> Unit,
    ): SqueezeResult {
        if (source.isEmpty()) {
            return SqueezeResult.Error(
                errorType = SqueezeError.FILE_EMPTY,
                message = "Source image is empty.",
            )
        }

        val config = CompressionConfig().apply(configBlock)
        if (config.targetWidth <= 0 || config.targetHeight <= 0 || config.maxSizeInBytes <= 0) {
            return SqueezeResult.Error(
                errorType = SqueezeError.INVALID_CONFIG,
                message = "Compression config is invalid. Width/height/maxSize must be > 0.",
            )
        }

        val (reqWidth, reqHeight) = if (config.isForDisplay) {
            DISPLAY_TARGET_SIZE to DISPLAY_TARGET_SIZE
        } else {
            config.targetWidth to config.targetHeight
        }

        val bitmap = try {
            PlatformImageCodec.decodeSampled(source, reqWidth, reqHeight)
        } catch (t: Throwable) {
            return SqueezeResult.Error(
                errorType = SqueezeError.DECODE_FAILED,
                exception = t,
                message = "Failed to decode image.",
            )
        } ?: return SqueezeResult.Error(
            errorType = SqueezeError.DECODE_FAILED,
            message = "Image is unsupported or corrupted.",
        )

        val rotatedBitmap = try {
            PlatformImageCodec.applyExifRotation(source, bitmap)
        } catch (_: Throwable) {
            bitmap
        }

        var quality = config.defaultQuality.coerceIn(1, 100)
        val minQuality = config.minQuality.coerceIn(1, 100)

        try {
            while (true) {
                val encoded = PlatformImageCodec.encode(rotatedBitmap, config.compressFormat, quality)
                    ?: return SqueezeResult.Error(
                        errorType = when (config.compressFormat) {
                            CompressionFormat.WEBP -> SqueezeError.UNSUPPORTED_FORMAT
                            else -> SqueezeError.ENCODE_FAILED
                        },
                        message = "Failed to encode compressed image.",
                    )

                if (encoded.size.toLong() <= config.maxSizeInBytes || quality <= minQuality) {
                    return SqueezeResult.Success(
                        bytes = encoded,
                        finalQuality = quality,
                        originalSize = source.size.toLong(),
                        compressedSize = encoded.size.toLong(),
                    )
                }

                quality = (quality - 10).coerceAtLeast(minQuality)
            }
        } catch (t: Throwable) {
            return SqueezeResult.Error(
                errorType = SqueezeError.UNKNOWN,
                exception = t,
                message = "Unexpected error during compression.",
            )
        } finally {
            PlatformImageCodec.recycle(rotatedBitmap)
            if (rotatedBitmap !== bitmap) {
                PlatformImageCodec.recycle(bitmap)
            }
        }
    }
}
