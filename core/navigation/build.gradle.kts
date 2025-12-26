
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "android.template.core.navigation"
    compileSdk = 36 // Should match app's compileSdk

    defaultConfig {
        minSdk = 21 // Should match app's minSdk
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Nav3 runtime API
    api(libs.androidx.navigation3.runtime)
}
