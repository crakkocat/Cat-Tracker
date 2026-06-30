package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- DATA CLASSES FOR GEMINI API (MOSHI CHOSEN OVER KOTLINX.SERIALIZATION AS MOSHI IS INCLUDED IN PRESET) ---

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<ContentPart>
)

@JsonClass(generateAdapter = true)
data class ContentPart(
    val parts: List<TextPart>
)

@JsonClass(generateAdapter = true)
data class TextPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<CandidatePart>?
)

@JsonClass(generateAdapter = true)
data class CandidatePart(
    val content: ContentPart?
)

// --- RETROFIT CLIENT CONFIGURATION ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

// --- API WRAPPER FUNCTION ---

object GeminiService {
    
    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
    }

    suspend fun getCatAdvice(prompt: String): String {
        if (!isApiKeyConfigured()) {
            return "API_KEY_MISSING"
        }

        val request = GeminiRequest(
            contents = listOf(
                ContentPart(
                    parts = listOf(
                        TextPart(text = prompt)
                    )
                )
            )
        )

        return try {
            val response = GeminiRetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No analysis generated. Your feline buddy looks great, but try asking again shortly!"
        } catch (e: Exception) {
            "API_ERROR: ${e.localizedMessage ?: "Unknown error connecting to Gemini Service"}"
        }
    }
}
