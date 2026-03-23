package com.aura.user

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import org.jetbrains.exposed.sql.Database
import kotlinx.serialization.json.Json

fun main() {
    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/aura",
        user = "aura_user",
        password = "aura_password"
    )

    val profileRepository = UserProfileRepositoryImpl(database)
    val getProfileUseCase = GetProfileUseCase(profileRepository)
    val saveProfileUseCase = SaveProfileUseCase(profileRepository)

    embeddedServer(Netty, port = 8002) {
        install(ContentNegotiation) {
            json(Json { prettyPrint = true; isLenient = true })
        }
        routing {
            route("/profile") {
                get("/{userId}") {
                    val userId = call.parameters["userId"]!!
                    val profile = getProfileUseCase.execute(userId)
                    call.respond(profile!!)
                }
                post("/{userId}") {
                    val userId = call.parameters["userId"]!!
                    val request = call.receive<SaveProfileRequest>()
                    saveProfileUseCase.execute(userId, request)
                    call.respond(mapOf("success" to true))
                }
            }
        }
    }.start(wait = true)
}

@kotlinx.serialization.Serializable
data class SaveProfileRequest(
    val skinType: String,
    val ageRange: String? = null,
    val concerns: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val goals: List<String> = emptyList()
)

class UserProfileRepositoryImpl(private val database: Database) {
    // Implementation
}

class GetProfileUseCase(private val repository: UserProfileRepositoryImpl) {
    suspend fun execute(userId: String): SkinProfile? = null
}

class SaveProfileUseCase(private val repository: UserProfileRepositoryImpl) {
    suspend fun execute(userId: String, request: SaveProfileRequest) {}
}

data class SkinProfile(
    val id: String,
    val userId: String,
    val skinType: String
)