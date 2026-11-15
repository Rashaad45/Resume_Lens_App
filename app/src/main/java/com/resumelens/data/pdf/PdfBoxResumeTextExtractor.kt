package com.resumelens.data.pdf

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.resumelens.di.IoDispatcher
import com.resumelens.domain.model.AppError
import com.resumelens.domain.model.AppResult
import com.resumelens.domain.model.ExtractedResume
import com.resumelens.domain.repository.ResumeTextExtractor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads a PDF the user picked through the Storage Access Framework. SAF gives a content:// Uri
 * with a one-time read grant, so the app needs no storage permission. The text is extracted
 * right away and the Uri is never kept.
 */
@Singleton
class PdfBoxResumeTextExtractor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : ResumeTextExtractor {

    @Volatile private var pdfBoxReady = false

    override suspend fun extract(uri: String): AppResult<ExtractedResume> = withContext(io) {
        ensurePdfBox()
        val contentUri = Uri.parse(uri)
        val (name, size) = queryNameAndSize(contentUri)

        if (size != null && size > MAX_BYTES) return@withContext AppResult.Failure(AppError.PdfTooLarge)

        try {
            val stream = context.contentResolver.openInputStream(contentUri)
                ?: return@withContext AppResult.Failure(AppError.PdfUnreadable)
            stream.use { input ->
                PDDocument.load(input).use { doc ->
                    val text = PDFTextStripper().apply { sortByPosition = true }.getText(doc)
                    val cleaned = text.lines().joinToString("\n") { it.trimEnd() }.trim()
                    if (cleaned.length < MIN_TEXT_CHARS) {
                        AppResult.Failure(AppError.PdfNoText) // likely a scanned/image-only PDF
                    } else {
                        AppResult.Success(ExtractedResume(name ?: "resume.pdf", doc.numberOfPages, cleaned))
                    }
                }
            }
        } catch (_: InvalidPasswordException) {
            AppResult.Failure(AppError.PdfEncrypted)
        } catch (_: IOException) {
            AppResult.Failure(AppError.PdfUnreadable)
        } catch (_: SecurityException) {
            AppResult.Failure(AppError.PdfUnreadable) // the Uri grant was revoked
        } catch (_: OutOfMemoryError) {
            AppResult.Failure(AppError.PdfTooLarge)
        }
    }

    private fun ensurePdfBox() {
        if (!pdfBoxReady) {
            PDFBoxResourceLoader.init(context)
            pdfBoxReady = true
        }
    }

    private fun queryNameAndSize(uri: Uri): Pair<String?, Long?> = try {
        context.contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null,
        )?.use { c ->
            if (!c.moveToFirst()) return null to null
            val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
            val name = if (nameIdx >= 0) c.getString(nameIdx) else null
            val size = if (sizeIdx >= 0 && !c.isNull(sizeIdx)) c.getLong(sizeIdx) else null
            name to size
        } ?: (null to null)
    } catch (_: Exception) {
        null to null
    }

    private companion object {
        const val MAX_BYTES = 10L * 1024 * 1024
        const val MIN_TEXT_CHARS = 50
    }
}
