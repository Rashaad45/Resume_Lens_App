package com.resumelens.di

import android.content.Context
import androidx.room.Room
import com.resumelens.BuildConfig
import com.resumelens.data.local.AnalysisDao
import com.resumelens.data.local.AppDatabase
import com.resumelens.data.pdf.PdfBoxResumeTextExtractor
import com.resumelens.data.remote.GeminiApi
import com.resumelens.data.remote.GeminiConfig
import com.resumelens.data.repository.AnalysisRepositoryImpl
import com.resumelens.domain.Clock
import com.resumelens.domain.repository.AnalysisRepository
import com.resumelens.domain.repository.ResumeTextExtractor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/** Injecting the dispatcher (instead of hard-coding Dispatchers.IO) lets tests swap in a test dispatcher. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true // tolerate new fields from the API
        isLenient = true
        coerceInputValues = true // e.g. null for a non-null field falls back to its default
        explicitNulls = false // omit nulls in requests; Gemini rejects some null fields
    }

    @Provides
    fun provideClock(): Clock = Clock { System.currentTimeMillis() }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    @Provides @Singleton
    fun provideGeminiConfig(): GeminiConfig =
        GeminiConfig(apiKey = BuildConfig.GEMINI_API_KEY, model = BuildConfig.GEMINI_MODEL)

    @Provides @Singleton
    fun provideOkHttp(config: GeminiConfig): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS) // "thinking" models can take a while on long inputs
            .callTimeout(120, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("x-goog-api-key", config.apiKey).build())
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    // BASIC logs method/URL/status only; headers (the API key) and bodies (the resume) are never logged.
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                }
            }
            .build()

    @Provides @Singleton
    fun provideGeminiApi(client: OkHttpClient, json: Json): GeminiApi =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    fun provideAnalysisDao(db: AppDatabase): AnalysisDao = db.analysisDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds
    abstract fun bindAnalysisRepository(impl: AnalysisRepositoryImpl): AnalysisRepository

    @Binds
    abstract fun bindExtractor(impl: PdfBoxResumeTextExtractor): ResumeTextExtractor
}
