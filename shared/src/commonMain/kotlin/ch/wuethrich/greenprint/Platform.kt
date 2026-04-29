package ch.wuethrich.greenprint

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform