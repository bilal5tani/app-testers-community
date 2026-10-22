package com.apptesters.data.repository

import com.apptesters.data.SupabaseClientProvider
import com.apptesters.data.dto.AppDetailResponseDto
import com.apptesters.data.dto.AppProgressResponseDto
import com.apptesters.data.dto.DiscoverAppDto
import com.apptesters.data.dto.HomeResponseDto
import com.apptesters.data.dto.NotificationDto
import com.apptesters.data.dto.ProfileResponseDto
import com.apptesters.domain.AppError
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class HomeRepository {
    private val postgrest = SupabaseClientProvider.client.postgrest

    suspend fun getHome(): Result<HomeResponseDto> = runCatching {
        try {
            postgrest.rpc("rpc_get_home").decodeAs<HomeResponseDto>()
        } catch (e: Exception) {
            throw AppError.fromMessage(e.message)
        }
    }

    suspend fun checkIn(appId: String, note: String? = null): Result<Unit> = runCatching {
        try {
            postgrest.rpc("rpc_check_in", buildJsonObject {
                put("p_app_id", appId)
                put("p_note", note)
            })
        } catch (e: Exception) {
            throw AppError.fromMessage(e.message)
        }
    }
}

class AppsRepository {
    private val postgrest = SupabaseClientProvider.client.postgrest

    suspend fun discover(query: String? = null, category: String? = null, limit: Int = 20, offset: Int = 0): Result<List<DiscoverAppDto>> = runCatching {
        try {
            postgrest.rpc("rpc_discover", buildJsonObject {
                put("p_q", query)
                put("p_category", category)
                put("p_limit", limit)
                put("p_offset", offset)
            }).decodeAs()
        } catch (e: Exception) {
            throw AppError.fromMessage(e.message)
        }
    }

    suspend fun getAppDetail(appId: String): Result<AppDetailResponseDto> = runCatching {
        try {
            postgrest.rpc("rpc_get_app_detail", buildJsonObject {
                put("p_app_id", appId)
            }).decodeAs()
        } catch (e: Exception) {
            throw AppError.fromMessage(e.message)
        }
    }

    suspend fun joinApp(appId: String): Result<Unit> = runCatching {
        try {
            postgrest.rpc("rpc_join_app", buildJsonObject {
                put("p_app_id", appId)
            })
        } catch (e: Exception) {
            throw AppError.fromMessage(e.message)
        }
    }

    suspend fun leaveApp(appId: String): Result<Unit> = runCatching {
        try {
            postgrest.rpc("rpc_leave_app", buildJsonObject {
                put("p_app_id", appId)
            })
        } catch (e: Exception) {
            throw AppError.fromMessage(e.message)
        }
    }

    suspend fun createApp(
        title: String,
        packageName: String,
        playStoreUrl: String,
        googleGroupUrl: String,
        iconUrl: String? = null,
        category: String = "other",
        description: String? = null,
        testersNeeded: Int = 12
    ): Result<Unit> = runCatching {
        try {
            postgrest.rpc("rpc_create_app", buildJsonObject {
                put("p_title", title)
                put("p_package_name", packageName)
                put("p_play_store_url", playStoreUrl)
                put("p_google_group_url", googleGroupUrl)
                put("p_icon_url", iconUrl)
                put("p_category", category)
                put("p_description", description)
                put("p_testers_needed", testersNeeded)
            })
        } catch (e: Exception) {
            throw AppError.fromMessage(e.message)
        }
    }
}

class ProgressRepository {
    private val postgrest = SupabaseClientProvider.client.postgrest

    suspend fun getAppProgress(appId: String): Result<AppProgressResponseDto> = runCatching {
        try {
            postgrest.rpc("rpc_get_app_progress", buildJsonObject {
                put("p_app_id", appId)
            }).decodeAs()
        } catch (e: Exception) {
            throw AppError.fromMessage(e.message)
        }
    }

    suspend fun nudgeTester(appId: String, testerId: String): Result<Unit> = runCatching {
        try {
            postgrest.rpc("rpc_nudge_tester", buildJsonObject {
                put("p_app_id", appId)
                put("p_tester_id", testerId)
            })
        } catch (e: Exception) {
            throw AppError.fromMessage(e.message)
        }
    }
}

class ProfileRepository {
    private val postgrest = SupabaseClientProvider.client.postgrest

    suspend fun getProfile(userId: String? = null): Result<ProfileResponseDto> = runCatching {
        try {
            postgrest.rpc("rpc_get_profile", buildJsonObject {
                put("p_user_id", userId)
            }).decodeAs()
        } catch (e: Exception) {
            throw AppError.fromMessage(e.message)
        }
    }

    suspend fun updateProfile(
        username: String? = null,
        displayName: String? = null,
        bio: String? = null,
        avatarUrl: String? = null,
        isPublic: Boolean? = null,
        playEmail: String? = null,
        deviceModel: String? = null,
        androidVersion: String? = null,
        theme: String? = null,
        language: String? = null
    ): Result<Unit> = runCatching {
        try {
            postgrest.rpc("rpc_update_profile", buildJsonObject {
                put("p_username", username)
                put("p_display_name", displayName)
                put("p_bio", bio)
                put("p_avatar_url", avatarUrl)
                put("p_is_public", isPublic)
                put("p_play_email", playEmail)
                put("p_device_model", deviceModel)
                put("p_android_version", androidVersion)
                put("p_theme", theme)
                put("p_language", language)
            })
        } catch (e: Exception) {
            throw AppError.fromMessage(e.message)
        }
    }
}

class InboxRepository {
    private val postgrest = SupabaseClientProvider.client.postgrest

    suspend fun listNotifications(limit: Int = 20): Result<List<NotificationDto>> = runCatching {
        try {
            postgrest.rpc("rpc_list_notifications", buildJsonObject {
                put("p_limit", limit)
            }).decodeAs()
        } catch (e: Exception) {
            throw AppError.fromMessage(e.message)
        }
    }

    suspend fun markRead(ids: List<String>? = null): Result<Unit> = runCatching {
        try {
            postgrest.rpc("rpc_mark_notifications_read", buildJsonObject {
                if (ids != null) put("p_ids", ids.joinToString(",", "[", "]"))
            })
        } catch (e: Exception) {
            throw AppError.fromMessage(e.message)
        }
    }
}
