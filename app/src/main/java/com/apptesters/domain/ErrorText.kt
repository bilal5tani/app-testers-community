package com.apptesters.domain

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException

/** Turns any exception into a short, user-friendly sentence for the UI. */
fun Throwable.userMessage(): String = when (this) {
    is AppError.NotAuthenticated -> "Your session expired. Please sign in again."
    is AppError.NotFound -> "This item no longer exists."
    is AppError.Forbidden -> "You are not allowed to do that."
    is AppError.AlreadyJoined -> "You have already joined this app."
    is AppError.AppFull -> "All tester spots are taken."
    is AppError.OwnApp -> "You cannot test your own app."
    is AppError.TooManyActiveTests -> "You can test at most 5 apps at a time."
    is AppError.NotEnrolled -> "You are not testing this app."
    is AppError.AppNotOpen -> "This app is not accepting testers right now."
    is AppError.AlreadyNudged -> "You already nudged this tester today."
    is AppError.InvalidInput -> "Please check the details and try again."
    is AppError.UsernameTaken -> "This username is already taken."
    is AppError.PackageTaken -> "This package name is already listed."
    is AppError.Blocked -> "This action is not available."
    is AppError.Network -> "No internet connection. Please try again."
    is AppError.Unknown -> "Something went wrong. Please try again."
    is GetCredentialCancellationException -> "Sign-in was cancelled."
    is NoCredentialException -> "No Google account found on this device."
    else -> {
        val m = message.orEmpty()
        if (m.contains("Unable to resolve host") || m.contains("timeout", ignoreCase = true))
            "No internet connection. Please try again."
        else "Something went wrong. Please try again."
    }
}
