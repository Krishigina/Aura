package com.aura.feature.journal

import com.aura.core.domain.model.SkinJournalProcedureEntry
import com.aura.core.domain.model.SkinJournalReminder
import com.aura.core.domain.model.SkinJournalSensorReading
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SkinJournalLogicTest {
    @Test
    fun moisturePercentInterpretationBoundaries() {
        assertEquals("Очень сухая", moisturePercentInterpretation(33))
        assertEquals("Сухая", moisturePercentInterpretation(34))
        assertEquals("Сухая", moisturePercentInterpretation(37))
        assertEquals("Нормальная", moisturePercentInterpretation(38))
        assertEquals("Нормальная", moisturePercentInterpretation(42))
        assertEquals("Влажная", moisturePercentInterpretation(43))
        assertEquals("Влажная", moisturePercentInterpretation(46))
        assertEquals("Очень влажная", moisturePercentInterpretation(47))
    }

    @Test
    fun oilinessScaleInterpretation_allLevels() {
        assertEquals("Очень сухая", oilinessScaleInterpretation(0))
        assertEquals("Очень сухая", oilinessScaleInterpretation(1))
        assertEquals("Сухая", oilinessScaleInterpretation(2))
        assertEquals("Нормальная", oilinessScaleInterpretation(3))
        assertEquals("Жирная", oilinessScaleInterpretation(4))
        assertEquals("Очень жирная", oilinessScaleInterpretation(5))
        assertEquals("Очень жирная", oilinessScaleInterpretation(6))
    }

    @Test
    fun softnessScaleInterpretation_allLevels() {
        assertEquals("Очень шершавая", softnessScaleInterpretation(0))
        assertEquals("Очень шершавая", softnessScaleInterpretation(1))
        assertEquals("Шершавая", softnessScaleInterpretation(2))
        assertEquals("Нормальная", softnessScaleInterpretation(3))
        assertEquals("Мягкая", softnessScaleInterpretation(4))
        assertEquals("Очень мягкая", softnessScaleInterpretation(5))
        assertEquals("Очень мягкая", softnessScaleInterpretation(6))
    }

    @Test
    fun hydrationScaleInterpretation_allLevels() {
        assertEquals("Очень сухая", hydrationScaleInterpretation(0))
        assertEquals("Очень сухая", hydrationScaleInterpretation(1))
        assertEquals("Сухая", hydrationScaleInterpretation(2))
        assertEquals("Нормальная", hydrationScaleInterpretation(3))
        assertEquals("Влажная", hydrationScaleInterpretation(4))
        assertEquals("Очень влажная", hydrationScaleInterpretation(5))
        assertEquals("Очень влажная", hydrationScaleInterpretation(6))
    }

    @Test
    fun activeSkinJournalRemindersUsesTodayFiltering() {
        val reminders = listOf(
            SkinJournalReminder(id = "overdue", title = "Overdue", due_at = "2000-01-01T12:00:00Z", status = "planned"),
            SkinJournalReminder(id = "future", title = "Future", due_at = "2099-01-01T12:00:00Z", status = "rescheduled"),
        )

        val active = activeSkinJournalReminders(reminders)

        assertEquals(listOf("overdue"), active.map { it.id })
    }

    @Test
    fun activeTodayRemindersKeepsOnlyActiveDueTodayOrOverdue() {
        val now = Instant.parse("2026-04-29T12:00:00Z")
        val reminders = listOf(
            SkinJournalReminder(id = "overdue", title = "Overdue", due_at = "2026-04-27T12:00:00Z", status = "planned"),
            SkinJournalReminder(id = "today", title = "Today", due_at = "2026-04-29T12:00:00Z", status = "rescheduled"),
            SkinJournalReminder(id = "future", title = "Future", due_at = "2026-05-01T12:00:00Z", status = "planned"),
            SkinJournalReminder(id = "done", title = "Done", due_at = "2026-04-27T12:00:00Z", status = "done"),
            SkinJournalReminder(id = "invalid", title = "Invalid", due_at = "not-a-date", status = "planned"),
        )

        val active = activeTodayReminders(reminders, now)

        assertEquals(listOf("overdue", "today"), active.map { it.id })
    }

    @Test
    fun skinJournalTodayStatusReflectsActiveReminders() {
        assertEquals("Сегодня все спокойно", skinJournalTodayStatus(emptyList()))

        val oneReminder = listOf(SkinJournalReminder(id = "one", title = "SPF", status = "planned"))
        assertEquals("SPF", skinJournalTodayStatus(oneReminder))

        val manyReminders = listOf(
            SkinJournalReminder(id = "one", title = "SPF", status = "planned"),
            SkinJournalReminder(id = "two", title = "Повтор", status = "rescheduled"),
        )
        assertEquals("Активных напоминаний: 2", skinJournalTodayStatus(manyReminders))
    }

    @Test
    fun futureJournalTimestampsAreIsoInstants() {
        assertTrue(tomorrowJournalTimestamp().contains("T"))
        assertTrue(sixMonthsJournalTimestamp().contains("T"))
    }

    @Test
    fun latestProceduresSortsNewestFirstAndLimitsCount() {
        val procedures = listOf(
            SkinJournalProcedureEntry(id = "old", procedure_name = "Old", performed_at = "2026-04-01T10:00:00Z"),
            SkinJournalProcedureEntry(id = "new", procedure_name = "New", performed_at = "2026-04-29T10:00:00Z"),
            SkinJournalProcedureEntry(id = "middle", procedure_name = "Middle", performed_at = "2026-04-10T10:00:00Z"),
        )

        val latest = latestSkinJournalProcedures(procedures, limit = 2)

        assertEquals(listOf("new", "middle"), latest.map { it.id })
    }

    @Test
    fun skinJournalEventDaysCountsProceduresAndSensorReadingsByDate() {
        val procedures = listOf(
            SkinJournalProcedureEntry(id = "p1", performed_at = "2026-04-29T10:00:00Z"),
            SkinJournalProcedureEntry(id = "p2", performed_at = "2026-04-29T18:00:00Z"),
        )
        val readings = listOf(
            SkinJournalSensorReading(id = "s1", measured_at = "2026-04-28T08:00:00Z"),
            SkinJournalSensorReading(id = "s2", measured_at = "bad-date"),
        )

        val eventDays = skinJournalEventDays(procedures, readings)

        assertEquals(2, eventDays["2026-04-29"]?.procedureCount)
        assertEquals(1, eventDays["2026-04-28"]?.sensorCount)
        assertTrue("bad-date" !in eventDays.keys)
    }

    @Test
    fun entriesForJournalDateReturnsMatchingProceduresAndSensors() {
        val procedures = listOf(
            SkinJournalProcedureEntry(id = "p1", performed_at = "2026-04-29T10:00:00Z"),
            SkinJournalProcedureEntry(id = "p2", performed_at = "2026-04-30T10:00:00Z"),
        )
        val readings = listOf(
            SkinJournalSensorReading(id = "s1", measured_at = "2026-04-29T08:00:00Z"),
        )

        val entries = entriesForJournalDate("2026-04-29", procedures, readings)

        assertEquals(listOf("p1"), entries.procedures.map { it.id })
        assertEquals(listOf("s1"), entries.sensorReadings.map { it.id })
    }

    @Test
    fun journalZoneLabelUsesKnownLabelAndFallsBackToId() {
        assertEquals("Лоб", journalZoneLabel("forehead"))
        assertEquals("custom_zone", journalZoneLabel("custom_zone"))
    }

    @Test
    fun journalZonesFilterBySearchMatchesIdOrLabelIgnoringWhitespaceAndCase() {
        val zones = listOf(
            JournalZone("left_cheek", "Левая щека"),
            JournalZone("nose", "Нос"),
        )

        assertEquals(listOf("left_cheek"), zones.filterBySearch(" CHEEK ").map { it.id })
        assertEquals(listOf("nose"), zones.filterBySearch("нос").map { it.id })
        assertEquals(zones, zones.filterBySearch("   "))
    }

    @Test
    fun visibleJournalSearchOptionsLimitsResultsAndReportsHiddenMatches() {
        val options = (1..10).map { it.toString() to "Option $it" }

        val visible = visibleJournalSearchOptions(options, limit = 8)

        assertEquals(8, visible.items.size)
        assertEquals("1", visible.items.first().first)
        assertTrue(visible.hasMore)
    }

    @Test
    fun journalDateDisplayConvertsIsoDateToRussianInputFormat() {
        assertEquals("29.04.2026", displayJournalDate("2026-04-29T10:00:00Z"))
        assertEquals("bad-date", displayJournalDate("bad-date"))
    }

    @Test
    fun journalInputDateParsesOnlyValidRussianDates() {
        assertEquals("2026-04-29T12:00:00Z", "29.04.2026".toJournalDateTimestampOrNull())
        assertEquals(null, "2026-04-29".toJournalDateTimestampOrNull())
        assertEquals(null, "31.02.2026".toJournalDateTimestampOrNull())
    }

    @Test
    fun journalCountTextUsesRussianPluralForms() {
        assertEquals("0 процедур", 0.procedureCountText())
        assertEquals("1 процедура", 1.procedureCountText())
        assertEquals("2 процедуры", 2.procedureCountText())
        assertEquals("5 процедур", 5.procedureCountText())
        assertEquals("11 процедур", 11.procedureCountText())
        assertEquals("21 процедура", 21.procedureCountText())

        assertEquals("0 замеров", 0.sensorCountText())
        assertEquals("1 замер", 1.sensorCountText())
        assertEquals("2 замера", 2.sensorCountText())
        assertEquals("5 замеров", 5.sensorCountText())
    }

    @Test
    fun shiftJournalMonthMovesAcrossYearBoundaries() {
        assertEquals(2026 to 5, shiftJournalMonth(2026, 4, 1))
        assertEquals(2025 to 12, shiftJournalMonth(2026, 1, -1))
        assertEquals(2027 to 1, shiftJournalMonth(2026, 12, 1))
    }

    @Test
    fun skinJournalEventMonthsCountsProceduresAndSensorsByMonth() {
        val procedures = listOf(
            SkinJournalProcedureEntry(id = "p1", performed_at = "2026-04-02T10:00:00Z"),
            SkinJournalProcedureEntry(id = "p2", performed_at = "2026-04-29T18:00:00Z"),
            SkinJournalProcedureEntry(id = "bad", performed_at = "bad-date"),
        )
        val readings = listOf(
            SkinJournalSensorReading(id = "s1", measured_at = "2026-04-10T08:00:00Z"),
            SkinJournalSensorReading(id = "s2", measured_at = "2026-05-01T08:00:00Z"),
        )

        val months = skinJournalEventMonths(procedures, readings)

        assertEquals(2, months["2026-04"]?.procedureCount)
        assertEquals(1, months["2026-04"]?.sensorCount)
        assertEquals(0, months["2026-05"]?.procedureCount)
        assertEquals(1, months["2026-05"]?.sensorCount)
        assertTrue("bad-date" !in months.keys)
    }

    @Test
    fun firstJournalEventDateInMonthReturnsEarliestDate() {
        val eventDays = mapOf(
            "2026-04-20" to SkinJournalDayEvents(procedureCount = 1),
            "2026-04-03" to SkinJournalDayEvents(sensorCount = 1),
            "2026-05-01" to SkinJournalDayEvents(procedureCount = 1),
        )

        assertEquals("2026-04-03", firstJournalEventDateInMonth(2026, 4, eventDays))
    }

    @Test
    fun selectedJournalMonthDateFallsBackToFirstDayWhenMonthIsEmpty() {
        val eventDays = mapOf("2026-05-01" to SkinJournalDayEvents(procedureCount = 1))

        assertEquals("2026-04-01", selectedJournalMonthDate(2026, 4, eventDays))
    }
}
