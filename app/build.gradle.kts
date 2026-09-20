plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseKeystorePath = providers.gradleProperty("woogit.release.keystore")
val releaseKeystorePassword = providers.gradleProperty("woogit.release.keystorePassword")
val releaseKeyAlias = providers.gradleProperty("woogit.release.keyAlias")
val releaseKeyPassword = providers.gradleProperty("woogit.release.keyPassword")

android {
    namespace="com.samanramezani1377.woogit"
    compileSdk=36
    defaultConfig {
        applicationId="com.samanramezani1377.woogit"
        minSdk=26
        targetSdk=36
        versionCode=1
        versionName="1.0.0"
        buildConfigField("String", "WOOGIT_BACKEND_BASE_URL", "\"https://woogit.ir\"")
    }

    flavorDimensions += "distribution"

    productFlavors {
        create("standard") {
            dimension = "distribution"
            buildConfigField("Boolean", "BAZAAR_BUILD", "false")
            buildConfigField("String", "BAZAAR_RSA_PUBLIC_KEY", "\"\"")
        }
        create("bazaar") {
            dimension = "distribution"
            buildConfigField("Boolean", "BAZAAR_BUILD", "true")
            val rsa = providers.gradleProperty("woogit.bazaar.rsaPublicKey").orNull ?: ""
            val escapedRsa = rsa.replace("\\", "\\\\").replace("\"", "\\\"")
            buildConfigField("String", "BAZAAR_RSA_PUBLIC_KEY", "\"$escapedRsa\"")
        }
    }

    signingConfigs {
        create("release") {
            if (releaseKeystorePath.isPresent && releaseKeystorePassword.isPresent && releaseKeyAlias.isPresent && releaseKeyPassword.isPresent) {
                storeFile = file(releaseKeystorePath.get())
                storePassword = releaseKeystorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures { compose=true; buildConfig=true }
}
kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":presentation"))
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode)

    bazaarImplementation(libs.poolakey)
}