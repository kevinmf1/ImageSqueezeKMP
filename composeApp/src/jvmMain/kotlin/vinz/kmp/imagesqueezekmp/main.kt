package vinz.kmp.imagesqueezekmp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ImageSqueezeKMP",
    ) {
        App()
    }
}