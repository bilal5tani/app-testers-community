package com.apptesters.data.repository

import com.apptesters.data.dto.DiscoverAppDto
import com.apptesters.data.dto.HomeResponseDto
import com.apptesters.data.dto.HomeSummaryAppDto
import com.apptesters.data.dto.NeedsTesterAppDto
import com.apptesters.data.dto.TodayCheckInDto

class FakeHomeRepository {
    suspend fun getHome(): Result<HomeResponseDto> = Result.success(
        HomeResponseDto(
            myApps = listOf(
                HomeSummaryAppDto(
                    id = "sample-app-1",
                    title = "My Sample App",
                    status = "testing",
                    qualifiedDays = 6,
                    testersNeeded = 12,
                    joinedCount = 9
                )
            ),
            todayCheckins = listOf(
                TodayCheckInDto(
                    appId = "test-app-1",
                    title = "Demo Tester App",
                    packageName = "com.demo.app",
                    playStoreUrl = "https://play.google.com/apps/testing/com.demo.app",
                    checkedInToday = false,
                    streak = 4
                )
            ),
            needsTesters = listOf(
                NeedsTesterAppDto(
                    id = "needs-app-1",
                    title = "Cool Productivity Tool",
                    category = "tools",
                    spotsLeft = 3,
                    ownerName = "Ali Raza"
                )
            ),
            unreadNotifications = 2
        )
    )
}

class FakeAppsRepository {
    suspend fun discover(): Result<List<DiscoverAppDto>> = Result.success(
        listOf(
            DiscoverAppDto(
                id = "app-1",
                title = "Fitness Tracker Pro",
                category = "lifestyle",
                description = "Closed test for fitness tracker app",
                spotsLeft = 4,
                ownerName = "Usman Ahmed",
                ownerKarma = 5,
                createdAt = "2026-10-21T10:00:00Z"
            )
        )
    )
}
