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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.TravelViewModel

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
                    enabled = viewModel.destination.isNotBlank() && viewModel.budget.isNotBlank(),
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
