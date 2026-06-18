package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

// --- Travel Data Models ---
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

// --- ViewModel ---

class TravelViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
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
    
    fun fetchLiveFlights(dep: String, arr: String, date: String) {
        viewModelScope.launch {
            flightError = null
            liveFlights = null
            val result = com.example.service.FlightService().searchFlights(dep, arr, date)
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
        saveProfile()
    }
    
    fun updateProfilePreferredDeparture(value: String) {
        userProfilePreferredDeparture = value
        saveProfile()
    }

    fun updateProfileCountry(value: String) {
        userProfileCountry = value
        saveProfile()
    }

    fun updateProfileAirlines(value: String) {
        userProfileAirlines = value
        saveProfile()
    }

    fun updateProfileDiet(value: String) {
        userProfileDiet = value
        saveProfile()
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

// --- UI Components ---
@Composable
fun IosTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            readOnly = readOnly,
            enabled = enabled,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BriefingScreen(viewModel: TravelViewModel) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    
    if (showProfileDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Mein Reise-Profil") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    IosTextField(
                        value = viewModel.userProfileHome,
                        onValueChange = { viewModel.updateProfileHome(it) },
                        label = "Heimatort (Stadt)",
                        placeholder = "z.B. Berlin"
                    )
                    IosTextField(
                        value = viewModel.userProfilePreferredDeparture,
                        onValueChange = { viewModel.updateProfilePreferredDeparture(it) },
                        label = "Bevorzugter Abflughafen",
                        placeholder = "z.B. Frankfurt (FRA)"
                    )
                    IosTextField(
                        value = viewModel.userProfileCountry,
                        onValueChange = { viewModel.updateProfileCountry(it) },
                        label = "Wohnort/Heimatland",
                        placeholder = "z.B. Deutschland"
                    )
                    IosTextField(
                        value = viewModel.userProfileAirlines,
                        onValueChange = { viewModel.updateProfileAirlines(it) },
                        label = "Bevorzugte Airlines",
                        placeholder = "z.B. Lufthansa, Emirates"
                    )
                    IosTextField(
                        value = viewModel.userProfileDiet,
                        onValueChange = { viewModel.updateProfileDiet(it) },
                        label = "Besondere Essenswünsche",
                        placeholder = "z.B. Vegetarisch, Halal"
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showProfileDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
    
    if (showDatePicker) {
        val context = LocalContext.current
        val config = LocalConfiguration.current
        val localizedContext = remember(context) {
            val configuration = android.content.res.Configuration(context.resources.configuration)
            configuration.setLocale(java.util.Locale.GERMAN)
            context.createConfigurationContext(configuration)
        }
        val localizedConfig = remember(config) {
            android.content.res.Configuration(config).apply {
                setLocale(java.util.Locale.GERMAN)
            }
        }
        
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalConfiguration provides localizedConfig
        ) {
            val datePickerState = androidx.compose.material3.rememberDateRangePickerState()
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showDatePicker = false },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { showDatePicker = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Schließen")
                            }
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    val startMillis = datePickerState.selectedStartDateMillis
                                    val endMillis = datePickerState.selectedEndDateMillis
                                    val formatter = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.GERMAN)
                                    if (startMillis != null && endMillis != null) {
                                        val startDate = formatter.format(java.util.Date(startMillis))
                                        val endDate = formatter.format(java.util.Date(endMillis))
                                        viewModel.dates = "$startDate - $endDate"
                                    } else if (startMillis != null) {
                                        viewModel.dates = formatter.format(java.util.Date(startMillis))
                                    }
                                    showDatePicker = false
                                }
                            ) {
                                Text("Speichern", fontWeight = FontWeight.Bold)
                            }
                        }
                        androidx.compose.material3.DateRangePicker(
                            state = datePickerState,
                            modifier = Modifier.weight(1f),
                            title = {
                                Text(
                                    "Reisezeitraum",
                                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp),
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            headline = { /* Use default headline or hide if preferred */ }
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TopAppBar(
                    title = { },
                    modifier = Modifier.widthIn(max = 700.dp),
                    actions = {
                        androidx.compose.material3.IconButton(onClick = { showProfileDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Profil Einstellungen")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("Reise planen", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
                
                IosTextField(
                    value = viewModel.departure,
                    onValueChange = { viewModel.departure = it },
                    label = "Von wo fliegst du? (Abflugort)",
                    placeholder = "z.B. Frankfurt, Berlin, München..."
                )
                
                IosTextField(
                    value = viewModel.destination,
                    onValueChange = { 
                        viewModel.destination = it 
                        viewModel.updateSuggestedAirportForDestination(it)
                    },
                    label = "Wohin soll die Reise gehen?",
                    placeholder = "z.B. Bali, Tokio..."
                )

                if (viewModel.destination.isNotBlank()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        IosTextField(
                            value = viewModel.destinationAirport,
                            onValueChange = { viewModel.destinationAirport = it },
                            label = "Zugehöriger Zielflughafen",
                            placeholder = "z.B. Palma de Mallorca (PMI) oder leer lassen"
                        )
                        
                        val alternatives = remember(viewModel.destination) { 
                            viewModel.getAlternativeAirportsForDestination(viewModel.destination) 
                        }
                        if (alternatives.isNotEmpty()) {
                            Text(
                                text = "ALTERNATIVE FLUGHÄFEN AUSWÄHLEN",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                            )
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(alternatives.size) { idx ->
                                    val altAirport = alternatives[idx]
                                    val isSelected = viewModel.destinationAirport.trim().lowercase() == altAirport.trim().lowercase()
                                    androidx.compose.material3.InputChip(
                                        selected = isSelected,
                                        onClick = { viewModel.destinationAirport = altAirport },
                                        label = { Text(altAirport, style = MaterialTheme.typography.bodySmall) }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Suggested Destinations Row
                Text(
                    text = "BELIEBTE REISEZIELE",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                )
                
                val suggestions = remember {
                    listOf(
                        "Mallorca" to "Mallorca 🏖️",
                        "Paris" to "Paris 🗼",
                        "Tokio" to "Tokio 🍣",
                        "Rom" to "Rom 🍕",
                        "Bali" to "Bali 🌴",
                        "New York" to "New York 🗽",
                        "London" to "London 🎡"
                    )
                }
                
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(suggestions.size) { index ->
                        val (sug, sugText) = suggestions[index]
                        val isSelected = viewModel.destination.trim().lowercase() == sug.lowercase()
                        androidx.compose.material3.SuggestionChip(
                            onClick = { 
                                viewModel.destination = sug
                                viewModel.updateSuggestedAirportForDestination(sug)
                            },
                            label = { 
                                Text(
                                    text = sugText, 
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                ) 
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }
                
                Box(modifier = Modifier.clickable { showDatePicker = true }) {
                    IosTextField(
                        value = viewModel.dates,
                        onValueChange = { },
                        label = "Wann möchtest du reisen?",
                        placeholder = "Tippen für Datumsauswahl...",
                        enabled = false
                    )
                }
                
                IosTextField(
                    value = viewModel.group,
                    onValueChange = { viewModel.group = it },
                    label = "Wer reist mit?",
                    placeholder = "z.B. 2 Erwachsene, 1 Kind"
                )
                
                IosTextField(
                    value = viewModel.budget,
                    onValueChange = { viewModel.budget = it },
                    label = "Gesamtbudget?",
                    placeholder = "z.B. 3000€"
                )
                
                IosTextField(
                    value = viewModel.style,
                    onValueChange = { viewModel.style = it },
                    label = "Reisestil & Wünsche?",
                    placeholder = "z.B. Strand, Kultur, Food"
                )
                
                IosTextField(
                    value = viewModel.extra,
                    onValueChange = { viewModel.extra = it },
                    label = "Besonderheiten?",
                    placeholder = "z.B. Vegan, Barrierefrei"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { viewModel.generatePlan() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = viewModel.destination.isNotBlank() || viewModel.style.isNotBlank(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recherche Starten", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
                
                val hasCachedPlan by viewModel.hasCachedPlan.collectAsState()
                if (hasCachedPlan) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.loadCachedPlan() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gespeicherten Plan laden", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_transition")
    val translationY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_anim"
    )
    
    val steps = listOf(
        "Analysiere persönliche Reisedaten...",
        "Suche nach passenden Flügen...",
        "Vergleiche Top-Hotels...",
        "Finde die besten Aktivitäten...",
        "Optimiere deinen Reiseplan..."
    )
    var stepIndex by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(3000)
            stepIndex = (stepIndex + 1) % steps.size
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = "Loading",
                modifier = Modifier
                    .size(48.dp)
                    .offset(y = translationY.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            CircularProgressIndicator(
                modifier = Modifier.size(120.dp),
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            "Reiseplan wird generiert",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Crossfade(
            targetState = stepIndex,
            animationSpec = tween(800),
            label = "step_crossfade"
        ) { targetIndex ->
            Text(
                steps[targetIndex],
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = "Error",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Da lief etwas schief.",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            message,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry, shape = RoundedCornerShape(14.dp), modifier = Modifier.height(50.dp)) {
            Text("Zurück zur Planung", fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(plan: TravelPlan, isCached: Boolean = false, onEditClick: () -> Unit) {
    val context = LocalContext.current
    val tabs = remember { listOf("Übersicht", "Aktivitäten", "Tagesplan", "Budget", "Infos") }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    
    fun sharePlan() {
        val shareText = """
            ✈️ Reiseplan für ${plan.destination}
            
            ${plan.description}
            
            Budget: ${plan.totalBudget}
            
            Hier sind ein paar Highlights:
            ${plan.activities.take(3).joinToString("\n") { "• ${it.title}" }}
        """.trimIndent()
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Reiseplan teilen"))
    }
    
    Scaffold(
        topBar = {
            Column {
                if (isCached) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "OFFLINE-MODUS • LOKAL GESPEICHERTER PLAN",
                            modifier = Modifier.padding(vertical = 4.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
                // Hero Banner - Sleek UI Style
                Surface(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                    color = MaterialTheme.colorScheme.primary, // #0059B2
                    shadowElevation = 8.dp
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Radial Gradients (opacity 20%)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent),
                                        center = androidx.compose.ui.geometry.Offset(200f, 150f),
                                        radius = 400f
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0xFFFFD700).copy(alpha = 0.2f), Color.Transparent),
                                        center = androidx.compose.ui.geometry.Offset(800f, 300f),
                                        radius = 500f
                                    )
                                )
                        )
                        
                        // Content
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(24.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(50),
                                ) {
                                    Text(
                                        "PERSONAL CONCIERGE",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.size(40.dp).clickable { onEditClick() }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Edit, contentDescription = "Bearbeiten", tint = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Surface(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.size(40.dp).clickable { sharePlan() }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Share, contentDescription = "Teilen", tint = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Surface(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("★ 9.2", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                plan.destination.uppercase(),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = (-1).sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(plan.totalBudget, color = MaterialTheme.colorScheme.primaryContainer, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(" • Budget Option", color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f), fontSize = 14.sp)
                            }
                        }
                    }
                }
                
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                color = MaterialTheme.colorScheme.primary,
                                height = 3.dp
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(title, fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->
            when(page) {
                0 -> OverviewTab(plan)
                1 -> ActivitiesTab(plan.activities)
                2 -> ItineraryTab(plan.itineraryDays)
                3 -> BudgetTab(plan.budgetBreakdown, plan.totalBudget)
                4 -> TipsTab(plan.tips)
            }
        }
    }
}

// ... Specific Tabs
@Composable
fun WeatherForecastSection(forecast: WeatherForecast) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌡️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Wetterprognose am Ziel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                androidx.compose.material3.SuggestionChip(
                    onClick = { },
                    label = { Text(forecast.averageTemperature, fontWeight = FontWeight.Bold) },
                    colors = androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = forecast.generalDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                lineHeight = 20.sp
            )
            
            if (forecast.forecastDays.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    forecast.forecastDays.forEach { dayForecast ->
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = dayForecast.day,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                                val emoji = when (dayForecast.icon.lowercase()) {
                                    "sunny" -> "☀️"
                                    "cloudy" -> "☁️"
                                    "rainy" -> "🌧️"
                                    "windy" -> "💨"
                                    "thunderstorm" -> "⛈️"
                                    "partly_cloudy" -> "⛅"
                                    else -> "🌡️"
                                }
                                Text(
                                    text = emoji,
                                    fontSize = 22.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = dayForecast.condition,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = dayForecast.temperature,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlightSegmentRow(
    title: String,
    segment: FlightSegmentDetails,
    isDirect: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (title == "Hinflug") "🛫" else "🛬",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = if (segment.stops == 0 && isDirect) "Direkt" else if (segment.stops == 0) "1 Stopp-Empfehlung" else "${segment.stops} Stopp(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Departure
            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1.2f)) {
                Text(segment.departureTime.ifBlank { "--:--" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(segment.departureAirport.ifBlank { "N/A" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            
            // Timeline decoration
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(segment.duration.ifBlank { "---" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)))
            }
            
            // Arrival
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1.2f)) {
                Text(segment.arrivalTime.ifBlank { "--:--" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(segment.arrivalAirport.ifBlank { "N/A" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
fun OverviewTab(plan: TravelPlan) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 800.dp)
            .verticalScroll(rememberScrollState())
            .padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        
        Text(plan.description, style = MaterialTheme.typography.bodyLarge, lineHeight = 24.sp)

        Button(
            onClick = {
                val uri = android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(plan.destination)}")
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback if Google Maps is not installed
                    val fallbackUri = android.net.Uri.parse("https://maps.google.com/?q=${android.net.Uri.encode(plan.destination)}")
                    val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, fallbackUri)
                    try {
                        context.startActivity(fallbackIntent)
                    } catch (e2: Exception) {
                        e2.printStackTrace()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reiseziel auf Karte ansehen", fontWeight = FontWeight.SemiBold)
        }
        
        plan.weatherForecast?.let { weather ->
            WeatherForecastSection(weather)
        }
        
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("BUDGET STATUS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Gesamtbudget", fontSize = 14.sp)
                    Text("€${plan.overview.flightBudget + plan.overview.hotelBudget + plan.overview.activityBudget}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.secondaryContainer)) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.75f).background(MaterialTheme.colorScheme.primary))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("€${plan.overview.bufferBudget} Puffer übrig", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
}

@Composable
fun FlightsTab(flights: List<Flight>) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 800.dp)
        ) {
        items(flights) { flight ->
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(flight.airline, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        if (flight.totalPrice.isNotBlank()) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(flight.totalPrice, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Gesamt (${flight.passengerCount} Pers.)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            Text(flight.price, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    
                    val displayOutbound = flight.outboundFlight
                    val displayReturn = flight.returnFlight
                    
                    if (displayOutbound != null) {
                        FlightSegmentRow(
                            title = "Hinflug",
                            segment = displayOutbound,
                            isDirect = flight.isDirect
                        )
                        
                        if (displayReturn != null && flight.isRoundTrip) {
                            Spacer(modifier = Modifier.height(16.dp))
                            FlightSegmentRow(
                                title = "Rückflug",
                                segment = displayReturn,
                                isDirect = flight.isDirect
                            )
                        }
                    } else {
                        // Fallback simple view
                        Text("Dauer: ${flight.duration} • ${if(flight.isDirect) "Direktflug" else "Mit Zwischenstopp"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (flight.totalPrice.isNotBlank() && flight.price.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Einzelpreis pro Person:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(flight.price, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    if (flight.bestPick) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                            Text("Best Pick", color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val hasUrl = !flight.url.isNullOrBlank()
                    Button(
                        onClick = {
                            if (hasUrl) {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(flight.url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        enabled = hasUrl,
                        modifier = Modifier.fillMaxWidth().height(44.dp), 
                        shape = RoundedCornerShape(10.dp), 
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasUrl) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer, 
                            contentColor = if (hasUrl) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (hasUrl) "Buchen" else "Keine Buchungsseite", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
}

@Composable
fun HotelsTab(hotels: List<Hotel>) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 800.dp)
        ) {
        items(hotels) { hotel ->
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(hotel.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(hotel.rating, color = Color(0xFFE6A822), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(hotel.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("${hotel.pricePerNight} / Nacht (Gesamt: ${hotel.totalPrice})", fontWeight = FontWeight.Medium)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Pros:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    hotel.pros.forEach { Text("• $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Cons:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    hotel.cons.forEach { Text("• $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    
                    if (hotel.bestPick) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                            Text("Best Pick", color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val hasUrl = !hotel.url.isNullOrBlank()
                    Button(
                        onClick = {
                            if (hasUrl) {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(hotel.url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        enabled = hasUrl,
                        modifier = Modifier.fillMaxWidth().height(44.dp), 
                        shape = RoundedCornerShape(10.dp), 
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasUrl) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer, 
                            contentColor = if (hasUrl) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (hasUrl) "Preise prüfen" else "Keine Preisprüfung", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
}

@Composable
fun ActivitiesTab(activities: List<Activity>) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 800.dp)
        ) {
        items(activities) { act ->
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(act.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(act.price, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(act.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Text("Dauer: ${act.duration}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("★ ${act.rating}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (act.isMustDo) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                            Text("Must-Do", color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    
                    val hasUrl = !act.url.isNullOrBlank()
                    if (hasUrl) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(act.url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mehr erfahren", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun ItineraryTab(days: List<ItineraryDay>) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 800.dp)
        ) {
        items(days) { day ->
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tag ${day.dayNumber}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Morgens", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(day.morning, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Mittags", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(day.afternoon, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Abends", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(day.evening, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
}

@Composable
fun BudgetTab(items: List<BudgetItem>, total: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp), 
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 800.dp)
        ) {
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.elevatedCardElevation(0.dp)
            ) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Geschätztes Gesamtbudget", style = MaterialTheme.typography.titleMedium)
                    Text(total, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(items) { item ->
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.category, fontWeight = FontWeight.Medium)
                Text(item.amount, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider()
        }
    }
}
}

@Composable
fun TipsTab(tips: List<Tip>) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 800.dp)
        ) {
        items(tips) { tip ->
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tip.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(tip.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
}

// --- Main App Logic ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: TravelViewModel = viewModel()
                    com.example.ui.MainApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun BackHandler(onBack: () -> Unit) {
    val dispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(dispatcher) {
        val callback = object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBack()
            }
        }
        dispatcher?.addCallback(callback)
        onDispose {
            callback.remove()
        }
    }
}
