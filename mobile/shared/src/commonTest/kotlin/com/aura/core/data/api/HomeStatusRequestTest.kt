package com.aura.core.data.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeStatusRequestTest {
    @Test
    fun buildsCoordinatesOnlyWhenBothValuesExist() {
        assertEquals("latitude=55.75&longitude=37.62", homeStatusCoordinateQuery(55.75, 37.62))
        assertNull(homeStatusCoordinateQuery(55.75, null))
        assertNull(homeStatusCoordinateQuery(null, 37.62))
        assertNull(homeStatusCoordinateQuery(null, null))
    }
}
