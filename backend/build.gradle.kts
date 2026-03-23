plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

allprojects {
    group = "com.aura"
    version = "1.0.0"
    
    repositories {
        google()
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.github.johnrengelman.shadow")
    
    dependencies {
        implementation(kotlin("stdlib"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
        
        // Ktor
        implementation("io.ktor:ktor-server-core:2.3.7")
        implementation("io.ktor:ktor-server-netty:2.3.7")
        implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
        implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
        implementation("io.ktor:ktor-client-core:2.3.7")
        implementation("io.ktor:ktor-client-okhttp:2.3.7")
        
        // Exposed
        implementation("org.jetbrains.exposed:exposed-core:0.45.0")
        implementation("org.jetbrains.exposed:exposed-jdbc:0.45.0")
        
        // PostgreSQL
        implementation("org.postgresql:postgresql:42.7.1")
        
        // JWT
        implementation("com.auth0:java-jwt:4.4.0")
        
        // Logging
        implementation("ch.qos.logback:logback-classic:1.4.14")
        
        // Testing
        testImplementation("io.ktor:ktor-server-tests:2.3.7")
        testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    }
    
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
    
    tasks.named<com.github.johnrengelman.shadow.tasks.ShadowJar>("shadowJar") {
        manifest {
            attributes(
                "Main-Class" to "com.aura.auth.AuthApplicationKt"
            )
        }
    }
}