package com.aura.core.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileRoutineContractsSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun decodesDailyFrequencyTokenToEnum() {
        val decoded = json.decodeFromString<ReminderPreference>("""{"frequency":"daily"}""")

        assertEquals(ReminderFrequency.DAILY, decoded.frequency)
    }

    @Test
    fun omitsDefaultNoneFrequency() {
        val encoded = json.encodeToString(ReminderPreference(frequency = ReminderFrequency.NONE))

        assertEquals("""{}""", encoded)
    }

    @Test
    fun decodesProfileNotificationSettingsFields() {
        val payload =
            """{"disable_all":true,"routine":{"frequency":"weekly","reminder_time":"09:30"},"journal":{"frequency":"monthly","reminder_time":"21:15"}}"""

        val decoded = json.decodeFromString<ProfileNotificationSettings>(payload)

        assertEquals(true, decoded.disable_all)
        assertEquals(ReminderFrequency.WEEKLY, decoded.routine.frequency)
        assertEquals("09:30", decoded.routine.reminder_time)
        assertEquals(ReminderFrequency.MONTHLY, decoded.journal.frequency)
        assertEquals("21:15", decoded.journal.reminder_time)
    }
}
