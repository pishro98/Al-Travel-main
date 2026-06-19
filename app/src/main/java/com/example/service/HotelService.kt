package com.example.service

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@Serializable data class HotelRatePerNight(
    val lowest: String? = null, val before_taxes_fees: String? = null
)
@Serializable data class HotelTotalRate(
    val lowest: String? = null, val before_taxes_fees: String? = null
)
@Serializable data class HotelProperty(
    val name: String = "",
    val link: String? = null,
    val check_in_time: String? = null,
    val check_out_time: String? = null,
    val rate_per_night: HotelRatePerNight? = null,
    val total_rate: HotelTotalRate? = null,
    val overall_rating: Double? = null,
    val reviews: Int? = null,
    val hotel_class: String? = null,
    val amenities: List<String>? = null,
    val description: String? = null,
    val thumbnail: String? = null
)
@Serializable data class GoogleHotelsResponse(
    val properties: List<HotelProperty>? = null
)

interface SerpApiHotelsEndpoint {
    @GET("search")
    suspend fun searchHotels(
        @Query("engine") engine: String = "google_hotels",
        @Query("q") query: String,
        @Query("check_in_date") checkInDate: String,
        @Query("check_out_date") checkOutDate: String,
        @Query("currency") currency: String = "EUR",
        @Query("hl") hl: String = "de",
        @Query("gl") gl: String = "de",
        @Query("adults") adults: Int = 2,
        @Query("sort_by") sortBy: Int = 8, // 8 = highest rating
        @Query("api_key") apiKey: String
    ): GoogleHotelsResponse
}

object HotelClient {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    val api: SerpApiHotelsEndpoint by lazy {
        Retrofit.Builder()
            .baseUrl("https://serpapi.com/")
            .client(OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS).build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(SerpApiHotelsEndpoint::class.java)
    }
}

class HotelService {
    suspend fun searchHotels(
        destination: String, checkInDate: String, checkOutDate: String, adults: Int = 2
    ): Result<GoogleHotelsResponse> = withContext(Dispatchers.IO) {
        try {
            val key = BuildConfig.SERP_API_KEY
            if (key.isBlank() || key == "YOUR_SERP_API_KEY")
                return@withContext Result.failure(Exception("SERP_API_KEY fehlt in .env"))
            Result.success(HotelClient.api.searchHotels(
                query = "Hotels in $destination",
                checkInDate = checkInDate,
                checkOutDate = checkOutDate,
                adults = adults,
                apiKey = key
            ))
        } catch (e: Exception) {
            Result.failure(Exception("Hotel-Fehler: ${e.message}"))
        }
    }
}
