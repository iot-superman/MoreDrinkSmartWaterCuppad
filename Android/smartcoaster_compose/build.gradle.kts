plugins {
    // AGP 8.13.2 正式支援 API 36，並修正舊版 SDK XML 解析器及 Gradle 淘汰 API 問題。
    id("com.android.application") version "8.13.2" apply false

    // Kotlin Android 與 Compose Compiler 必須使用相同版本，避免編譯器版本不一致。
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
}
