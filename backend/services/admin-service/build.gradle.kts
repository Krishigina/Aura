plugins { kotlin("jvm"); kotlin("plugin.serialization") }
group = "com.aura"; version = "1.0.0"
dependencies {
    implementation("io.ktor:ktor-server-core:2.3.7"); implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7"); implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("org.jetbrains.exposed:exposed-core:0.45.0"); implementation("org.jetbrains.exposed:exposed-jdbc:0.45.0")
    implementation("org.postgresql:postgresql:42.7.1")
}
application { mainClass.set("com.aura.admin.AdminApplicationKt") }
tasks.named("compileKotlin") { kotlinOptions { jvmTarget = "17" } }