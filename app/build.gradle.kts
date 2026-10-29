plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.topz.karaoke"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.topz.karaoke"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // Para sa NDK (FluidSynth/Oboe). Piliin lang ang kailangan para mas maliit ang APK.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }

    // Native synth (TinySoundFont)
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // Sa GitHub Actions, gamitin ang NDK na naka-install na sa runner (iwas download).
    val ciNdk = System.getenv("ANDROID_NDK_LATEST_HOME")
    if (ciNdk != null) ndkVersion = File(ciNdk).name

    // Huwag i-compress ang SoundFont sa APK para mabilis kopyahin.
    androidResources {
        noCompress += listOf("sf2")
    }
}

dependencies {
    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Room (song library)
    val room = "2.6.1"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    // Para sa pagpili ng folder (Storage Access Framework)
    implementation("androidx.documentfile:documentfile:1.0.1")

    // Video player para sa background video
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Tests
    testImplementation("junit:junit:4.13.2")
}

// ---- Auto-download ng mga third-party file (hindi isinasama sa repo) ----
val tsfHeader = layout.projectDirectory.file("src/main/cpp/tsf.h").asFile
val defaultSoundFont = layout.projectDirectory.file("src/main/assets/default.sf2").asFile

fun download(url: String, target: File) {
    target.parentFile.mkdirs()
    val part = File(target.path + ".part")
    java.net.URI(url).toURL().openStream().use { input ->
        part.outputStream().use { out -> input.copyTo(out) }
    }
    part.renameTo(target)
}

// TinySoundFont (MIT): isang header file
val fetchTsf by tasks.registering {
    onlyIf { !tsfHeader.exists() }
    doLast { download("https://raw.githubusercontent.com/schellingb/TinySoundFont/main/tsf.h", tsfHeader) }
}

// GeneralUser GS (default SoundFont). Kung pumalya ang download, tuloy pa rin ang build.
val fetchDefaultSoundFont by tasks.registering {
    onlyIf { !defaultSoundFont.exists() }
    doLast {
        try {
            download("https://github.com/mrbumpy409/GeneralUser-GS/raw/refs/heads/main/GeneralUser-GS.sf2", defaultSoundFont)
        } catch (e: Exception) {
            logger.warn("Hindi na-download ang default SoundFont (${e.message}). Itutuloy ang build na wala nito.")
        }
    }
}

tasks.configureEach {
    if (name == "preBuild") dependsOn(fetchTsf, fetchDefaultSoundFont)
    if (name.startsWith("configureCMake") || name.startsWith("buildCMake")) dependsOn(fetchTsf)
}
