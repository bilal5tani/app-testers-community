@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
package com.apptesters.data.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull

// Kotlin property names stay the same as before so the screens keep compiling;
// @SerialName points each one at the key the Supabase rpc functions really return.

/** Reads any JSON value (object, array, text) into a String, so `data` can stay a String. */
object JsonAsStringSerializer : KSerializer<String?> {
    override val descriptor = PrimitiveSerialDescriptor("JsonAsString", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): String? {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement() ?: return decoder.decodeString()
        return if (element is JsonNull) null else element.toString()
    }
    override fun serialize(encoder: Encoder, value: String?) {
        if (value == null) encoder.encodeNull() else encoder.encodeString(value)
    }
}

// ---------------------------------------------------------------- rpc_get_home
@Serializable
data class HomeResponseDto(
    @SerialName("my_apps") val myApps: List<HomeSummaryAppDto> = emptyList(),
    @SerialName("today") val todayCheckins: List<TodayCheckInDto> = emptyList(),
    @SerialName("needs_testers") val needsTesters: List<NeedsTesterAppDto> = emptyList(),
    @SerialName("unread_count") val unreadNotifications: Int = 0
) {
    /** First app the user owns (the Home card shows one). */
    val myApp: HomeSummaryAppDto? get() = myApps.firstOrNull()
}

@Serializable
data class HomeSummaryAppDto(
    val id: String,
    @SerialName("name") val title: String,
    @SerialName("icon_url") val iconUrl: String? = null,
    val status: String,
    @SerialName("qualified_days") val qualifiedDays: Int = 0,
    @SerialName("testers_needed") val testersNeeded: Int = 12,
    @SerialName("opted_in") val joinedCount: Int = 0,
    @SerialName("checked_in_today") val checkedInToday: Int = 0
)

@Serializable
data class TodayCheckInDto(
    @SerialName("app_id") val appId: String,
    @SerialName("name") val title: String,
    @SerialName("icon_url") val iconUrl: String? = null,
    @SerialName("package_name") val packageName: String,
    @SerialName("opt_in_url") val playStoreUrl: String,
    @SerialName("checked_today") val checkedInToday: Boolean = false,
    val streak: Int = 0,
    @SerialName("days_done") val daysDone: Int = 0
)

@Serializable
data class NeedsTesterAppDto(
    val id: String,
    @SerialName("name") val title: String,
    @SerialName("icon_url") val iconUrl: String? = null,
    val category: String = "other",
    @SerialName("spots_left") val spotsLeft: Int = 0,
    @SerialName("owner_name") val ownerName: String
)

// ---------------------------------------------------------------- rpc_discover
@Serializable
data class DiscoverAppDto(
    val id: String,
    @SerialName("name") val title: String,
    @SerialName("icon_url") val iconUrl: String? = null,
    val category: String = "other",
    val description: String? = null,
    @SerialName("testers_needed") val testersNeeded: Int = 12,
    @SerialName("spots_left") val spotsLeft: Int = 0,
    @SerialName("opted_in") val joinedCount: Int = 0,
    @SerialName("owner_name") val ownerName: String,
    @SerialName("owner_avatar") val ownerAvatar: String? = null,
    @SerialName("owner_karma") val ownerKarma: Int = 0,
    @SerialName("listed_at") val createdAt: String = "",
    /** null = not enrolled; otherwise joined / dropped / completed / removed */
    @SerialName("my_status") val myStatus: String? = null
)

// ---------------------------------------------------------------- rpc_get_app_detail
@Serializable
data class AppDetailResponseDto(
    val app: AppInfoDto,
    val owner: OwnerInfoDto,
    @SerialName("is_owner") val isOwner: Boolean = false,
    @SerialName("my_enrollment") val myEnrollment: MyEnrollmentDto? = null,
    @SerialName("tester_faces") val testerFaces: List<TesterFaceDto> = emptyList()
)

@Serializable
data class AppInfoDto(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("name") val title: String,
    @SerialName("package_name") val packageName: String,
    @SerialName("opt_in_url") val playStoreUrl: String,
    @SerialName("group_url") val googleGroupUrl: String,
    @SerialName("icon_url") val iconUrl: String? = null,
    val category: String = "other",
    val description: String? = null,
    val status: String,
    @SerialName("testers_needed") val testersNeeded: Int = 12,
    @SerialName("spots_left") val spotsLeft: Int = 0,
    @SerialName("opted_in") val joinedCount: Int = 0,
    @SerialName("qualified_days") val qualifiedDays: Int = 0
)

