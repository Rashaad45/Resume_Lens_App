package com.resumelens.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.resumelens.R
import com.resumelens.domain.model.AppError

@Composable
fun AppError.message(): String = when (this) {
    AppError.PdfUnreadable -> stringResource(R.string.error_pdf_unreadable)
    AppError.PdfEncrypted -> stringResource(R.string.error_pdf_encrypted)
    AppError.PdfNoText -> stringResource(R.string.error_pdf_no_text)
    AppError.PdfTooLarge -> stringResource(R.string.error_pdf_too_large)
    AppError.ResumeTooShort -> stringResource(R.string.error_resume_short)
    AppError.JobDescriptionTooShort -> stringResource(R.string.error_job_short)
    AppError.MissingApiKey -> stringResource(R.string.error_missing_key)
    AppError.InvalidApiKey -> stringResource(R.string.error_invalid_key)
    AppError.ModelUnavailable -> stringResource(R.string.error_model)
    AppError.RateLimited -> stringResource(R.string.error_rate_limited)
    AppError.NoConnection -> stringResource(R.string.error_no_connection)
    AppError.Timeout -> stringResource(R.string.error_timeout)
    AppError.ContentBlocked -> stringResource(R.string.error_blocked)
    is AppError.Server -> stringResource(R.string.error_server, code)
    AppError.BadResponse -> stringResource(R.string.error_bad_response)
    AppError.Unknown -> stringResource(R.string.error_unknown)
}

/** Errors where retrying the same request can help. The others need the user to change something. */
val AppError.isRetryable: Boolean
    get() = this is AppError.RateLimited || this is AppError.NoConnection || this is AppError.Timeout ||
        this is AppError.Server || this is AppError.BadResponse || this is AppError.Unknown
