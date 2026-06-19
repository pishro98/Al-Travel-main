package com.example

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.app.Application

class TravelViewModel(application: Application) : AndroidViewModel(application) {
    private val database = com.example.data.AppDatabase.getDatabase(application)
    private val repository = com.example.data.TravelRepository(database.travelPlanDao(), com.example.service.TravelService())
    
    private val _uiState = MutableStateFlow<TravelUiState>(TravelUiState.Briefing)
    val uiState: StateFlow<TravelUiState> = _uiState.asStateFlow()
    
    private val _hasCachedPlan = MutableStateFlow(false)
    val hasCachedPlan = _hasCachedPlan.asStateFlow()
    
    val savedPlans: StateFlow<List<com.example.data.SavedTravelPlanEntity>> = repository.allSavedPlans
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    private val prefs = application.getSharedPreferences("user_profile_prefs", android.content.Context.MODE_PRIVATE)

    var liveWeather by mutableStateOf<com.example.service.WeatherResponse?>(null)
    
    fun fetchLiveWeather(city: String) {
        viewModelScope.launch {
            val result = com.example.service.WeatherService().getForecastForCity(city)
            result.onSuccess { weather ->
                liveWeather = weather
            }
        }
    }

    var liveFlights by mutableStateOf<com.example.service.GoogleFlightsResponse?>(null)
    var flightError by mutableStateOf<String?>(null)
    
    private fun normalizeAirportCode(input: String): String {
        val trimmed = input.trim()
        if (trimmed.contains("(") && trimmed.contains(")")) {
            return trimmed.substringAfter("(").substringBefore(")").trim()
        }
        val lowercase = trimmed.lowercase()
        val mapped = airportSuggestionsMap.entries.firstOrNull { 
            lowercase.contains(it.key) || it.key.startsWith(lowercase)
        }?.value?.first()
        if (mapped != null) {
             return mapped.substringAfter("(").substringBefore(")").trim()
        }
        return trimmed.take(3).uppercase()
    }

    fun fetchLiveFlights(dep: String, arr: String, date: String) {
        viewModelScope.launch {
            flightError = null
            liveFlights = null
            val normDep = normalizeAirportCode(dep)
            val normArr = normalizeAirportCode(arr)
            val result = com.example.service.FlightService().searchFlights(normDep, normArr, date)
            result.onSuccess {
                liveFlights = it
            }.onFailure {
                flightError = it.message ?: "Ein unbekannter Fehler ist aufgetreten."
            }
        }
    }

    var departure by mutableStateOf(prefs.getString("preferred_departure", "") ?: "")
    var destination by mutableStateOf("")
    var destinationAirport by mutableStateOf("")
    var dates by mutableStateOf("")
    var group by mutableStateOf("")
    var budget by mutableStateOf("")
    var style by mutableStateOf("")
    var extra by mutableStateOf("")
    
    // User profile
    var userProfileCountry by mutableStateOf(prefs.getString("country", "Deutschland") ?: "Deutschland")
    var userProfileHome by mutableStateOf(prefs.getString("home", "") ?: "")
    var userProfilePreferredDeparture by mutableStateOf(prefs.getString("preferred_departure", "") ?: "")
    var userProfileAirlines by mutableStateOf(prefs.getString("airlines", "") ?: "")
    var userProfileDiet by mutableStateOf(prefs.getString("diet", "") ?: "")

    var suggestionsLoading by mutableStateOf(false)
    var suggestions by mutableStateOf<List<DestinationSuggestion>>(emptyList())
    
    fun fetchSuggestions() {
        // We might want to reload if empty
        if (userProfileHome.isBlank()) return
        viewModelScope.launch {
            suggestionsLoading = true
            val result = repository.generateSuggestions(userProfileHome)
            suggestionsLoading = false
            result.onSuccess { 
                suggestions = it
            }
        }
    }

