package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class WeatherDayForecast(
    val day: String = "",
    val condition: String = "",
    val icon: String = "", // e.g. "sunny", "cloudy", "rainy", "windy", "thunderstorm", "partly_cloudy"
    val temperature: String = ""
)

@Serializable
data class WeatherForecast(
    val generalDescription: String = "",
    val averageTemperature: String = "",
    val forecastDays: List<WeatherDayForecast> = emptyList()
)

@Serializable
data class FlightSegmentDetails(
    val departureAirport: String = "",
    val arrivalAirport: String = "",
    val departureTime: String = "",
    val arrivalTime: String = "",
    val duration: String = "",
    val stops: Int = 0
)

@Serializable
data class DestinationSuggestion(
    val id: String = "",
    val destination: String = "",
    val subtitle: String = "",
    val description: String = "",
    val imageUrl: String = ""
)

@Serializable
data class TravelPlan(
    val destination: String = "",
    val totalBudget: String = "",
    val description: String = "",
    val overview: Overview = Overview(),
    val flights: List<Flight> = emptyList(),
    val hotels: List<Hotel> = emptyList(),
    val activities: List<Activity> = emptyList(),
    val itineraryDays: List<ItineraryDay> = emptyList(),
    val budgetBreakdown: List<BudgetItem> = emptyList(),
    val tips: List<Tip> = emptyList(),
    val weatherForecast: WeatherForecast? = null
)

@Serializable data class Overview(val flightBudget: Int = 0, val hotelBudget: Int = 0, val activityBudget: Int = 0, val bufferBudget: Int = 0)
@Serializable data class Flight(
    val airline: String = "",
    val price: String = "", // Price per person, e.g. "350 €"
    val duration: String = "", // Outbound duration or combined duration
    val isDirect: Boolean = false,
    val bestPick: Boolean = false,
    val url: String = "",
    val isRoundTrip: Boolean = true,
    val outboundFlight: FlightSegmentDetails? = null,
    val returnFlight: FlightSegmentDetails? = null,
    val passengerCount: Int = 1,
    val totalPrice: String = "" // Total price for all passengers combined
)
@Serializable data class Hotel(val name: String = "", val pricePerNight: String = "", val totalPrice: String = "", val rating: String = "", val location: String = "", val bestPick: Boolean = false, val pros: List<String> = emptyList(), val cons: List<String> = emptyList(), val url: String = "")
@Serializable data class Activity(val title: String = "", val description: String = "", val price: String = "", val duration: String = "", val rating: String = "", val isMustDo: Boolean = false, val url: String = "")
@Serializable data class ItineraryDay(val dayNumber: Int = 0, val morning: String = "", val afternoon: String = "", val evening: String = "")
@Serializable data class BudgetItem(val category: String = "", val amount: String = "")
@Serializable data class Tip(val title: String = "", val description: String = "")
