package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.TravelViewModel
import kotlinx.coroutines.launch

@Composable
fun WeatherScreen(viewModel: TravelViewModel, navController: NavHostController) {
    var city by remember { mutableStateOf("") }
    val weather = viewModel.liveWeather

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Wetter (Live)", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("Stadt eingeben") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.fetchLiveWeather(city) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Wetter abrufen")
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        if (weather != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Aktuelles Wetter in $city", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${weather.current_weather?.temperature ?: "--"}°C", fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    Text("Windgeschw.: ${weather.current_weather?.windspeed ?: "--"} km/h", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}



@Composable
fun FlightSearchScreen(viewModel: TravelViewModel, navController: NavHostController) {
    var origin by remember { mutableStateOf(viewModel.departure.ifBlank { "" }) }
    var destination by remember { mutableStateOf(
        if (viewModel.destinationAirport.isNotBlank()) viewModel.destinationAirport
        else viewModel.destination.substringAfter("(").substringBefore(")").ifBlank { "" }
    ) }
    var date by remember { mutableStateOf("") }
    
    val flights = viewModel.liveFlights
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().background(brush = GradientTravel)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).padding(bottom = 100.dp)) {
            Text("Flugsuche (Live)", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = origin,
                            onValueChange = { origin = it },
                            label = { Text("Von", color = Color.White) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            )
                        )
                        OutlinedTextField(
                            value = destination,
                            onValueChange = { destination = it },
                            label = { Text("Nach", color = Color.White) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Datum (YYYY-MM-DD)", color = Color.White) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.fetchLiveFlights(origin, destination, date) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Flüge suchen", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            if (viewModel.flightError != null) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = viewModel.flightError ?: "",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else if (flights != null) {
                val bestFlights = flights.best_flights ?: emptyList()
                if (bestFlights.isEmpty()) {
                    Text("Keine Flüge gefunden.", color = Color.White)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        item {
                            Text("Beste Flüge", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        items(bestFlights.size) { index ->
                            val flight = bestFlights[index]
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/travel/flights"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) { }
                                    },
                                cornerRadius = 16.dp
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("${flight.price} €", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AccentBlue)
                                        Text("${flight.total_duration.div(60)}h ${flight.total_duration.rem(60)}m", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    flight.flights.forEachIndexed { i, leg ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(leg.departure_airport.id, fontWeight = FontWeight.Bold, color = Color.White)
                                            Icon(Icons.AutoMirrored.Filled.ArrowRightAlt, contentDescription = "to", modifier = Modifier.padding(horizontal = 8.dp), tint = Color.White)
                                            Text(leg.arrival_airport.id, fontWeight = FontWeight.Bold, color = Color.White)
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(leg.airline, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                                        }
                                        if (i < flight.flights.size - 1) {
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.2f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