    // Map of common destinations to a list of airports (First is default)
    private val airportSuggestionsMap = mapOf(
        "mallorca" to listOf("Palma de Mallorca (PMI)"),
        "paris" to listOf("Paris-Charles-de-Gaulle (CDG)", "Paris-Orly (ORY)", "Beauvais-Tillé (BVA)"),
        "tokio" to listOf("Tokio Haneda (HND)", "Tokio Narita (NRT)"),
        "tokyo" to listOf("Tokio Haneda (HND)", "Tokio Narita (NRT)"),
        "rom" to listOf("Rom Fiumicino (FCO)", "Rom Ciampino (CIA)"),
        "rome" to listOf("Rom Fiumicino (FCO)", "Rom Ciampino (CIA)"),
        "bali" to listOf("Denpasar Bali (DPS)"),
        "new york" to listOf("John F. Kennedy (JFK)", "Newark Liberty (EWR)", "LaGuardia (LGA)"),
        "london" to listOf("London Heathrow (LHR)", "London Gatwick (LGW)", "London Stansted (STN)", "London Luton (LTN)"),
        "barcelona" to listOf("Barcelona El Prat (BCN)", "Girona (GRO)"),
        "bangkok" to listOf("Suvarnabhumi (BKK)", "Don Mueang (DMK)"),
        "dubai" to listOf("Dubai International (DXB)", "Al Maktoum (DWC)"),
        "berlin" to listOf("Berlin Brandenburg (BER)"),
        "münchen" to listOf("München (MUC)"),
        "munich" to listOf("München (MUC)"),
        "frankfurt" to listOf("Frankfurt (FRA)", "Frankfurt-Hahn (HHN)"),
        "amsterdam" to listOf("Amsterdam Schiphol (AMS)"),
        "wien" to listOf("Wien (VIE)"),
        "vienna" to listOf("Wien (VIE)"),
        "zürich" to listOf("Zürich (ZRH)"),
        "zurich" to listOf("Zürich (ZRH)"),
        "lisabon" to listOf("Lissabon Humberto Delgado (LIS)"),
        "lisbon" to listOf("Lissabon Humberto Delgado (LIS)"),
        "athen" to listOf("Athen Eleftherios Venizelos (ATH)"),
        "athens" to listOf("Athen Eleftherios Venizelos (ATH)"),
        "japan" to listOf("Tokio Haneda (HND)", "Tokio Narita (NRT)"),
        "spanien" to listOf("Palma de Mallorca (PMI)", "Barcelona El Prat (BCN)", "Madrid-Barajas (MAD)"),
        "spain" to listOf("Palma de Mallorca (PMI)", "Barcelona El Prat (BCN)", "Madrid-Barajas (MAD)")
    )

    fun updateSuggestedAirportForDestination(dest: String) {
        val normalized = dest.trim().lowercase()
        if (normalized.length < 3) {
            destinationAirport = ""
            return
        }
        val entry = airportSuggestionsMap.entries.firstOrNull { 
            normalized.contains(it.key) || it.key.startsWith(normalized)
        }
        if (entry != null) {
            destinationAirport = entry.value.first()
        } else {
            destinationAirport = ""
        }
    }

    fun getAlternativeAirportsForDestination(dest: String): List<String> {
        val normalized = dest.trim().lowercase()
        if (normalized.length < 3) return emptyList()
        val entry = airportSuggestionsMap.entries.firstOrNull { 
            normalized.contains(it.key) || it.key.startsWith(normalized)
        }
        return entry?.value ?: emptyList()
    }

    fun saveProfile() {
        prefs.edit().apply {
            putString("country", userProfileCountry)
            putString("home", userProfileHome)
            putString("preferred_departure", userProfilePreferredDeparture)
            putString("airlines", userProfileAirlines)
            putString("diet", userProfileDiet)
            apply()
        }
        if (departure.isBlank() && userProfilePreferredDeparture.isNotBlank()) {
            departure = userProfilePreferredDeparture
        }
    }

    fun updateProfileHome(value: String) {
        if (value != userProfileHome) {
            suggestions = emptyList()
        }
        userProfileHome = value
    }
    
    fun updateProfilePreferredDeparture(value: String) {
        userProfilePreferredDeparture = value
    }

    fun updateProfileCountry(value: String) {
        userProfileCountry = value
    }

