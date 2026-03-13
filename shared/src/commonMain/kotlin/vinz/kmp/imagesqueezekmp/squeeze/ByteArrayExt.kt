package vinz.kmp.imagesqueezekmp.squeeze

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

suspend fun ByteArray.squeeze(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    configBlock: CompressionConfig.() -> Unit = {},
): SqueezeResult {
    return ImageSqueeze.compress(this, dispatcher, configBlock)
}
