// Pinakasimpleng app: walang Kotlin, walang Compose, walang kahit anong library.
plugins {
    id("com.android.application")
}

android {
    namespace = "com.topz.karaoke.diag"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.topz.karaoke.diag"   // iba sa main app, kaya puwedeng magkasabay
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "diag-1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
