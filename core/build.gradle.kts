plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    androidTarget()

    sourceSets {
        commonMain.dependencies {
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.samanramezani1377.woogit.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }
}
