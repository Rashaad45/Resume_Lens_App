package com.resumelens.domain.repository

import com.resumelens.domain.model.AppResult
import com.resumelens.domain.model.ExtractedResume

/**
 * Takes the content Uri as a String so the domain layer and ViewModel tests
 * don't depend on android.net.Uri.
 */
interface ResumeTextExtractor {
    suspend fun extract(uri: String): AppResult<ExtractedResume>
}
