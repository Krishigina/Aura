package com.aura.core.data.api

import kotlin.test.Test
import kotlin.test.assertEquals

class RoutineProductOptionParsingTest {

    private val apiClient = AuraApiClient("http://localhost")

    @Test
    fun displayLabelFallsBackWhenFieldsMissing() {
        val option = com.aura.core.domain.model.RoutineProductOption(id = 1, brand = null, name = "  ")

        assertEquals("Без названия", option.displayLabel)
    }

    @Test
    fun parseRoutineProductOptionsReturnsEmptyListForMalformedPayload() {
        val parsed = apiClient.parseRoutineProductOptions("not-json")

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

        val parsed = apiClient.parseRoutineProductOptions(payload)

        assertEquals(2, parsed.size)
        assertEquals(11, parsed[0].id)
        assertEquals("ACME Foam", parsed[0].displayLabel)
        assertEquals(12, parsed[1].id)
        assertEquals("Serum", parsed[1].displayLabel)
    }
}
