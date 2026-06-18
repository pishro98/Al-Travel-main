package com.example.data

import com.example.model.*
import com.example.service.TravelService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TravelRepository(
    private val travelPlanDao: TravelPlanDao,
    private val travelService: TravelService,
    private val weatherService: com.example.service.WeatherService = com.example.service.WeatherService(),
    private val localSearchService: com.example.service.LocalSearchService = com.example.service.LocalSearchService(),
    private val flightService: com.example.service.FlightService = com.example.service.FlightService()
) {
    val allSavedPlans: kotlinx.coroutines.flow.Flow<List<SavedTravelPlanEntity>> = travelPlanDao.getAllSavedPlans()

    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true 
        coerceInputValues = true 
    }

    suspend fun generateTravelPlan(
        departure: String,
        destination: String,
        dates: String,
        group: String,
        budget: String,
        style: String,
        extra: String
    ): Result<TravelPlan> {
        val result = travelService.generateTravelPlan(departure, destination, dates, group, budget, style, extra)
        
        return result.map { originalPlan ->
            var augmentedPlan = originalPlan

            // 1. Fetch real Weather Data
            weatherService.getForecastForCity(destination).onSuccess { weatherResponse ->
                val daily = weatherResponse.daily
                if (daily != null && daily.time.isNotEmpty()) {
                    val days = daily.time.indices.map { i ->
                        val code = daily.weathercode.getOrNull(i) ?: 0
                        val maxT = daily.temperature_2m_max.getOrNull(i)?.toInt() ?: 0
                        val minT = daily.temperature_2m_min.getOrNull(i)?.toInt() ?: 0
                        val condition = when (code) {
                            0, 1 -> "Sonnig"
                            2 -> "Leicht bewölkt"
                            3 -> "Bewölkt"
                            45, 48 -> "Nebel"
                            51, 53, 55, 61, 63, 65 -> "Regen"
                            71, 73, 75 -> "Schnee"
                            95, 96, 99 -> "Gewitter"
                            else -> "Wechselhaft"
                        }
                        WeatherDayForecast(
                            day = daily.time[i],
                            condition = condition,
                            icon = "weather_icon", 
                            temperature = "${minT}° / ${maxT}°"
                        )
                    }
                    augmentedPlan = augmentedPlan.copy(
                        weatherForecast = WeatherForecast(
                            generalDescription = "Echtzeit-Wettervorhersage für $destination",
                            averageTemperature = "${daily.temperature_2m_max.average().toInt()}°C max avg",
                            forecastDays = days.take(7) 
                        )
                    )
                }
            }

            // 2. Fetch real Hotels
            localSearchService.searchLocalPlaces("Hotels in $destination").onSuccess { serpResponse ->
                val places = serpResponse.local_results?.places ?: emptyList()
                val realHotels = places.take(3).map { place ->
                    Hotel(
                        name = place.title ?: "Hotel",
                        pricePerNight = place.price ?: "Preise auf Anfrage",
                        totalPrice = "",
                        rating = place.rating?.toString() ?: "N/A",
                        location = place.address ?: destination,
                        bestPick = true,
                        pros = emptyList(),
                        cons = emptyList(),
                        url = "https://www.google.com/search?q=Hotel+${place.title?.replace(" ", "+")}+${destination.replace(" ", "+")}"
                    )
                }
                
                if (realHotels.isNotEmpty()) {    
                    augmentedPlan = augmentedPlan.copy(hotels = realHotels)
                }
            }

            // 3. Fix activity URLs and add Google Flights Link
            val fixedActivities = augmentedPlan.activities.map { act ->
                act.copy(url = "https://www.google.com/search?q=${act.title.replace(" ", "+")}+${destination.replace(" ", "+")}")
            }
            
            val newFlights = if (augmentedPlan.flights.isEmpty()) {
                listOf(
                    Flight(
                        airline = "Flugsuche via Google",
                        price = "Preise prüfen",
                        duration = "N/A",
                        url = "https://www.google.com/travel/flights?q=Flights+from+${departure.replace(" ", "+")}+to+${destination.replace(" ", "+")}"
                    )
                )
            } else {
                augmentedPlan.flights.map { flight ->
                    flight.copy(url = "https://www.google.com/travel/flights?q=Flights+from+${departure.replace(" ", "+")}+to+${destination.replace(" ", "+")}")
                }
            }

            augmentedPlan = augmentedPlan.copy(
                activities = fixedActivities,
                flights = newFlights
            )

            // Save and return
            saveCachedPlan(augmentedPlan)
            saveToHistory(augmentedPlan, dates)
            
            augmentedPlan
        }
    }

    suspend fun generateSuggestions(homeCity: String): Result<List<DestinationSuggestion>> {
        return travelService.generateSuggestions(homeCity)
    }

    suspend fun saveToHistory(plan: TravelPlan, dates: String) {
        try {
            val jsonStr = json.encodeToString(plan)
            travelPlanDao.insertSavedPlan(
                SavedTravelPlanEntity(
                    destination = if (plan.destination.isNotBlank()) plan.destination else "Unbekannt",
                    dates = dates,
                    totalBudget = if (plan.totalBudget.isNotBlank()) plan.totalBudget else "",
                    planData = jsonStr
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteSavedPlan(id: Int) {
        try {
            travelPlanDao.deleteSavedPlanById(id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveCachedPlan(plan: TravelPlan) {
        try {
            val jsonStr = json.encodeToString(plan)
            travelPlanDao.insertPlan(TravelPlanEntity(planData = jsonStr))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getCachedPlan(): TravelPlan? {
        val entity = travelPlanDao.getCachedPlan() ?: return null
        return try {
            json.decodeFromString<TravelPlan>(entity.planData)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
