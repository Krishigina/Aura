package com.aura.core.data.api

import com.aura.core.data.api.client.parseRoutineProductOptions
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class RoutineProductOptionParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun displayLabelFallsBackWhenFieldsMissing() {
        val option = com.aura.core.domain.model.RoutineProductOption(id = 1, brand = null, name = "  ")

        assertEquals("Без названия", option.displayLabel)
    }

    @Test
    fun parseRoutineProductOptionsReturnsEmptyListForMalformedPayload() {
        val parsed = parseRoutineProductOptions("not-json", json)

        assertEquals(emptyList(), parsed)
    }

    @Test
    fun parseRoutineProductOptionsSkipsMalformedItemsAndKeepsValidOnes() {
        val payload =
            """
            [
              {"id": 11, "brand": "ACME", "name": "Foam"},
              {"id": "bad", "brand": "X", "name": "Y"},
              {"brand": "NoId", "name": "Skipped"},
              {"id": 12, "brand": null, "name": "Serum"}
            ]
            """.trimIndent()

        val parsed = parseRoutineProductOptions(payload, json)

        assertEquals(2, parsed.size)
        assertEquals(11, parsed[0].id)
        assertEquals("ACME Foam", parsed[0].displayLabel)
        assertEquals(12, parsed[1].id)
        assertEquals("Serum", parsed[1].displayLabel)
    }

    @Test
    fun parseRoutineProductOptionsSkipsItemsWithNonPrimitiveFields() {
        val payload =
            """
            [
              {"id": 31, "brand": "Valid", "name": "One"},
              {"id": {"value": 32}, "brand": "Broken", "name": "IdObject"},
              {"id": 33, "brand": ["Nested"], "name": "BrandArray"},
              {"id": 34, "brand": "Good", "name": {"text": "Nested"}},
              {"id": 35, "brand": null, "name": "Two"}
            ]
            """.trimIndent()

        val parsed = parseRoutineProductOptions(payload, json)

        assertEquals(2, parsed.size)
        assertEquals(31, parsed[0].id)
        assertEquals("Valid One", parsed[0].displayLabel)
        assertEquals(35, parsed[1].id)
        assertEquals("Two", parsed[1].displayLabel)
    }
}
