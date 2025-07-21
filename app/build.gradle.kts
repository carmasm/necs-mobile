plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.firebase.appdistribution)  // Use version catalog reference
    alias(libs.plugins.google.services)           // Use version catalog reference
}

android {
    namespace = "com.itanddev.necsmobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.itanddev.necsmobile"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.18"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Ensure debug builds are signed too
            signingConfig = signingConfigs.getByName("debug")
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
}

firebaseAppDistribution {
    appId = "your-firebase-app-id"
    serviceCredentialsFile = "firebase-service-account.json" // Path to your service account file
    groups = "testers"
//        releaseNotesFile = "release-notes.txt"
}

dependencies {

//    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation(fileTree("libs") { include("*.jar") })

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.appdistribution.api)
    implementation(libs.firebase.appdistribution)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.androidx.security.crypto)

//    implementation (com.google.firebase:firebase-appdistribution:16.0.0-beta10)
//    id("com.google.firebase.appdistribution") version "5.1.1" apply false

//    implementation("com.google.firebase:firebase-appdistribution-api-ktx:16.0.0-beta15")

//    implementation(libs.androidx.lifecycle.runtime.ktx)

//    implementation("com.squareup.retrofit2:retrofit:2.9.0")
//    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
//    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
}

//apply(plugin = "com.google.gms.google-services")
//apply(plugin = "com.google.firebase.appdistribution")

