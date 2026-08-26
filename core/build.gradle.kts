plugins {
    id("org.jetbrains.kotlin.multiplatform")
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
