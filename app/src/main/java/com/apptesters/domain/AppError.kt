package com.apptesters.domain

/** Errors raised by the Supabase rpc functions (message = code). */
sealed class AppError : Exception() {
    data object NotAuthenticated : AppError()
    data object NotFound : AppError()
    data object Forbidden : AppError()
    data object AlreadyJoined : AppError()
    data object AppFull : AppError()
    data object OwnApp : AppError()
    data object TooManyActiveTests : AppError()
    data object NotEnrolled : AppError()
    data object AppNotOpen : AppError()
    data object AlreadyNudged : AppError()
    data object InvalidInput : AppError()
    data object UsernameTaken : AppError()
    data object PackageTaken : AppError()
    data object Blocked : AppError()
    data object Network : AppError()
    data class Unknown(override val message: String) : AppError()

    companion object {
        fun fromMessage(msg: String?): AppError {
            if (msg == null) return Unknown("An unexpected error occurred.")
            return when {
                msg.contains("not_authenticated") -> NotAuthenticated
                msg.contains("not_found") -> NotFound
                msg.contains("forbidden") -> Forbidden
                msg.contains("already_joined") -> AlreadyJoined
                msg.contains("app_full") -> AppFull
                msg.contains("own_app") -> OwnApp
                msg.contains("too_many_active_tests") -> TooManyActiveTests
                msg.contains("not_enrolled") -> NotEnrolled
                msg.contains("app_not_open") -> AppNotOpen
                msg.contains("already_nudged") -> AlreadyNudged
                msg.contains("username_taken") -> UsernameTaken
                msg.contains("apps_package_name_key") -> PackageTaken
                msg.contains("invalid_input") -> InvalidInput
                msg.contains("blocked") -> Blocked
                msg.contains("Unable to resolve host") || msg.contains("timeout", ignoreCase = true) ||
                    msg.contains("Failed to connect") -> Network
                else -> Unknown(msg)
            }
        }
    }
}
