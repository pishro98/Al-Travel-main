package com.example.service

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
data class SearchParameters(
    val engine: String? = null,
    val q: String? = null,
    val google_domain: String? = null,
    val device: String? = null
)

@Serializable
data class GpsCoordinates(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class LocalPlace(
    val position: Int? = null,
    val rating: Double? = null,
    val reviews: Int? = null,
    val price: String? = null,
    val description: String? = null,
    val thumbnail: String? = null,
    val thumbnail_large: String? = null,
    val place_id: String? = null,
    val title: String? = null,
    val type: String? = null,
    val address: String? = null,
    val gps_coordinates: GpsCoordinates? = null
)

@Serializable
data class LocalResults(
    val places: List<LocalPlace>? = null,
    val more_locations_link: String? = null
)

@Serializable
data class KnowledgeGraph(
    val title: String? = null,
    val type: String? = null,
    val description: String? = null
)

@Serializable
data class SerpApiGoogleSearchResponse(
    val search_parameters: SearchParameters? = null,
    val local_results: LocalResults? = null,
    val knowledge_graph: KnowledgeGraph? = null
)

interface SerpApiGoogleSearch {
    @GET("search")
    suspend fun searchLocalPlaces(
        @Query("engine") engine: String = "google",
        @Query("q") query: String,
        @Query("api_key") apiKey: String,
        @Query("hl") hl: String = "de",
        @Query("gl") gl: String = "de"
    ): SerpApiGoogleSearchResponse
}

object LocalSearchClient {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: SerpApiGoogleSearch by lazy {
        Retrofit.Builder()
            .baseUrl("https://serpapi.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SerpApiGoogleSearch::class.java)
    }
}

class LocalSearchService {
    suspend fun searchLocalPlaces(query: String): Result<SerpApiGoogleSearchResponse> = withContext(Dispatchers.IO) {
        try {
            val apiKey = com.example.BuildConfig.SERP_API_KEY
            if (apiKey.isEmpty() || apiKey == "YOUR_SERP_API_KEY") {
                return@withContext Result.failure(Exception("Bitte SerpApi-Schlüssel in den Secrets hinterlegen."))
            }
            
            val response = LocalSearchClient.api.searchLocalPlaces(
                query = query,
                apiKey = apiKey
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(Exception("Fehler beim Abrufen der lokalen Ergebnisse: ${e.message}"))
        }
    }
}
