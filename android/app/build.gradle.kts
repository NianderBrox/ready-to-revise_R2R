plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.r2r.readytorevise"
    compileSdk = 37 // Updated to meet the requirement for core-ktx and lifecycle 2.11.0
    defaultConfig {
        applicationId = "com.r2r.readytorevise"
        minSdk = 26
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    buildFeatures {
        compose = true
    }
}
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

//dependencies {
////    implementation(platform(libs.androidx.compose.bom))
//    implementation(libs.androidx.activity.compose)
//    implementation(libs.androidx.compose.ui)
//    implementation(libs.androidx.compose.ui.graphics)
//    implementation(libs.androidx.compose.ui.tooling.preview)
//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.lifecycle.runtime.ktx)
//
////    testImplementation(libs.junit.junit)
//
//    androidTestImplementation(platform(libs.androidx.compose.bom))
//    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
//    implementation(libs.androidx.compose.material3)
//    androidTestImplementation(libs.androidx.espresso.core)
//    androidTestImplementation(libs.androidx.junit)
//    debugImplementation(libs.androidx.compose.ui.test.manifest)
//    debugImplementation(libs.androidx.compose.ui.tooling)
//    implementation(libs.androidx.navigation.compose)
//    implementation(libs.androidx.lifecycle.viewmodel.compose)
//    implementation(libs.androidx.lifecycle.runtime.compose)
//    implementation(libs.kotlinx.serialization.json)
//    implementation(libs.retrofit.core)
//    implementation(libs.retrofit.converter.kotlinx.serialization)
//    implementation(libs.okhttp.core)
//    implementation(libs.okhttp.logging.interceptor)
//
//    //room
//    implementation(libs.androidx.room.runtime)
//    implementation(libs.androidx.room.ktx)
//    ksp(libs.androidx.room.compiler)
//    implementation(libs.androidx.datastore.preferences)
//    implementation(libs.coil.compose)
//    implementation(libs.androidx.camera.core)
//
//    implementation(libs.androidx.camera.camera2)
//
//    implementation(libs.androidx.camera.lifecycle)
//
//    implementation(libs.androidx.camera.view)
//
//    implementation(libs.androidx.camera.compose)
//    implementation(platform(libs.firebase.bom))
//
//    val workVersion = "2.11.2" // Use the latest stable version
//
//        // For Kotlin + Coroutines support (Highly Recommended)
//    implementation(libs.androidx.work.runtime.ktx)
//    implementation(libs.google.firebase.messaging)
//    implementation(libs.androidx.compose.material.icons.extended)
//
////    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
//}


dependencies {
    // 1. The Compose BOMs (Declared exactly ONCE per configuration)
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    // 2. Core Compose & UI
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // 3. AndroidX & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    // 4. Data & Network
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)

    // 5. Media, Camera, & Firebase
    implementation(libs.coil.compose)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // 6. WorkManager (Make sure 'work' is in your TOML file!)
    implementation(libs.androidx.work.runtime.ktx)

    // 7. Room & KSP (Uncomment when you are ready to use them)
     implementation(libs.androidx.room.runtime)
     implementation(libs.androidx.room.ktx)
     ksp(libs.androidx.room.compiler)

    // 8. Icons (Requires manual versioning in TOML as discussed previously)
    implementation(libs.androidx.compose.material.icons.extended)

    // 9. Testing (Scrubbed of duplicates)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // 10. Debugging
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}