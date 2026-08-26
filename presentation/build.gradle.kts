plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android { namespace = "com.samanramezani1377.woogit.presentation"; compileSdk = 36; defaultConfig { minSdk = 26 }; buildFeatures { compose = true } }

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.2")
}
