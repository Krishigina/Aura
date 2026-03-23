plugins {
    kotlin("multiplatform"); kotlin("plugin.serialization") version "1.9.22"
    id("com.android.application") version "8.2.0"
    id("org.jetbrains.compose") version "1.5.10"
}

repositories { mavenCentral(); google() }

kotlin {
    androidTarget { compilations.all { kotlinOptions { jvmTarget = "17" } } }
    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(compose.ui); implementation(compose.material3); implementation(compose.navigation)
                implementation("io.ktor:ktor-client-core:2.3.7"); implementation("io.ktor:ktor-client-okhttp:2.3.7")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7"); implementation("io.insert-koin:koin-android:3.5.3")
                implementation("io.coil-kt:coil-compose:2.5.0"); implementation("androidx.datastore:datastore-preferences:1.0.0")
            }
        }
    }
}

android {
    compileSdk = 34
    defaultConfig {
        applicationId = "com.aura.app"; minSdk = 24; targetSdk = 34
        vectorDrawables { useSupportLibrary = true }
    }
    buildTypes { getByName("release") { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

dependencies { implementation("androidx.core:core-ktx:1.12.0"); implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2") }