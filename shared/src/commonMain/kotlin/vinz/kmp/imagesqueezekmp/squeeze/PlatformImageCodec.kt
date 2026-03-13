package vinz.kmp.imagesqueezekmp.squeeze

internal expect class PlatformBitmap

internal expect object PlatformImageCodec {
    suspend fun decodeSampled(source: ByteArray, reqWidth: Int, reqHeight: Int): PlatformBitmap?
    suspend fun applyExifRotation(source: ByteArray, bitmap: PlatformBitmap): PlatformBitmap
    suspend fun encode(bitmap: PlatformBitmap, format: CompressionFormat, quality: Int): ByteArray?
    fun recycle(bitmap: PlatformBitmap)
}
