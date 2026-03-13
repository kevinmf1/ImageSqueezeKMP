package vinz.kmp.imagesqueezekmp.squeeze

sealed class SqueezeResult {
    data class Success(
        val bytes: ByteArray,
        val finalQuality: Int,
        val originalSize: Long,
        val compressedSize: Long,
    ) : SqueezeResult()

    data class Error(
        val errorType: SqueezeError,
        val exception: Throwable? = null,
        val message: String,
    ) : SqueezeResult()
}
