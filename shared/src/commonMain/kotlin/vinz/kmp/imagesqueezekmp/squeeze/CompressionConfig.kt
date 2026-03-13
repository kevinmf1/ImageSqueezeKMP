package vinz.kmp.imagesqueezekmp.squeeze

class CompressionConfig {
    var targetWidth: Int = 612
    var targetHeight: Int = 816
    var maxSizeInBytes: Long = 1_000_000L
    var minQuality: Int = 10
    var defaultQuality: Int = 80
    var compressFormat: CompressionFormat = CompressionFormat.JPEG
    var isForDisplay: Boolean = false

    fun resolution(width: Int, height: Int) {
        this.targetWidth = width
        this.targetHeight = height
    }

    fun format(format: CompressionFormat) {
        this.compressFormat = format
    }

    fun size(maxFileSizeInBytes: Long) {
        this.maxSizeInBytes = maxFileSizeInBytes
    }

    fun quality(quality: Int) {
        this.defaultQuality = quality
    }

    fun minQuality(quality: Int) {
        this.minQuality = quality
    }

    fun forDisplay(enabled: Boolean = true) {
        isForDisplay = enabled
    }
}
