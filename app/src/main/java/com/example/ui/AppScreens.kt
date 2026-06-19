package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.navigation.NavHostController
import com.example.TravelViewModel

@Composable
fun HomeScreen(navController: NavHostController, viewModel: TravelViewModel) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 100.dp)
        ) {
            // ── Hero glass header ───────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                // Animated gradient background
                Box(modifier = Modifier.fillMaxSize().background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1A3A6E), Color(0xFF0A1628)),
                        radius = 800f
                    )
                ))
                GlassScrim(modifier = Modifier.fillMaxSize())
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)
                ) {
                    Text("Guten ${greeting()}", color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (viewModel.userProfileHome.isNotBlank())
                            viewModel.userProfileHome else "Wohin reist du?",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    // ── Search field → opens BriefingScreen ──────
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("agent") },
                        cornerRadius = 16.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f))
                            Spacer(Modifier.width(12.dp))
                            Text("Reiseziel suchen...",
                                color = Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Quick Actions (all real navigation targets) ──────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickGlassAction(
                    icon = Icons.Default.SmartToy, label = "KI-Planer",
                    modifier = Modifier.weight(1f)
                ) { navController.navigate("agent") }
                QuickGlassAction(
                    icon = Icons.Default.FlightTakeoff, label = "Flüge",
                    modifier = Modifier.weight(1f)
                ) { navController.navigate("flights") }
                QuickGlassAction(
                    icon = Icons.Default.Luggage, label = "Meine Reisen",
                    modifier = Modifier.weight(1f)
                ) { navController.navigate("trips") }
                QuickGlassAction(
                    icon = Icons.Default.Person, label = "Profil",
                    modifier = Modifier.weight(1f)
                ) { navController.navigate("profile") }
            }

            Spacer(Modifier.height(24.dp))

            // ── AI Destination suggestions (real Gemini data) ───
            if (viewModel.userProfileHome.isNotBlank()) {
                LaunchedEffect(viewModel.userProfileHome) {
                    if (viewModel.suggestions.isEmpty() && !viewModel.suggestionsLoading) {
                        viewModel.fetchSuggestions()
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vorschläge für dich", color = Color.White,
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (viewModel.suggestionsLoading)
                        CircularProgressIndicator(modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp, color = AccentBlue)
                }
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    items(viewModel.suggestions) { s ->
                        GlassCard(
                            modifier = Modifier.width(200.dp).height(130.dp).clickable {
                                viewModel.destination = s.destination
                                navController.navigate("agent")
                            }
                        ) {
                            Box(Modifier.fillMaxSize().padding(16.dp)) {
                                Column(Modifier.align(Alignment.BottomStart)) {
                                    Text(s.destination, color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium)
                                    Text(s.subtitle, color = AccentTeal,
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper: greeting based on time of day
fun greeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Morgen"
        hour < 18 -> "Tag"
        else       -> "Abend"
    }
}

// Quick Action Button in glass style
@Composable
fun QuickGlassAction(
    icon: ImageVector, label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(modifier = modifier.clickable(onClick = onClick), cornerRadius = 20.dp) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = AccentBlue,
                modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, color = Color.White, fontSize = 11.sp,
                fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(navController: NavHostController, viewModel: TravelViewModel) {
    val savedPlans by viewModel.savedPlans.collectAsState()
    
    Box(
        modifier = Modifier.fillMaxSize().background(brush = GradientTravel), 
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(bottom = 100.dp)
        ) {
            TopAppBar(
                title = { 
                    Text(
                        "Meine Reisen", 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ) 
                },
                modifier = Modifier.fillMaxWidth(),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )

            if (savedPlans.isEmpty()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flight,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Noch keine Reisen geplant",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Erstelle deine erste Reise ganz einfach.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { navController.navigate("agent") },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reise planen", fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(savedPlans.size) { index ->
                        val entity = savedPlans[index]
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectSavedPlan(entity)
                                    navController.navigate("dashboard")
                                },
                            cornerRadius = 16.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "📍",
                                            fontSize = 24.sp,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Column {
                                            Text(
                                                text = entity.destination,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            if (entity.dates.isNotBlank()) {
                                                Text(
                                                    text = entity.dates,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                    
                                    IconButton(
                                        onClick = { viewModel.deleteSavedPlan(entity.id) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Reise löschen",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text("Budget: " + (if (entity.totalBudget.isNotBlank()) entity.totalBudget else "N/A"), fontWeight = FontWeight.Bold, color = Color.White) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = Color.White.copy(alpha = 0.2f),
                                            labelColor = Color.White
                                        )
                                    )
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.clickable {
                                            viewModel.selectSavedPlan(entity)
                                            navController.navigate("dashboard")
                                        }
                                    ) {
                                        Text(
                                            text = "Details ansehen",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = AccentTeal,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = AccentTeal,
                                            modifier = Modifier.size(16.dp)
                                        )
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
fun ProfileFormContent(viewModel: TravelViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.padding(16.dp)) {
                IosTextField(
                    value = viewModel.userProfileHome,
                    onValueChange = { viewModel.updateProfileHome(it) },
                    label = "Heimatort (Stadt)",
                    placeholder = "z.B. Berlin"
                )
            }
        }
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.padding(16.dp)) {
                IosTextField(
                    value = viewModel.userProfilePreferredDeparture,
                    onValueChange = { viewModel.updateProfilePreferredDeparture(it) },
                    label = "Bevorzugter Abflughafen",
                    placeholder = "z.B. Frankfurt (FRA)"
                )
            }
        }
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.padding(16.dp)) {
                IosTextField(
                    value = viewModel.userProfileCountry,
                    onValueChange = { viewModel.updateProfileCountry(it) },
                    label = "Wohnort/Heimatland",
                    placeholder = "z.B. Deutschland"
                )
            }
        }
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.padding(16.dp)) {
                IosTextField(
                    value = viewModel.userProfileAirlines,
                    onValueChange = { viewModel.updateProfileAirlines(it) },
                    label = "Bevorzugte Airlines",
                    placeholder = "z.B. Lufthansa, Emirates"
                )
            }
        }
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.padding(16.dp)) {
                IosTextField(
                    value = viewModel.userProfileDiet,
                    onValueChange = { viewModel.updateProfileDiet(it) },
                    label = "Besondere Essenswünsche",
                    placeholder = "z.B. Vegetarisch, Halal"
                )
            }
        }
    }
}

@Composable
fun ProfileScreen(viewModel: TravelViewModel) {
    Box(modifier = Modifier.fillMaxSize().background(brush = GradientTravel), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(16.dp)
                .padding(bottom = 100.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Profil", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
            ProfileFormContent(viewModel)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.saveProfile() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Profil speichern", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}
