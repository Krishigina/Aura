package com.aura.core.domain.model

import com.aura.core.data.api.model.RecommendationFavoritesResponse
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class RecommendationFavoriteContractsSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decodesRecommendationFavoritesListSnapshot() {
        val payload = """
            {
              "items": [
                {
                  "favorite_id": "fav-1",
                  "id": "rec-1",
                  "saved_at": "2026-06-11T12:00:00Z",
                  "generated_at": "2026-06-11T11:58:00Z",
                  "summary": {"title": "Персональная линейка", "description": "Фокус на барьер"},
                  "lines": [
                    {
                      "key": "professional",
                      "title": "Профессиональная линейка",
                      "routine": {
                        "morning": [{"product_id": 10, "product_name": "Cleanser", "compatibility_percent": 86}],
                        "evening": [],
                        "weekly": [{"product_id": 11, "product_name": "Enzyme Powder", "frequency": "1-2 раза в неделю"}]
                      }
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val response = json.decodeFromString<RecommendationFavoritesResponse>(payload)

        assertEquals(1, response.items.size)
        assertEquals("fav-1", response.items[0].favoriteId)
        assertEquals("2026-06-11T12:00:00Z", response.items[0].savedAt)
        assertEquals("Персональная линейка", response.items[0].summary.title)
        assertEquals("Cleanser", response.items[0].lines[0].routine.morning[0].productName)
        assertEquals("Enzyme Powder", response.items[0].lines[0].routine.weekly[0].productName)
    }
}
