package com.apptesters.data.repository

import com.apptesters.data.SupabaseClientProvider
import com.apptesters.data.dto.AppDetailResponseDto
import com.apptesters.data.dto.AppProgressResponseDto
import com.apptesters.data.dto.CheckInResultDto
import com.apptesters.data.dto.DiscoverAppDto
import com.apptesters.data.dto.HomeResponseDto
import com.apptesters.data.dto.NotificationDto
import com.apptesters.data.dto.NotificationPrefsDto
import com.apptesters.data.dto.ProfileResponseDto
import com.apptesters.data.dto.TesterEmailDto
import com.apptesters.domain.AppError
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Runs an rpc call and turns any failure into an AppError (cancellation passes through). */
private inline fun <T> guarded(block: () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: AppError) {
    throw e
} catch (e: Exception) {
    throw AppError.fromMessage(e.message)
}

private val postgrest get() = SupabaseClientProvider.client.postgrest

class HomeRepository {
    suspend fun getHome(): Result<HomeResponseDto> = runCatching {
        guarded { postgrest.rpc("rpc_get_home").decodeAs<HomeResponseDto>() }
    }

    /** Idempotent: a second tap on the same day returns already = true. */
    suspend fun checkIn(appId: String, note: String? = null): Result<CheckInResultDto> = runCatching {
        guarded {
            postgrest.rpc("rpc_check_in", buildJsonObject {
                put("p_app_id", appId)
                put("p_note", note)
            }).decodeAs<CheckInResultDto>()
        }
    }
}

class AppsRepository {
    suspend fun discover(
        query: String? = null, category: String? = null, limit: Int = 20, offset: Int = 0
    ): Result<List<DiscoverAppDto>> = runCatching {
        guarded {
            postgrest.rpc("rpc_discover", buildJsonObject {
                put("p_q", query)
                put("p_category", category)
                put("p_limit", limit)
                put("p_offset", offset)
            }).decodeAs<List<DiscoverAppDto>>()
        }
    }

    suspend fun getAppDetail(appId: String): Result<AppDetailResponseDto> = runCatching {
        guarded {
            postgrest.rpc("rpc_get_app_detail", buildJsonObject { put("p_app_id", appId) })
                .decodeAs<AppDetailResponseDto>()
        }
    }

    suspend fun joinApp(appId: String): Result<Unit> = runCatching {
        guarded { postgrest.rpc("rpc_join_app", buildJsonObject { put("p_app_id", appId) }); Unit }
    }

    suspend fun leaveApp(appId: String): Result<Unit> = runCatching {
        guarded { postgrest.rpc("rpc_leave_app", buildJsonObject { put("p_app_id", appId) }); Unit }
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
        guarded {
            postgrest.rpc("rpc_create_app", buildJsonObject {
                put("p_name", title)
                put("p_package_name", packageName)
                put("p_category", category)
                put("p_description", description)
                put("p_icon_url", iconUrl)
                put("p_opt_in_url", playStoreUrl)
                put("p_group_url", googleGroupUrl)
                put("p_testers_needed", testersNeeded)
            })
            Unit
        }
    }

    /** Owner only. fields: name, category, description, icon_url, opt_in_url, group_url, testers_needed, status (paused|active). */
    suspend fun updateApp(appId: String, fields: Map<String, String?>): Result<Unit> = runCatching {
        guarded {
            postgrest.rpc("rpc_update_app", buildJsonObject {
                put("p_app_id", appId)
                put("p_fields", buildJsonObject { fields.forEach { (k, v) -> put(k, v) } })
            })
            Unit
        }
    }
}

class ProgressRepository {
    suspend fun getAppProgress(appId: String): Result<AppProgressResponseDto> = runCatching {
        guarded {
            postgrest.rpc("rpc_get_app_progress", buildJsonObject { put("p_app_id", appId) })
                .decodeAs<AppProgressResponseDto>()
        }
    }

    suspend fun getTesterEmails(appId: String): Result<List<TesterEmailDto>> = runCatching {
        guarded {
            postgrest.rpc("rpc_get_tester_emails", buildJsonObject { put("p_app_id", appId) })
                .decodeAs<List<TesterEmailDto>>()
        }
    }

    suspend fun nudgeTester(appId: String, testerId: String): Result<Unit> = runCatching {
        guarded {
            postgrest.rpc("rpc_nudge_tester", buildJsonObject {
                put("p_app_id", appId)
                put("p_tester_id", testerId)
            })
            Unit
        }
    }

