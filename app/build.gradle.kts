plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.itanddev.necsmobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.itanddev.necsmobile"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.21"
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

dependencies {

//    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation(fileTree("libs") { include("*.jar") })

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.androidx.security.crypto)

//    implementation(libs.androidx.lifecycle.runtime.ktx)

//    implementation("com.squareup.retrofit2:retrofit:2.9.0")
//    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
//    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
}