    fun updateProfileAirlines(value: String) {
        userProfileAirlines = value
    }

    fun updateProfileDiet(value: String) {
        userProfileDiet = value
    }

    fun selectSavedPlan(entity: com.example.data.SavedTravelPlanEntity) {
        viewModelScope.launch {
            try {
                val jsonParser = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
                val plan = jsonParser.decodeFromString<TravelPlan>(entity.planData)
                destination = plan.destination
                dates = entity.dates
                budget = plan.totalBudget
                
                val loadedAirport = plan.flights.firstOrNull()?.outboundFlight?.arrivalAirport
                if (!loadedAirport.isNullOrBlank()) {
                    destinationAirport = loadedAirport
                } else {
                    updateSuggestedAirportForDestination(plan.destination)
                }
                
                _uiState.value = TravelUiState.Success(plan, isCached = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteSavedPlan(id: Int) {
        viewModelScope.launch {
            repository.deleteSavedPlan(id)
        }
    }

    init {
        viewModelScope.launch {
            if (repository.getCachedPlan() != null) {
                _hasCachedPlan.value = true
            }
        }
    }

    fun setBriefingState() {
        _uiState.value = TravelUiState.Briefing
    }

    fun loadCachedPlan() {
        viewModelScope.launch {
            val cached = repository.getCachedPlan()
            if (cached != null) {
                _uiState.value = TravelUiState.Success(cached, isCached = true)
            }
        }
    }

    fun generatePlan() {
        viewModelScope.launch {
            _uiState.value = TravelUiState.Loading
            
            val userProfileContext = "Heimatort: ${if (userProfileHome.isNotBlank()) userProfileHome else "Nicht angegeben"}, Wohnort/Heimatland: $userProfileCountry. Bevorzugte Airlines: ${if (userProfileAirlines.isNotBlank()) userProfileAirlines else "Keine Präferenz"}. Essenspräferenzen: ${if (userProfileDiet.isNotBlank()) userProfileDiet else "Keine Präferenz"}."
            val enhancedExtra = if (extra.isNotBlank()) "$extra. $userProfileContext" else userProfileContext

            val result = repository.generateTravelPlan(
                departure = departure,
                destination = if (destinationAirport.isNotBlank()) "$destination ($destinationAirport)" else destination,
                dates = dates,
                group = group,
                budget = budget,
                style = style,
                extra = enhancedExtra
            )
            
            result.onSuccess { plan ->
                _hasCachedPlan.value = true
                _uiState.value = TravelUiState.Success(plan, isCached = false)
            }.onFailure { e ->
                val errorMsg = when (e) {
                    is retrofit2.HttpException -> {
                        val errorBody = e.response()?.errorBody()?.string() ?: ""
                        when (e.code()) {
                            429 -> "API-Rate-Limit überschritten. Bitte warten Sie einen Moment."
                            400 -> "Ungültige API-Anfrage. Details: $errorBody"
                            503 -> "Es gibt aktuell sehr viele Anfragen. Bitte versuchen Sie es in wenigen Minuten erneut."
                            else -> "HTTP-Fehler ${e.code()} bei Modell ${e.response()?.raw()?.request?.url}: $errorBody"
                        }
                    }
                    is kotlinx.serialization.SerializationException -> "Die KI hat fehlerhafte Daten zurückgegeben: \n${e.message}"
                    is java.net.SocketTimeoutException -> "Das Erstellen des Reiseplans hat zu lange gedauert (Timeout). Bitte versuchen Sie es erneut."
                    is java.net.UnknownHostException -> "Der Emulator hat keine Internetverbindung oder die API wird blockiert. Bitte versuchen Sie es erneut."
                    else -> e.message ?: "Ein unbekannter Fehler ist aufgetreten."
                }
                _uiState.value = TravelUiState.Error(errorMsg)
            }
        }
    }
}

sealed class TravelUiState {
    object Briefing : TravelUiState()
    object Loading : TravelUiState()
    data class Success(val plan: TravelPlan, val isCached: Boolean = false) : TravelUiState()
    data class Error(val message: String) : TravelUiState()
}