    suspend fun removeTester(appId: String, testerId: String): Result<Unit> = runCatching {
        guarded {
            postgrest.rpc("rpc_remove_tester", buildJsonObject {
                put("p_app_id", appId)
                put("p_tester_id", testerId)
            })
            Unit
        }
    }
}

class ProfileRepository {
    suspend fun getProfile(userId: String? = null): Result<ProfileResponseDto> = runCatching {
        guarded {
            postgrest.rpc("rpc_get_profile", buildJsonObject { put("p_user_id", userId) })
                .decodeAs<ProfileResponseDto>()
        }
    }

    /** Only the values you pass (not null) are changed. Username is lower-cased by the server. */
    suspend fun updateProfile(
        username: String? = null,
        displayName: String? = null,
        bio: String? = null,
        avatarUrl: String? = null,
        isPublic: Boolean? = null,
        showHistory: Boolean? = null,
        findByEmail: Boolean? = null,
        playEmail: String? = null,
        deviceModel: String? = null,
        androidVersion: String? = null,
        theme: String? = null,
        language: String? = null
    ): Result<ProfileResponseDto> = runCatching {
        guarded {
            postgrest.rpc("rpc_update_profile", buildJsonObject {
                put("p_fields", buildJsonObject {
                    username?.let { put("username", it) }
                    displayName?.let { put("display_name", it) }
                    bio?.let { put("bio", it) }
                    avatarUrl?.let { put("avatar_url", it) }
                    isPublic?.let { put("is_public", it) }
                    showHistory?.let { put("show_history", it) }
                    findByEmail?.let { put("find_by_email", it) }
                    playEmail?.let { put("play_email", it) }
                    deviceModel?.let { put("device_model", it) }
                    androidVersion?.let { put("android_version", it) }
                    theme?.let { put("theme", it) }
                    language?.let { put("language", it) }
                })
            }).decodeAs<ProfileResponseDto>()
        }
    }
}

class InboxRepository {
    suspend fun listNotifications(limit: Int = 30, before: String? = null): Result<List<NotificationDto>> = runCatching {
        guarded {
            postgrest.rpc("rpc_list_notifications", buildJsonObject {
                put("p_limit", limit)
                put("p_before", before)
            }).decodeAs<List<NotificationDto>>()
        }
    }

    /** ids = null marks everything read. */
    suspend fun markRead(ids: List<String>? = null): Result<Unit> = runCatching {
        guarded {
            postgrest.rpc("rpc_mark_notifications_read", buildJsonObject {
                if (ids != null) put("p_ids", JsonArray(ids.map { JsonPrimitive(it) }))
            })
            Unit
        }
    }
}

class SettingsRepository {
    suspend fun savePrefs(prefs: Map<String, Any>): Result<NotificationPrefsDto> = runCatching {
        guarded {
            postgrest.rpc("rpc_save_prefs", buildJsonObject {
                put("p_prefs", buildJsonObject {
                    prefs.forEach { (k, v) ->
                        when (v) {
                            is Boolean -> put(k, v)
                            else -> put(k, v.toString())   // remind_at as "HH:mm"
                        }
                    }
                })
            }).decodeAs<NotificationPrefsDto>()
        }
    }

    suspend fun blockUser(userId: String): Result<Unit> = runCatching {
        guarded { postgrest.rpc("rpc_block_user", buildJsonObject { put("p_user_id", userId) }); Unit }
    }

    /** targetType: user | app | feedback */
    suspend fun report(targetType: String, targetId: String, reason: String): Result<Unit> = runCatching {
        guarded {
            postgrest.rpc("rpc_report", buildJsonObject {
                put("p_target_type", targetType)
                put("p_target_id", targetId)
                put("p_reason", reason)
            })
            Unit
        }
    }

    /** kind: bug | suggestion | praise. screenshotPath = "<uid>/<appId>/<file>" inside the feedback bucket. */
    suspend fun sendFeedback(appId: String, kind: String, body: String, screenshotPath: String? = null): Result<Unit> = runCatching {
        guarded {
            postgrest.rpc("rpc_send_feedback", buildJsonObject {
                put("p_app_id", appId)
                put("p_kind", kind)
                put("p_body", body)
                put("p_screenshot_path", screenshotPath)
            })
            Unit
        }
    }
}
