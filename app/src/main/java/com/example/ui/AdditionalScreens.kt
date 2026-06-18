package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun BudgetScreen(navController: NavHostController) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Reisebudget", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Gesamtbudget: 2500 €", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = { 0.4f }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text("1000 € von 2500 € ausgegeben", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("✈️ Flüge", fontWeight = FontWeight.SemiBold); Text("400 €")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("🏨 Hotels", fontWeight = FontWeight.SemiBold); Text("500 €")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("🍽️ Essen", fontWeight = FontWeight.SemiBold); Text("100 €")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        FloatingActionButton(onClick = {}, modifier = Modifier.align(Alignment.End)) {
            Icon(Icons.Default.Add, contentDescription = "Ausgabe hinzufügen")
        }
    }
}

@Composable
fun FlightSearchScreen(viewModel: TravelViewModel, navController: NavHostController) {
    var origin by remember { mutableStateOf("CDG") }
    var destination by remember { mutableStateOf("AUS") }
    var date by remember { mutableStateOf("2026-03-03") }
    
    val flights = viewModel.liveFlights

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Flugsuche (Live)", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = origin,
                onValueChange = { origin = it },
                label = { Text("Von") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text("Nach") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Datum (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.fetchLiveFlights(origin, destination, date) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Flüge suchen")
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        if (viewModel.flightError != null) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = viewModel.flightError ?: "",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        } else if (flights != null) {
            val bestFlights = flights.best_flights ?: emptyList()
            if (bestFlights.isEmpty()) {
                Text("Keine Flüge gefunden.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        Text("Beste Flüge", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    items(bestFlights.size) { index ->
                        val flight = bestFlights[index]
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("${flight.price} €", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text("${flight.total_duration.div(60)}h ${flight.total_duration.rem(60)}m", style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                flight.flights.forEachIndexed { i, leg ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(leg.departure_airport.id, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.ArrowRightAlt, contentDescription = "to", modifier = Modifier.padding(horizontal = 8.dp))
                                        Text(leg.arrival_airport.id, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(leg.airline, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (i < flight.flights.size - 1) {
                                        Divider(modifier = Modifier.padding(vertical = 8.dp))
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

@Composable
fun DocsScreen(navController: NavHostController) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Dokumente", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(3) { index ->
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if(index==0) "Flugticket_Lufthansa.pdf" else if (index==1) "Booking_Bestätigung.pdf" else "Reisepass_Kopie.pdf", fontWeight = FontWeight.SemiBold)
                            Text("Hinzugefügt: Heute", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Upload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Dokument hochladen")
        }
    }
}
