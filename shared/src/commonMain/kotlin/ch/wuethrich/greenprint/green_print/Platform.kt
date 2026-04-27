package ch.wuethrich.greenprint.green_print

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform