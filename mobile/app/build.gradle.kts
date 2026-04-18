plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.application")
    id("org.jetbrains.compose")
}

repositories { mavenCentral(); google() }

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(compose.ui)
                implementation(compose.material3)
                implementation("androidx.navigation:navigation-compose:2.7.6")
                implementation("io.ktor:ktor-client-core:2.3.7")
                implementation("io.ktor:ktor-client-okhttp:2.3.7")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
                implementation("io.insert-koin:koin-android:3.5.3")
                implementation("io.coil-kt:coil-compose:2.5.0")
                implementation("androidx.datastore:datastore-preferences:1.0.0")
            }
        }
    }
}

android {
    namespace = "com.aura.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.aura.app"; minSdk = 24; targetSdk = 34
        vectorDrawables { useSupportLibrary = true }
    }
    buildTypes { getByName("release") { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

dependencies { implementation(project(":shared")); implementation("androidx.core:core-ktx:1.12.0"); implementation("androidx.core:core-splashscreen:1.0.1"); implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2"); implementation("androidx.activity:activity-compose:1.8.2"); implementation("androidx.work:work-runtime-ktx:2.9.1"); testImplementation(kotlin("test-junit")) }
