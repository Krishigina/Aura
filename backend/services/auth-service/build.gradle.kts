plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "com.aura"
version = "1.0.0"

dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    
    // Exposed
    implementation("org.jetbrains.exposed:exposed-core:0.45.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.45.0")
    
    // PostgreSQL
    implementation("org.postgresql:postgresql:42.7.1")
    
    // JWT
    implementation("com.auth0:java-jwt:4.4.0")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

application {
    mainClass.set("com.aura.auth.AuthApplicationKt")
}

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin") {
    kotlinOptions {
        jvmTarget = "17"
    }
}