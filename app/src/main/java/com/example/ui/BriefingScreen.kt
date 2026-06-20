package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.TravelViewModel
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.LaunchedEffect



data class TopDestination(val city: String, val label: String, val airport: String, val country: String)

@Composable
fun StepperTile(
    label: String,
    value: Int,
    min: Int = 0,
    max: Int = 9,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    GlassCard(cornerRadius = 16.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White,
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FilledIconButton(
                    onClick = onDecrement,
                    enabled = value > min,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White,
                        disabledContainerColor = Color.White.copy(alpha = 0.06f),
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        color = if (value > min) Color.White else Color.White.copy(alpha = 0.3f))
                }
                Text(
                    "$value",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.widthIn(min = 28.dp),
                    textAlign = TextAlign.Center
                )
                FilledIconButton(
                    onClick = onIncrement,
                    enabled = value < max,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = AccentBlue,
                        contentColor = Color.White,
                        disabledContainerColor = Color.White.copy(alpha = 0.06f),
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CabinClassTile(value: String, onChange: (String) -> Unit) {
    val classes = listOf("Economy", "Premium Economy", "Business", "First Class")
    val currentIndex = classes.indexOf(value).coerceAtLeast(0)
    GlassCard(cornerRadius = 16.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text("Flugklasse", color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                classes.forEach { cls ->
                    val selected = cls == value
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) AccentBlue else Color.White.copy(alpha = 0.12f)
                            )
                            .clickable { onChange(cls) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (cls) {
                                "Economy" -> "Eco"
                                "Premium Economy" -> "Prem."
                                "Business" -> "Biz"
                                else -> "First"
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BriefingScreen(viewModel: TravelViewModel) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Auto-fill departure from profile if currently empty
    LaunchedEffect(viewModel.userProfilePreferredDeparture) {
        if (viewModel.departure.isBlank() && viewModel.userProfilePreferredDeparture.isNotBlank()) {
            viewModel.departure = viewModel.userProfilePreferredDeparture
        }
    }
    
    if (showProfileDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Mein Reise-Profil") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    com.example.ui.ProfileFormContent(viewModel)
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

    Box(
        modifier = Modifier.fillMaxSize().background(brush = GradientTravel), 
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = { },
                modifier = Modifier.fillMaxWidth(),
                actions = {
                    androidx.compose.material3.IconButton(onClick = { showProfileDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Profil Einstellungen", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 700.dp)
                        .verticalScroll(scrollState)
                        .verticalScrollIndicator(scrollState)
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .padding(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text("Reise planen", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp, color = Color.White)
                    
                    if (viewModel.userProfileHome.isNotBlank() || viewModel.userProfileAirlines.isNotBlank()) {
                        GlassCard(cornerRadius = 12.dp, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null,
                                    tint = AccentTeal, modifier = Modifier.size(18.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Dein Profil wird berücksichtigt",
                                        color = AccentTeal, style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold)
                                    val profileSummary = buildString {
                                        if (viewModel.userProfileHome.isNotBlank()) append("📍 ${viewModel.userProfileHome}")
                                        if (viewModel.userProfileAirlines.isNotBlank()) append("  ✈️ ${viewModel.userProfileAirlines}")
                                        if (viewModel.userProfileDiet.isNotBlank()) append("  🥗 ${viewModel.userProfileDiet}")
                                    }
                                    if (profileSummary.isNotBlank()) {
                                        Text(profileSummary, color = Color.White.copy(alpha = 0.7f),
                                            style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                    }
                                }
                            }
                        }
                    }

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

                    // Suggested Destinations Row
                    val top50Destinations = remember {
                        listOf(
                            TopDestination("Mallorca",         "Mallorca 🏖️",           "PMI", "Spanien"),
                            TopDestination("Teneriffa",        "Teneriffa 🌊",           "TFS", "Spanien"),
                            TopDestination("Gran Canaria",     "Gran Canaria ☀️",        "LPA", "Spanien"),
                            TopDestination("Fuerteventura",    "Fuerteventura 🏄",       "FUE", "Spanien"),
                            TopDestination("Ibiza",            "Ibiza 🎶",               "IBZ", "Spanien"),
                            TopDestination("Barcelona",        "Barcelona 🎨",           "BCN", "Spanien"),
                            TopDestination("Lanzarote",        "Lanzarote 🌋",           "ACE", "Spanien"),
                            TopDestination("Kreta",            "Kreta 🏛️",              "HER", "Griechenland"),
                            TopDestination("Rhodos",           "Rhodos 🌞",              "RHO", "Griechenland"),
                            TopDestination("Santorini",        "Santorini 🌅",           "JTR", "Griechenland"),
                            TopDestination("Mykonos",          "Mykonos 💃",             "JMK", "Griechenland"),
                            TopDestination("Korfu",            "Korfu 🫒",               "CFU", "Griechenland"),
                            TopDestination("Antalya",          "Antalya 🏖️",            "AYT", "Türkei"),
                            TopDestination("Istanbul",         "Istanbul 🕌",            "IST", "Türkei"),
                            TopDestination("Bodrum",           "Bodrum ⛵",              "BJV", "Türkei"),
                            TopDestination("Dubai",            "Dubai ✨",               "DXB", "VAE"),
                            TopDestination("Abu Dhabi",        "Abu Dhabi 🏎️",          "AUH", "VAE"),
                            TopDestination("Phuket",           "Phuket 🌺",              "HKT", "Thailand"),
                            TopDestination("Bangkok",          "Bangkok 🛺",             "BKK", "Thailand"),
                            TopDestination("Koh Samui",        "Koh Samui 🥥",          "USM", "Thailand"),
                            TopDestination("Bali",             "Bali 🌴",                "DPS", "Indonesien"),
                            TopDestination("Malediven",        "Malediven 🐠",           "MLE", "Malediven"),
                            TopDestination("Singapur",         "Singapur 🌆",            "SIN", "Singapur"),
                            TopDestination("Tokio",            "Tokio 🍣",               "HND", "Japan"),
                            TopDestination("Osaka",            "Osaka 🦌",               "KIX", "Japan"),
                            TopDestination("Kyoto",            "Kyoto 🌸",               "KIX", "Japan"),
                            TopDestination("New York",         "New York 🗽",            "JFK", "USA"),
                            TopDestination("Miami",            "Miami 🌴",               "MIA", "USA"),
                            TopDestination("Las Vegas",        "Las Vegas 🎰",           "LAS", "USA"),
                            TopDestination("Orlando",          "Orlando 🏰",             "MCO", "USA"),
                            TopDestination("Los Angeles",      "Los Angeles 🎬",         "LAX", "USA"),
                            TopDestination("Cancún",           "Cancún 🌮",              "CUN", "Mexiko"),
                            TopDestination("Marrakech",        "Marrakech 🧿",           "RAK", "Marokko"),
                            TopDestination("Hurghada",         "Hurghada 🐚",            "HRG", "Ägypten"),
                            TopDestination("Sharm el-Sheikh",  "Sharm el-Sheikh 🤿",     "SSH", "Ägypten"),
                            TopDestination("Lissabon",         "Lissabon 🎸",            "LIS", "Portugal"),
                            TopDestination("Algarve",          "Algarve 🌊",             "FAO", "Portugal"),
                            TopDestination("Madeira",          "Madeira 🌺",             "FNC", "Portugal"),
                            TopDestination("Paris",            "Paris 🗼",               "CDG", "Frankreich"),
                            TopDestination("Nizza",            "Nizza 🥂",               "NCE", "Frankreich"),
                            TopDestination("Dubrovnik",        "Dubrovnik 💎",           "DBV", "Kroatien"),
                            TopDestination("Amsterdam",        "Amsterdam 🌷",           "AMS", "Niederlande"),
                            TopDestination("Prag",             "Prag 🍺",                "PRG", "Tschechien"),
                            TopDestination("Wien",             "Wien 🎼",                "VIE", "Österreich"),
                            TopDestination("London",           "London 🎡",              "LHR", "Großbritannien"),
                            TopDestination("Rom",              "Rom 🍕",                 "FCO", "Italien"),
                            TopDestination("Venedig",          "Venedig 🚤",             "VCE", "Italien"),
                            TopDestination("Kapstadt",         "Kapstadt 🦁",            "CPT", "Südafrika"),
                            TopDestination("Reykjavik",        "Reykjavik 🌋",           "KEF", "Island"),
                            TopDestination("Sydney",           "Sydney 🦘",              "SYD", "Australien")
                        )
                    }

                    Text(
                        text = "TOP 50 REISEZIELE",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )

                    // Scrollable chip grid — 3 per row
                    val chunkedDests = top50Destinations.chunked(3)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        chunkedDests.forEach { rowItems ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowItems.forEach { dest ->
                                    val isSelected = viewModel.destination.trim().lowercase() == dest.city.lowercase()
                                    GlassCard(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                viewModel.destination = dest.city
                                                viewModel.destinationAirport = dest.airport
                                                viewModel.updateSuggestedAirportForDestination(dest.city)
                                            },
                                        cornerRadius = 14.dp,
                                        alpha = if (isSelected) 0.45f else 0.18f
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = dest.label,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = Color.White,
                                                textAlign = TextAlign.Center,
                                                maxLines = 2
                                            )
                                            if (isSelected) {
                                                Text(
                                                    text = dest.airport,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AccentBlue,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                                // Fill remaining slots in last row if < 3 items
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
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
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "REISEGRUPPE & AUSSTATTUNG",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    StepperTile(
                        label = "👨‍👩‍👧  Erwachsene",
                        value = viewModel.adultsCount,
                        min = 1, max = 9,
                        onDecrement = { viewModel.adultsCount-- },
                        onIncrement = { viewModel.adultsCount++ }
                    )
                    StepperTile(
                        label = "🧒  Kinder (unter 12)",
                        value = viewModel.childrenCount,
                        min = 0, max = 6,
                        onDecrement = { viewModel.childrenCount-- },
                        onIncrement = { viewModel.childrenCount++ }
                    )
                    StepperTile(
                        label = "🛏  Zimmer",
                        value = viewModel.roomsCount,
                        min = 1, max = 5,
                        onDecrement = { viewModel.roomsCount-- },
                        onIncrement = { viewModel.roomsCount++ }
                    )
                    CabinClassTile(
                        value = viewModel.cabinClass,
                        onChange = { viewModel.cabinClass = it }
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
                        enabled = viewModel.destination.isNotBlank() && viewModel.budget.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Recherche Starten", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    
                    val hasCachedPlan by viewModel.hasCachedPlan.collectAsState()
                    if (hasCachedPlan) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.loadCachedPlan() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha=0.2f), contentColor = Color.White)
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
}
