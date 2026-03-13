package vinz.kmp.imagesqueezekmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform