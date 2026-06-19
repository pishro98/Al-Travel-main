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
        val aiResult = travelService.generateTravelPlan(departure, destination, dates, group, budget, style, extra)
        
        if (aiResult.isFailure) return aiResult

        val aiPlan = aiResult.getOrThrow()
        
        val finalPlan = enrichPlanWithRealData(aiPlan, dates, departure)

        // Save and return
        saveCachedPlan(finalPlan)
        saveToHistory(finalPlan, dates)

        return Result.success(finalPlan)
    }

    private suspend fun enrichPlanWithRealData(aiPlan: TravelPlan, dates: String, departure: String): TravelPlan {
        val destinationCity = aiPlan.destination.substringBefore("(").trim()
        val destinationAirport = aiPlan.destination.substringAfter("(").substringBefore(")").trim()

        // --- REAL WEATHER (Open-Meteo, free, no API key required) ---
        val realWeather = try {
            weatherService.getForecastForCity(destinationCity).getOrNull()?.let { w ->
                val daily = w.daily
                WeatherForecast(
                    generalDescription = "Echtzeit-Wetterdaten via Open-Meteo für $destinationCity",
                    averageTemperature = "${w.current_weather?.temperature?.toInt() ?: "?"}°C",
                    forecastDays = daily?.time?.take(7)?.mapIndexed { i, date ->
                        val code = daily.weathercode.getOrElse(i) { 0 }
                        WeatherDayForecast(
                            day = date,
                            condition = wmoCodeToCondition(code),
                            icon = wmoCodeToIcon(code),
                            temperature = "${daily.temperature_2m_max.getOrElse(i){0.0}.toInt()}°" +
                                        "/${daily.temperature_2m_min.getOrElse(i){0.0}.toInt()}°C"
                        )
                    } ?: emptyList()
                )
            }
        } catch (e: Exception) { null }

        // --- REAL FLIGHTS (SerpAPI Google Flights) ---
        val realFlights = try {
            val parts = dates.split("-").map { it.trim() }
            val checkIn = parseDateToIso(parts.firstOrNull() ?: "")
            val depCode = departure.substringAfter("(").substringBefore(")").trim()
                .ifBlank { departure.take(3).uppercase() }
            val arrCode = destinationAirport.ifBlank {
                aiPlan.destination.substringAfter("(").substringBefore(")").trim()
            }
            if (depCode.length == 3 && arrCode.length == 3 && checkIn.isNotBlank()) {
                flightService.searchFlights(depCode, arrCode, checkIn).getOrNull()
                    ?.best_flights?.take(3)?.map { bf ->
                        val leg = bf.flights.firstOrNull()
                        Flight(
                            airline = leg?.airline ?: "?",
                            price = "${bf.price} €",
                            duration = "${bf.total_duration / 60}h ${bf.total_duration % 60}min",
                            isDirect = bf.flights.size == 1,
                            bestPick = bf == flightService.searchFlights(depCode, arrCode, checkIn).getOrNull()?.best_flights?.firstOrNull(),
                            url = "https://www.google.com/travel/flights",
                            isRoundTrip = true,
                            passengerCount = 1,
                            outboundFlight = leg?.let { l ->
                                FlightSegmentDetails(
                                    departureAirport = l.departure_airport.id,
                                    arrivalAirport = l.arrival_airport.id,
                                    departureTime = l.departure_airport.time,
                                    arrivalTime = l.arrival_airport.time,
                                    duration = "${l.duration / 60}h ${l.duration % 60}min",
                                    stops = bf.flights.size - 1
                                )
                            }
                        )
                    } ?: emptyList()
            } else emptyList()
        } catch (e: Exception) { emptyList() }

        // --- REAL HOTELS (SerpAPI Google Hotels) ---
        val realHotels = try {
            val parts = dates.split("-").map { it.trim() }
            val checkIn  = parseDateToIso(parts.firstOrNull() ?: "")
            val checkOut = parseDateToIso(parts.getOrNull(1) ?: "")
            if (checkIn.isNotBlank() && checkOut.isNotBlank()) {
                com.example.service.HotelService().searchHotels(destinationCity, checkIn, checkOut).getOrNull()
                    ?.properties?.take(3)?.map { p ->
                        Hotel(
                            name = p.name,
                            pricePerNight = p.rate_per_night?.lowest ?: "Preis auf Anfrage",
                            totalPrice = p.total_rate?.lowest ?: "",
                            rating = p.overall_rating?.let { "%.1f".format(it) } ?: "N/A",
                            location = destinationCity,
                            bestPick = p == com.example.service.HotelService().searchHotels(destinationCity, checkIn, checkOut).getOrNull()?.properties?.firstOrNull(),
                            pros = p.amenities?.take(3) ?: emptyList(),
                            cons = emptyList(),
                            url = p.link ?: "https://www.google.com/travel/hotels"
                        )
                    } ?: emptyList()
            } else emptyList()
        } catch (e: Exception) { emptyList() }

        return aiPlan.copy(
            weatherForecast = realWeather ?: aiPlan.weatherForecast,
            flights = realFlights.ifEmpty { aiPlan.flights },
            hotels = realHotels.ifEmpty { aiPlan.hotels }
        )
    }

    private fun parseDateToIso(input: String): String {
        val parts = input.trim().split(".")
        return if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else input
    }
    
    private fun wmoCodeToCondition(code: Int) = when (code) {
        0 -> "Klar und sonnig"; 1, 2 -> "Überwiegend klar"; 3 -> "Bedeckt"
        45, 48 -> "Neblig"; 51, 53, 55 -> "Nieselregen"; 61, 63, 65 -> "Regen"
        71, 73, 75 -> "Schneefall"; 80, 81, 82 -> "Regenschauer"
        95, 96, 99 -> "Gewitter"; else -> "Wechselhaft"
    }
    
    private fun wmoCodeToIcon(code: Int) = when (code) {
        0 -> "sunny"; 1, 2 -> "partly_cloudy"; 3 -> "cloudy"
        45, 48, 51, 53, 55, 61, 63, 65, 80, 81, 82 -> "rainy"
        71, 73, 75 -> "cloudy"; 95, 96, 99 -> "thunderstorm"; else -> "partly_cloudy"
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
