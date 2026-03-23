package com.aura.tracker
import io.ktor.server.application.*, io.ktor.server.engine.*, io.ktor.server.netty.*, io.ktor.server.routing.*, io.ktor.server.plugins.contentnegotiation.*, io.ktor.serialization.kotlinx.json.*, org.jetbrains.exposed.sql.Database, kotlinx.serialization.json.Json

fun main() {
    val database = Database.connect("jdbc:postgresql://localhost:5432/aura", "aura_user", "aura_password")
    val logUsage = LogUsageUseCase(TrackerRepositoryImpl(database))
    val getHistory = GetHistoryUseCase(TrackerRepositoryImpl(database))
    val getReminders = GetRemindersUseCase(ReminderRepositoryImpl(database))
    val createReminder = CreateReminderUseCase(ReminderRepositoryImpl(database))

    embeddedServer(Netty, port = 8005) {
        install(ContentNegotiation) { json(Json { prettyPrint = true }) }
        routing {
            post("/tracker/{userId}/log") {
                val userId = call.parameters["userId"]!!
                val req = call.receive<LogUsageRequest>()
                logUsage.execute(userId, req.productId, req.action)
                call.respond(mapOf("success" to true))
            }
            get("/tracker/{userId}/history") {
                val userId = call.parameters["userId"]!!
                call.respond(getHistory.execute(userId))
            }
            get("/reminders/{userId}") { val userId = call.parameters["userId"]!!; call.respond(getReminders.execute(userId)) }
            post("/reminders/{userId}") {
                val userId = call.parameters["userId"]!!
                val req = call.receive<CreateReminderRequest>()
                createReminder.execute(userId, req.title, req.scheduledAt)
                call.respond(mapOf("success" to true))
            }
        }
    }.start(wait = true)
}

@kotlinx.serialization.Serializable data class LogUsageRequest(val productId: String?, val action: String)
@kotlinx.serialization.Serializable data class CreateReminderRequest(val title: String, val scheduledAt: String)

class LogUsageUseCase(private val repo: TrackerRepository) { suspend fun execute(userId: String, productId: String?, action: String) {} }
class GetHistoryUseCase(private val repo: TrackerRepository) { suspend fun execute(userId: String) = HistoryResult(emptyList(), 0) }
class GetRemindersUseCase(private val repo: ReminderRepository) { suspend fun execute(userId: String) = emptyList<Reminder>() }
class CreateReminderUseCase(private val repo: ReminderRepository) { suspend fun execute(userId: String, title: String, scheduledAt: String) {} }

interface TrackerRepository { suspend fun save(log: TrackerLog) }
class TrackerRepositoryImpl(private val db: Database) : TrackerRepository { suspend fun save(log: TrackerLog) {} }
interface ReminderRepository { suspend fun findByUserId(userId: String): List<Reminder>; suspend fun save(reminder: Reminder) }
class ReminderRepositoryImpl(private val db: Database) : ReminderRepository { suspend fun findByUserId(userId: String) = emptyList(); suspend fun save(reminder: Reminder) {} }

data class TrackerLog(val id: String, val userId: String, val productId: String?, val action: String, val loggedAt: String)
data class Reminder(val id: String, val userId: String, val title: String, val scheduledAt: String, val isActive: Boolean)
data class HistoryResult(val logs: List<TrackerLog>, val total: Int)