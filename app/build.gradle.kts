plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("kotlin-parcelize")
}

android {
    namespace = "com.pkm.pinme"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pkm.pinme"
        minSdk = 26
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

dependencies {
    implementation (libs.obj)
    implementation (libs.glide)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.core)
    implementation (libs.popup.dialog)
    implementation (libs.code.scanner)
    implementation (libs.lottie)
    implementation (libs.sceneform)
    implementation (libs.filamat.android)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx.v262)
    implementation(libs.logging.interceptor)

    androidTestImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.coroutines.test)

    implementation (libs.androidx.lifecycle.runtime.ktx)
    implementation (libs.androidx.lifecycle.viewmodel.ktx)
    implementation (libs.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}