package com.resumelens.domain.model

/**
 * Every failure the user can hit, as a closed set. The UI maps each case to a
 * friendly message, so no raw exception text ever reaches the screen.
 */
sealed interface AppError {
    // PDF
    data object PdfUnreadable : AppError
    data object PdfEncrypted : AppError
    data object PdfNoText : AppError
    data object PdfTooLarge : AppError

    // Input validation
    data object ResumeTooShort : AppError
    data object JobDescriptionTooShort : AppError

    // Network / API
    data object MissingApiKey : AppError
    data object InvalidApiKey : AppError
    data object ModelUnavailable : AppError
    data object RateLimited : AppError
    data object NoConnection : AppError
    data object Timeout : AppError
    data object ContentBlocked : AppError
    data class Server(val code: Int) : AppError
    data object BadResponse : AppError

    data object Unknown : AppError
}

/** Minimal success/failure type. Kept separate from kotlin.Result so errors are typed. */
sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}
