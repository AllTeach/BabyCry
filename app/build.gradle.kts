plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.example.babycry"
    compileSdk = 34
    // Specify that the tflite file should not be compressed when building the APK package.


    defaultConfig {
        applicationId = "com.example.babycry"
        minSdk = 28
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        mlModelBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
   // implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.metadata)
    testImplementation(libs.junit)
    //implementation ("org.tensorflow:tensorflow-lite-task-audio:0.2.0")
    // Import the Audio Task Library dependency (NNAPI is included)
    implementation ("org.tensorflow:tensorflow-lite-task-audio:0.4.4")
    // Import the GPU delegate plugin Library for GPU inference
    implementation ("org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.4")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}