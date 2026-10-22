package com.apptesters.data

import com.apptesters.data.repository.FakeAppsRepository
import com.apptesters.data.repository.FakeHomeRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeRepositoriesTest {

    @Test
    fun testFakeHomeRepository() = runTest {
        val repo = FakeHomeRepository()
        val result = repo.getHome()
        assertTrue(result.isSuccess)

        val homeData = result.getOrNull()
        assertNotNull(homeData)
        assertEquals("sample-app-1", homeData?.myApp?.id)
        assertEquals(1, homeData?.todayCheckins?.size)
        assertEquals(1, homeData?.needsTesters?.size)
    }

    @Test
    fun testFakeAppsRepository() = runTest {
        val repo = FakeAppsRepository()
        val result = repo.discover()
        assertTrue(result.isSuccess)

        val apps = result.getOrNull()
        assertNotNull(apps)
        assertEquals(1, apps?.size)
        assertEquals("Fitness Tracker Pro", apps?.first()?.title)
    }
}
