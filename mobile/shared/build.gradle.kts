plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("org.jetbrains.compose")
}

repositories { mavenCentral(); google() }

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.material3)
                implementation("androidx.navigation:navigation-compose:2.7.6")
                implementation("androidx.compose.material:material-icons-extended:1.5.4")
                implementation("io.ktor:ktor-client-core:2.3.7")
                implementation("io.ktor:ktor-client-okhttp:2.3.7")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
                implementation("io.insert-koin:koin-core:3.5.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
                implementation("androidx.datastore:datastore-preferences:1.0.0")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("io.insert-koin:koin-android:3.5.3")
                implementation("io.coil-kt:coil-compose:2.5.0")
            }
        }
    }
}

android {
    namespace = "com.aura.core"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