@Serializable
data class OwnerInfoDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    val username: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val karma: Int = 0
)

@Serializable
data class MyEnrollmentDto(
    val status: String,
    @SerialName("joined_at") val joinedAt: String,
    val streak: Int = 0,
    @SerialName("days_done") val daysDone: Int = 0,
    @SerialName("checked_today") val checkedToday: Boolean = false
)

@Serializable
data class TesterFaceDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

// ---------------------------------------------------------------- rpc_get_app_progress (owner only)
@Serializable
data class AppProgressResponseDto(
    @SerialName("app_id") val appId: String,
    val name: String = "",
    val status: String = "",
    @SerialName("qualified_days") val qualifiedDays: Int = 0,
    @SerialName("required_days") val requiredDays: Int = 14,
    @SerialName("testers_needed") val testersNeeded: Int = 12,
    @SerialName("opted_in") val joinedCount: Int = 0,
    @SerialName("checked_in_today") val checkedInToday: Int = 0,
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
    @SerialName("days_done") val daysDone: Int = 0,
    @SerialName("checked_today") val checkedInToday: Boolean = false,
    @SerialName("missed_streak") val missedStreak: Int = 0,
    /** last 14 days, oldest first; true = checked in */
    val pattern: List<Boolean> = emptyList()
)

@Serializable
data class TesterEmailDto(
    @SerialName("tester_id") val testerId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("play_email") val playEmail: String
)

// ---------------------------------------------------------------- rpc_get_profile
@Serializable
data class ProfileResponseDto(
    val profile: UserProfileDto,
    val private: PrivateProfileDto? = null,
    @SerialName("my_apps") val myApps: List<MyListedAppDto> = emptyList(),
    val stats: UserStatsDto,
    val history: List<HistoryItemDto> = emptyList()
)

@Serializable
data class UserProfileDto(
    val id: String,
    val username: String? = null,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    @SerialName("is_public") val isPublic: Boolean = true,
    @SerialName("show_history") val showHistory: Boolean = true,
    @SerialName("find_by_email") val findByEmail: Boolean = false,
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
    @SerialName("name") val title: String,
    @SerialName("icon_url") val iconUrl: String? = null,
    val status: String,
    @SerialName("qualified_days") val qualifiedDays: Int = 0,
    @SerialName("testers_needed") val testersNeeded: Int = 12
)

@Serializable
data class UserStatsDto(
    @SerialName("apps_tested") val appsTested: Int = 0,
    @SerialName("best_streak") val bestStreak: Int = 0,
    val karma: Int = 0,
    @SerialName("testers_received") val testersReceived: Int = 0
)

@Serializable
data class HistoryItemDto(
    @SerialName("app_id") val appId: String,
    @SerialName("name") val title: String,
    @SerialName("icon_url") val iconUrl: String? = null,
    val status: String,
    @SerialName("joined_at") val joinedAt: String,
    @SerialName("days_done") val daysDone: Int = 0
)

// ---------------------------------------------------------------- rpc_list_notifications
@Serializable
data class NotificationDto(
    val id: String,
    @SerialName("type") val event: String,
    val title: String = "",
    val body: String = "",
    /** JSON text, e.g. {"route":"app/<id>/progress"} */
    @Serializable(with = JsonAsStringSerializer::class) val data: String? = null,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String
)

// ---------------------------------------------------------------- small results
@Serializable
data class CheckInResultDto(
    val day: String,
    val already: Boolean = false,
    val streak: Int = 0,
    @SerialName("days_done") val daysDone: Int = 0
)

@Serializable
data class NotificationPrefsDto(
    val remind: Boolean = true,
    @SerialName("streak_risk") val streakRisk: Boolean = true,
    @SerialName("new_apps") val newApps: Boolean = false,
    @SerialName("tester_joined") val testerJoined: Boolean = true,
    @SerialName("tester_missed") val testerMissed: Boolean = true,
    @SerialName("reached_target") val reachedTarget: Boolean = true,
    @SerialName("period_complete") val periodComplete: Boolean = true,
    val weekly: Boolean = false,
    @SerialName("remind_at") val remindAt: String = "20:00:00"
)
