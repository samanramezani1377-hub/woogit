plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android { namespace="com.samanramezani1377.woogit"; compileSdk=36; defaultConfig { applicationId="com.samanramezani1377.woogit"; minSdk=26; targetSdk=36; versionCode=1; versionName="1.0.0" }; buildFeatures { compose=true } }
kotlin { jvmToolchain(17) }
dependencies {
 implementation(project(":core")); implementation(project(":data")); implementation(project(":presentation"))
 implementation(libs.androidx.work.runtime.ktx); implementation(libs.androidx.core.ktx)
 implementation(platform(libs.androidx.compose.bom)); implementation(libs.androidx.compose.ui); implementation(libs.androidx.compose.material3); implementation(libs.androidx.activity.compose)
}
