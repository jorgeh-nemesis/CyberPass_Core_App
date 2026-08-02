import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// 1. Load keystore properties from app/ directory
val keystoreProperties = Properties()
val keystorePropertiesFile = file("keystore.properties")

if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { stream ->
        keystoreProperties.load(stream)
    }
}

android {
    namespace = "com.cybercastle.cyberpass"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.cybercastle.cyberpass"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val ksFile = file("release.keystore")
            val storePass = keystoreProperties.getProperty("RELEASE_STORE_PASSWORD")
            val alias = keystoreProperties.getProperty("RELEASE_KEY_ALIAS")
            val keyPass = keystoreProperties.getProperty("RELEASE_KEY_PASSWORD")

            // Hard validation: Fail the build immediately if credentials or keystore are missing
            if (!keystorePropertiesFile.exists()) {
                throw GradleException("Signing Error: 'app/keystore.properties' does not exist in worktree.")
            }
            if (!ksFile.exists()) {
                throw GradleException("Signing Error: 'app/release.keystore' does not exist in ${ksFile.absolutePath}")
            }
            if (storePass.isNullOrEmpty() || alias.isNullOrEmpty() || keyPass.isNullOrEmpty()) {
                throw GradleException("Signing Error: One or more RELEASE_* keys in keystore.properties are missing or empty.")
            }

            storeFile = ksFile
            storePassword = storePass
            keyAlias = alias
            keyPassword = keyPass
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)
    implementation(libs.zxing.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.google.gson)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.reorderable)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}