plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.samanramezani1377.woogit.presentation"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }
}
