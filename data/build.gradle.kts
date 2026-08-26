plugins {
    id("com.android.library")
}

android {
    namespace = "com.samanramezani1377.woogit.data"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
}

dependencies {
    implementation(project(":core"))
}
