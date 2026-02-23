plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.coinlet"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.coinlet"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }
    packaging {
        resources {
            pickFirsts += setOf(
                "META-INF/native-image/**",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.androidx.material3)

    implementation(libs.androidx.cardview)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("androidx.biometric:biometric:1.1.0")

    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-firestore")

//    implementation("org.opencv:opencv:4.13.0")
    implementation("org.bytedeco:javacv:1.5.12")

    // Xiaomi = arm64, więc bierzemy tylko android-arm64
    implementation("org.bytedeco:javacpp:1.5.12:android-arm64")
    implementation("org.bytedeco:opencv:4.11.0-1.5.12:android-arm64")

    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    implementation("com.google.guava:guava:32.1.3-android")
    implementation("org.bytedeco:openblas:0.3.28-1.5.12:android-arm64")

    implementation("com.google.mlkit:face-detection:16.1.6")
    implementation("org.tensorflow:tensorflow-lite:2.14.0")

    debugImplementation("org.bytedeco:javacpp:1.5.12:android-x86_64")
    debugImplementation("org.bytedeco:opencv:4.11.0-1.5.12:android-x86_64")
    debugImplementation("org.bytedeco:openblas:0.3.28-1.5.12:android-x86_64")

    debugImplementation("org.bytedeco:javacpp:1.5.12:android-x86_64")
    debugImplementation("org.bytedeco:opencv:4.11.0-1.5.12:android-x86_64")
    debugImplementation("org.bytedeco:openblas:0.3.28-1.5.12:android-x86_64")

}
