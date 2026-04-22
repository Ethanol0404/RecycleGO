plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "my.edu.utar.RecycleGO"
    compileSdk = 36

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

    // Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

    // Add the dependencies for Auth and Firestore
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")
}