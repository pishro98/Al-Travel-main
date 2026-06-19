package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*

@Composable
fun WeatherForecastSection(forecast: WeatherForecast) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
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
                        color = Color.White
                    )
                }
                androidx.compose.material3.SuggestionChip(
                    onClick = { },
                    label = { Text(forecast.averageTemperature, fontWeight = FontWeight.Bold, color = Color.White) },
                    colors = androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        labelColor = Color.White
                    )
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = forecast.generalDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                lineHeight = 20.sp
            )
            
            if (forecast.forecastDays.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    forecast.forecastDays.forEach { dayForecast ->
                        GlassCard(
                            modifier = Modifier.weight(1f),
                            cornerRadius = 12.dp
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
                                    textAlign = TextAlign.Center,
                                    color = Color.White
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
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = dayForecast.temperature,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentBlue,
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
                    color = AccentBlue
                )
            }
            Text(
                text = if (segment.stops == 0 && isDirect) "Direkt" else if (segment.stops == 0) "1 Stopp-Empfehlung" else "${segment.stops} Stopp(s)",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
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
                Text(segment.departureTime.ifBlank { "--:--" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text(segment.departureAirport.ifBlank { "N/A" }, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
            }
            
            // Timeline decoration
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(segment.duration.ifBlank { "---" }, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.3f)))
            }
            
            // Arrival
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1.2f)) {
                Text(segment.arrivalTime.ifBlank { "--:--" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text(segment.arrivalAirport.ifBlank { "N/A" }, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
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
            .padding(16.dp)
            .padding(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        
        Text(plan.description, style = MaterialTheme.typography.bodyLarge, lineHeight = 24.sp, color = Color.White)

        Button(
            onClick = {
                val uri = android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(plan.destination)}")
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val fallbackUri = android.net.Uri.parse("https://maps.google.com/?q=${android.net.Uri.encode(plan.destination)}")
                    val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, fallbackUri)
                    try { context.startActivity(fallbackIntent) } catch (e2: Exception) { e2.printStackTrace() }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
        ) {
            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reiseziel auf Karte ansehen", fontWeight = FontWeight.SemiBold, color = Color.White)
        }
        
        plan.weatherForecast?.let { weather ->
            WeatherForecastSection(weather)
        }
        
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("BUDGET STATUS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Gesamtbudget", fontSize = 14.sp, color = Color.White)
                    Text("€${plan.overview.flightBudget + plan.overview.hotelBudget + plan.overview.activityBudget}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.2f))) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.75f).background(AccentBlue))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("€${plan.overview.bufferBudget} Puffer übrig", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 116.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 800.dp)
        ) {
        items(flights) { flight ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(flight.airline, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        if (flight.totalPrice.isNotBlank()) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(flight.totalPrice, color = AccentBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Gesamt (${flight.passengerCount} Pers.)", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            }
                        } else {
                            Text(flight.price, color = AccentBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.2f))
                    
                    val displayOutbound = flight.outboundFlight
                    val displayReturn = flight.returnFlight
                    
                    if (displayOutbound != null) {
                        FlightSegmentRow(title = "Hinflug", segment = displayOutbound, isDirect = flight.isDirect)
                        if (displayReturn != null && flight.isRoundTrip) {
                            Spacer(modifier = Modifier.height(16.dp))
                            FlightSegmentRow(title = "Rückflug", segment = displayReturn, isDirect = flight.isDirect)
                        }
                    } else {
                        Text("Dauer: ${flight.duration} • ${if(flight.isDirect) "Direktflug" else "Mit Zwischenstopp"}", color = Color.White.copy(alpha = 0.7f))
                    }

                    if (flight.totalPrice.isNotBlank() && flight.price.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Einzelpreis pro Person:", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                                Text(flight.price, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    
                    if (flight.bestPick) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Badge(containerColor = AccentPurple) {
                            Text("Best Pick", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
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
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                        },
                        enabled = hasUrl,
                        modifier = Modifier.fillMaxWidth().height(44.dp), 
                        shape = RoundedCornerShape(10.dp), 
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasUrl) AccentBlue else Color.White.copy(alpha = 0.2f), 
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (hasUrl) "Buchen" else "Keine Buchungsseite", fontWeight = FontWeight.SemiBold, color = Color.White)
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 116.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 800.dp)
        ) {
        items(hotels) { hotel ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(hotel.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), color = Color.White)
                        Text(hotel.rating, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(hotel.location, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("${hotel.pricePerNight} / Nacht (Gesamt: ${hotel.totalPrice})", fontWeight = FontWeight.Medium, color = Color.White)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Pros:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    hotel.pros.forEach { Text("• $it", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f)) }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Cons:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    hotel.cons.forEach { Text("• $it", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f)) }
                    
                    if (hotel.bestPick) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Badge(containerColor = AccentPurple) {
                            Text("Best Pick", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
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
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                        },
                        enabled = hasUrl,
                        modifier = Modifier.fillMaxWidth().height(44.dp), 
                        shape = RoundedCornerShape(10.dp), 
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasUrl) AccentBlue else Color.White.copy(alpha = 0.2f), 
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (hasUrl) "Preise prüfen" else "Keine Preisprüfung", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}
}

@Composable
fun ActivitiesTab(activities: List<Activity>, destination: String = "") {
    val context = LocalContext.current
    var realPlaces by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<com.example.service.LocalPlace>>(emptyList()) }
    var isLoadingPlaces by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(destination) {
        if (destination.isNotBlank()) {
            isLoadingPlaces = true
            try {
                val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.service.LocalSearchService()
                        .searchLocalPlaces("Sehenswürdigkeiten und Aktivitäten in $destination")
                }
                result.onSuccess { response ->
                    realPlaces = response.local_results?.places ?: emptyList()
                }
            } catch (e: Exception) { }
            isLoadingPlaces = false
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 116.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 800.dp)
        ) {
            if (activities.isNotEmpty()) {
                item {
                    Text("KI-Empfehlungen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
                items(activities) { act ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(act.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), color = Color.White)
                                Text(act.price, color = AccentBlue, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(act.description, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row {
                                Text("Dauer: ${act.duration}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("★ ${act.rating}", fontSize = 12.sp, color = Color(0xFFFFD700))
                            }
                            if (act.isMustDo) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Badge(containerColor = AccentPurple) {
                                    Text("Must-Do", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
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
                                        } catch (e: Exception) { e.printStackTrace() }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Mehr erfahren", fontWeight = FontWeight.SemiBold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            if (isLoadingPlaces) {
                item { CircularProgressIndicator(modifier = Modifier.padding(16.dp), color = AccentBlue) }
            } else if (realPlaces.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Echte Orte in der Nähe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
                items(realPlaces) { place ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(place.title ?: "Unbekannt", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = Color.White)
                                if (place.rating != null) {
                                    Text("★ ${place.rating}", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (!place.address.isNullOrBlank()) {
                                Text(place.address, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                            }
                            if (!place.type.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                SuggestionChip(onClick = {}, label = { Text(place.type, style = MaterialTheme.typography.labelSmall, color = Color.White) }, colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.White.copy(alpha = 0.2f)))
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 116.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 800.dp)
        ) {
        items(days) { day ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tag ${day.dayNumber}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = AccentBlue)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Morgens", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                    Text(day.morning, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Mittags", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                    Text(day.afternoon, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Abends", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                    Text(day.evening, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 116.dp), 
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 800.dp)
        ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Geschätztes Gesamtbudget", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(total, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = AccentBlue)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(items) { item ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.category, fontWeight = FontWeight.Medium, color = Color.White)
                Text(item.amount, fontWeight = FontWeight.Bold, color = Color.White)
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
        }
    }
}
}

@Composable
fun TipsTab(tips: List<Tip>) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 116.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 800.dp)
        ) {
        items(tips) { tip ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = AccentBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tip.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(tip.description, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                }
            }
        }
    }
}
}
