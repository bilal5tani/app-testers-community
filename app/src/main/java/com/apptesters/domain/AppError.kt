package com.apptesters.domain

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
    data object Blocked : AppError()
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
                msg.contains("invalid_input") -> InvalidInput
                msg.contains("blocked") -> Blocked
                else -> Unknown(msg)
            }
        }
    }
}
