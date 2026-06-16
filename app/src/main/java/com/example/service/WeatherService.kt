package com.example.service

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

// Models for Open-Meteo Geocoding
@Serializable
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null
)

@Serializable
data class GeocodingResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String? = null
)

// Models for Open-Meteo Weather API
@Serializable
data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val current_weather: CurrentWeather? = null,
    val daily: DailyWeather? = null
)

@Serializable
data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double,
    val winddirection: Double,
    val weathercode: Int,
    val time: String
)

@Serializable
data class DailyWeather(
    val time: List<String>,
    val weathercode: List<Int>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val uv_index_max: List<Double>? = null,
    val precipitation_probability_max: List<Int>? = null
)

interface OpenMeteoGeocodingApi {
    @GET("v1/search")
    suspend fun searchCity(
        @Query("name") name: String,
        @Query("count") count: Int = 1,
        @Query("language") language: String = "de",
        @Query("format") format: String = "json"
    ): GeocodingResponse
}

interface OpenMeteoWeatherApi {
    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current_weather") currentWeather: Boolean = true,
        @Query("daily") daily: String = "weathercode,temperature_2m_max,temperature_2m_min,uv_index_max,precipitation_probability_max",
        @Query("timezone") timezone: String = "auto"
    ): WeatherResponse
}

object WeatherClient {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val geocodingApi: OpenMeteoGeocodingApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenMeteoGeocodingApi::class.java)
    }

    val weatherApi: OpenMeteoWeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenMeteoWeatherApi::class.java)
    }
}

class WeatherService {
    suspend fun getForecastForCity(cityName: String): Result<WeatherResponse> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Geocoding
            val geoResponse = WeatherClient.geocodingApi.searchCity(cityName)
            val topResult = geoResponse.results?.firstOrNull()
                ?: throw Exception("City not found: $cityName")
            
            // Step 2: Weather Forecast
            val weatherResponse = WeatherClient.weatherApi.getWeather(
                latitude = topResult.latitude,
                longitude = topResult.longitude
            )
            Result.success(weatherResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
