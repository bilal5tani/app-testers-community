package com.apptesters.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HomeResponseDto(
    @SerialName("my_app") val myApp: HomeSummaryAppDto? = null,
    @SerialName("today_checkins") val todayCheckins: List<TodayCheckInDto> = emptyList(),
    @SerialName("needs_testers") val needsTesters: List<NeedsTesterAppDto> = emptyList(),
    @SerialName("unread_notifications") val unreadNotifications: Int = 0
)

@Serializable
data class HomeSummaryAppDto(
    val id: String,
    val title: String,
    @SerialName("icon_url") val iconUrl: String? = null,
    val status: String,
    @SerialName("qualified_days") val qualifiedDays: Int = 0,
    @SerialName("testers_needed") val testersNeeded: Int = 12,
    @SerialName("joined_count") val joinedCount: Int = 0
)

@Serializable
data class TodayCheckInDto(
    @SerialName("app_id") val appId: String,
    val title: String,
    @SerialName("icon_url") val iconUrl: String? = null,
    @SerialName("package_name") val packageName: String,
    @SerialName("play_store_url") val playStoreUrl: String,
    @SerialName("checked_in_today") val checkedInToday: Boolean = false,
    val streak: Int = 0
)

@Serializable
data class NeedsTesterAppDto(
    val id: String,
    val title: String,
    @SerialName("icon_url") val iconUrl: String? = null,
    val category: String = "other",
    @SerialName("spots_left") val spotsLeft: Int = 0,
    @SerialName("owner_name") val ownerName: String
)

@Serializable
data class DiscoverAppDto(
    val id: String,
    val title: String,
    @SerialName("icon_url") val iconUrl: String? = null,
    val category: String = "other",
    val description: String? = null,
    @SerialName("testers_needed") val testersNeeded: Int = 12,
    @SerialName("spots_left") val spotsLeft: Int = 0,
    @SerialName("owner_name") val ownerName: String,
    @SerialName("owner_avatar") val ownerAvatar: String? = null,
    @SerialName("owner_karma") val ownerKarma: Int = 0,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class AppDetailResponseDto(
    val app: AppInfoDto,
    val owner: OwnerInfoDto,
    @SerialName("my_enrollment") val myEnrollment: MyEnrollmentDto? = null,
    @SerialName("tester_faces") val testerFaces: List<TesterFaceDto> = emptyList()
)

@Serializable
data class AppInfoDto(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val title: String,
    @SerialName("package_name") val packageName: String,
    @SerialName("play_store_url") val playStoreUrl: String,
    @SerialName("google_group_url") val googleGroupUrl: String,
    @SerialName("icon_url") val iconUrl: String? = null,
    val category: String = "other",
    val description: String? = null,
    val status: String,
    @SerialName("testers_needed") val testersNeeded: Int = 12,
    @SerialName("spots_left") val spotsLeft: Int = 0,
    @SerialName("qualified_days") val qualifiedDays: Int = 0
)

@Serializable
data class OwnerInfoDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val karma: Int = 0
)

@Serializable
data class MyEnrollmentDto(
    val status: String,
    @SerialName("joined_at") val joinedAt: String,
    val streak: Int = 0
)

@Serializable
data class TesterFaceDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class AppProgressResponseDto(
    @SerialName("app_id") val appId: String,
    @SerialName("qualified_days") val qualifiedDays: Int = 0,
    @SerialName("testers_needed") val testersNeeded: Int = 12,
    @SerialName("joined_count") val joinedCount: Int = 0,
    val testers: List<ProgressTesterDto> = emptyList()
)

@Serializable
data class ProgressTesterDto(
    @SerialName("tester_id") val testerId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val status: String,
    @SerialName("joined_at") val joinedAt: String,
    val streak: Int = 0,
    @SerialName("checked_in_today") val checkedInToday: Boolean = false,
    @SerialName("missed_streak") val missedStreak: Int = 0
)

@Serializable
data class ProfileResponseDto(
    val profile: UserProfileDto,
    val private: PrivateProfileDto? = null,
    @SerialName("my_apps") val myApps: List<MyListedAppDto> = emptyList(),
    val stats: UserStatsDto
)

@Serializable
data class UserProfileDto(
    val id: String,
    val username: String? = null,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    @SerialName("is_public") val isPublic: Boolean = true,
    val karma: Int = 0,
    val theme: String = "system",
    val language: String = "en"
)

@Serializable
data class PrivateProfileDto(
    @SerialName("play_email") val playEmail: String? = null,
    @SerialName("device_model") val deviceModel: String? = null,
    @SerialName("android_version") val androidVersion: String? = null
)

@Serializable
data class MyListedAppDto(
    val id: String,
    val title: String,
    @SerialName("icon_url") val iconUrl: String? = null,
    val status: String,
    @SerialName("qualified_days") val qualifiedDays: Int = 0
)

@Serializable
data class UserStatsDto(
    @SerialName("apps_tested") val appsTested: Int = 0,
    val karma: Int = 0,
    @SerialName("testers_received") val testersReceived: Int = 0
)

@Serializable
data class NotificationDto(
    val id: String,
    val event: String,
    val data: String? = null,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String
)
