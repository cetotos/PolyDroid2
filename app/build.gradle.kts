plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

tasks.named("preBuild") {
    doFirst {
        val required = listOf(
            "src/main/jniLibs/arm64-v8a/libbox64.so",
            "src/main/jniLibs/arm64-v8a/libXlorie.so",
            "src/main/assets/glibc-x86_64/libX11.so.6",
            "src/main/assets/glibc-x86_64/libpulse.so.0",
            "src/main/assets/glibc-x86_64/libpulse-simple.so.0",
        )
        for (path in required) {
            if (!file(path).exists()) error("Missing pre-built: $path")
        }
    }
}

android {
    namespace = "com.cetotos.polydroid2"
    compileSdk { version = release(36) }

    defaultConfig {
        applicationId = "com.cetotos.polydroid2"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-beta"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk { abiFilters += "arm64-v8a" }
    }

    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt") } }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    androidResources { noCompress += listOf("tar.gz", "tar.xz", "txz") }
    buildFeatures { aidl = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
    kotlinOptions { jvmTarget = "11" }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation("androidx.browser:browser:1.8.0")
    implementation("org.apache.commons:commons-compress:1.27.1")
    implementation("org.tukaani:xz:1.10")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
