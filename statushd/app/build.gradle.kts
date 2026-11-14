plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.statushd.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.statushd.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Debug: tanpa minify supaya senang uji di telefon.
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        // java.time pada minSdk 24 memerlukan desugaring.
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // FFmpegKit ships native libs for all ABIs; keep them all so the
        // app runs on any device (APK is large — see build-log.md).
        jniLibs {
            useLegacyPackaging = false
        }
    }

    // 16 KB page size (Android 16 / API 36): FFmpegKit native libs are
    // built 16 KB-aligned upstream (ffmpeg-kit 6.0); no action needed here.
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        // Amaran API eksperimen Material 3 — kekalkan sebagai amaran,
        // bukan ralat, supaya build tidak gagal atas API baharu.
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Jetpack Compose + Material 3 (versi diurus oleh BOM).
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Data tempatan: Room (rekod kerja) + DataStore (tetapan).
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    // Mampatan video atas peranti: FFmpegKit (full — libavcodec/format/
    // filter/util, swscale, swresample, avdevice; sama seperti original).
    implementation(libs.ffmpeg.kit.full)

    implementation(libs.kotlinx.coroutines.android)
}
