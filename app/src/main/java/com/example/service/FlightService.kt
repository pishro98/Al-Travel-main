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
data class AirportInfo(
    val name: String,
    val id: String,
    val time: String
)

@Serializable
data class FlightLeg(
    val departure_airport: AirportInfo,
    val arrival_airport: AirportInfo,
    val duration: Int,
    val airplane: String? = null,
    val airline: String,
    val airline_logo: String? = null,
    val travel_class: String? = null,
    val flight_number: String
)

@Serializable
data class BestFlight(
    val flights: List<FlightLeg>,
    val layovers: List<Map<String, String>>? = null,
    val total_duration: Int,
    val price: Int,
    val type: String,
    val airline_logo: String? = null
)

@Serializable
data class GoogleFlightsResponse(
    val best_flights: List<BestFlight>? = null,
    val other_flights: List<BestFlight>? = null
)

interface SerpApiGoogleFlights {
    @GET("search")
    suspend fun searchFlights(
        @Query("engine") engine: String = "google_flights",
        @Query("departure_id") departureId: String,
        @Query("arrival_id") arrivalId: String,
        @Query("outbound_date") outboundDate: String,
        @Query("currency") currency: String = "EUR",
        @Query("hl") hl: String = "de",
        @Query("api_key") apiKey: String
    ): GoogleFlightsResponse
}

object FlightClient {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: SerpApiGoogleFlights by lazy {
        Retrofit.Builder()
            .baseUrl("https://serpapi.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SerpApiGoogleFlights::class.java)
    }
}

class FlightService {
    suspend fun searchFlights(departure: String, arrival: String, date: String): Result<GoogleFlightsResponse> = withContext(Dispatchers.IO) {
        try {
            val apiKey = com.example.BuildConfig.SERP_API_KEY
            if (apiKey.isEmpty() || apiKey == "YOUR_SERP_API_KEY") {
                return@withContext Result.failure(Exception("Bitte fügen Sie einen SerpApi-Schlüssel in den AI Studio Secrets (bzw. local.properties/.env) unter dem Namen SERP_API_KEY hinzu, um Echtdaten abzurufen."))
            }
            
            val response = FlightClient.api.searchFlights(
                departureId = departure,
                arrivalId = arrival,
                outboundDate = date,
                apiKey = apiKey
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(Exception("Fehler beim Abrufen der Flugdaten: ${e.message}"))
        }
    }
}
