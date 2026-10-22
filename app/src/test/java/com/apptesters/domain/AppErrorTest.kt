package com.apptesters.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppErrorTest {

    @Test
    fun testErrorMapping() {
        assertEquals(AppError.NotAuthenticated, AppError.fromMessage("P0001: not_authenticated"))
        assertEquals(AppError.NotFound, AppError.fromMessage("P0001: not_found"))
        assertEquals(AppError.Forbidden, AppError.fromMessage("P0001: forbidden"))
        assertEquals(AppError.AlreadyJoined, AppError.fromMessage("P0001: already_joined"))
        assertEquals(AppError.AppFull, AppError.fromMessage("P0001: app_full"))
        assertEquals(AppError.OwnApp, AppError.fromMessage("P0001: own_app"))
        assertEquals(AppError.TooManyActiveTests, AppError.fromMessage("P0001: too_many_active_tests"))
        assertEquals(AppError.NotEnrolled, AppError.fromMessage("P0001: not_enrolled"))
        assertEquals(AppError.AppNotOpen, AppError.fromMessage("P0001: app_not_open"))
        assertEquals(AppError.AlreadyNudged, AppError.fromMessage("P0001: already_nudged"))
        assertEquals(AppError.InvalidInput, AppError.fromMessage("P0001: invalid_input"))
        assertEquals(AppError.Blocked, AppError.fromMessage("P0001: blocked"))

        val unknownError = AppError.fromMessage("Random network timeout")
        assertTrue(unknownError is AppError.Unknown)
    }
}
