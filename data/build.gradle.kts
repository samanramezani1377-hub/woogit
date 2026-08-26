plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("app.cash.sqldelight")
}

android {
    namespace = "com.samanramezani1377.woogit.data"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
}

dependencies {
    implementation(project(":core"))
    implementation("app.cash.sqldelight:android-driver:2.1.0")
}

sqldelight {
    databases {
        create("WooGitDatabase") {
            packageName.set("com.samanramezani1377.woogit.data.db")
        }
    }
}
