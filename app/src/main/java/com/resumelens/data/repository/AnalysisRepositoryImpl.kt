package com.resumelens.data.repository

import com.resumelens.data.local.AnalysisDao
import com.resumelens.data.local.AnalysisMapper
import com.resumelens.data.remote.AnalysisResponseParser
import com.resumelens.data.remote.ErrorMapper
import com.resumelens.data.remote.GeminiApi
import com.resumelens.data.remote.GeminiConfig
import com.resumelens.data.remote.PromptBuilder
import com.resumelens.domain.Clock
import com.resumelens.domain.model.Analysis
import com.resumelens.domain.model.AppError
import com.resumelens.domain.model.AppResult
import com.resumelens.domain.repository.AnalysisRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class AnalysisRepositoryImpl @Inject constructor(
    private val api: GeminiApi,
    private val config: GeminiConfig,
    private val promptBuilder: PromptBuilder,
    private val parser: AnalysisResponseParser,
    private val dao: AnalysisDao,
    private val mapper: AnalysisMapper,
    private val clock: Clock,
) : AnalysisRepository {

    override suspend fun analyze(
        resumeText: String,
        jobDescription: String,
        resumeFileName: String,
    ): AppResult<Long> {
        if (config.apiKey.isBlank()) return AppResult.Failure(AppError.MissingApiKey)

        val response = try {
            api.generateContent(config.model, promptBuilder.build(resumeText, jobDescription))
        } catch (e: CancellationException) {
            throw e // never swallow cancellation; it would break structured concurrency
        } catch (e: Exception) {
            return AppResult.Failure(ErrorMapper.map(e))
        }

        return when (val parsed = parser.parse(response)) {
            is AppResult.Failure -> parsed
            is AppResult.Success -> {
                val analysis = Analysis(
                    id = 0,
                    createdAtMillis = clock.nowMillis(),
                    resumeFileName = resumeFileName,
                    jobTitle = parsed.value.jobTitle,
                    result = parsed.value.result,
                )
                AppResult.Success(dao.insert(mapper.toEntity(analysis)))
            }
        }
    }

    override fun observeHistory(): Flow<List<Analysis>> =
        dao.observeAll().map { rows -> rows.mapNotNull(mapper::toDomain) }

    override fun observeAnalysis(id: Long): Flow<Analysis?> =
        dao.observeById(id).map { it?.let(mapper::toDomain) }

    override suspend fun delete(analysis: Analysis) = dao.deleteById(analysis.id)

    override suspend fun restore(analysis: Analysis) {
        dao.insert(mapper.toEntity(analysis))
    }
}
