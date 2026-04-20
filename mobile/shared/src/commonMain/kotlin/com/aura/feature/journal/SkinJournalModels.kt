package com.aura.feature.journal

data class JournalZone(val id: String, val label: String)

data class VisibleJournalSearchOptions<T>(
    val items: List<T>,
    val hasMore: Boolean,
)

val procedureFaceZones = listOf(
    JournalZone("forehead", "Лоб"),
    JournalZone("glabella", "Межбровье"),
    JournalZone("temples", "Виски"),
    JournalZone("periorbital", "Периорбитальная зона"),
    JournalZone("nose", "Нос"),
    JournalZone("nasolabial", "Носогубные складки"),
    JournalZone("cheeks", "Щеки"),
    JournalZone("cheekbones", "Скулы"),
    JournalZone("lips", "Губы"),
    JournalZone("chin", "Подбородок"),
    JournalZone("jawline", "Овал лица"),
    JournalZone("neck", "Шея"),
)

val sensorFaceZones = listOf(
    JournalZone("forehead", "Лоб"),
    JournalZone("nose", "Нос"),
    JournalZone("left_cheek", "Левая щека"),
    JournalZone("right_cheek", "Правая щека"),
    JournalZone("chin", "Подбородок"),
    JournalZone("around_eyes", "Вокруг глаз"),
)

fun journalZoneLabel(zoneId: String): String = (procedureFaceZones + sensorFaceZones)
    .firstOrNull { it.id == zoneId }
    ?.label
    ?: zoneId

fun List<JournalZone>.filterBySearch(query: String): List<JournalZone> {
    val normalized = query.trim().lowercase()
    if (normalized.isEmpty()) return this
    return filter { it.id.lowercase().contains(normalized) || it.label.lowercase().contains(normalized) }
}

fun <T> visibleJournalSearchOptions(options: List<T>, limit: Int = 8): VisibleJournalSearchOptions<T> {
    val safeLimit = limit.coerceAtLeast(0)
    return VisibleJournalSearchOptions(
        items = options.take(safeLimit),
        hasMore = options.size > safeLimit,
    )
}
