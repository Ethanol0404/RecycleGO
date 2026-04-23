plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "my.edu.utar.RecycleGO"
    compileSdk = 35

    defaultConfig {
        applicationId = "my.edu.utar.RecycleGO"
        minSdk = 24
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // Map & Location
    implementation(libs.osmdroid)
    implementation(libs.osmbonuspack)
    implementation(libs.play.services.location)

    implementation("androidx.core:core-splashscreen:1.2.0")

    //firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")

    // Gemini AI - Corrected to 0.9.0 (latest stable) for Gemini 1.5 support
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("com.google.guava:guava:33.3.0-android")
    implementation("org.reactivestreams:reactive-streams:1.0.4")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    //asset
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.glide)
    implementation(libs.circleimageview)
}
