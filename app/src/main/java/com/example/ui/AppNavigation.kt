package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.TravelUiState
import com.example.TravelViewModel
import androidx.compose.animation.*

data class NavItem(val route: String, val label: String, val icon: ImageVector)

val APP_NAV_ITEMS = listOf(
    NavItem("home",    "Home",   Icons.Default.Home),
    NavItem("agent",   "Planen", Icons.Default.SmartToy),
    NavItem("flights", "Flüge",  Icons.Default.FlightTakeoff),
    NavItem("trips",   "Reisen", Icons.Default.Luggage),
    NavItem("profile", "Profil", Icons.Default.Person)
)

@Composable
fun FloatingBottomNav(navController: NavController, modifier: Modifier = Modifier) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    FloatingGlassNavBar(modifier = modifier) {
        APP_NAV_ITEMS.forEach { item ->
            val selected = currentRoute == item.route
            val iconColor by animateColorAsState(
                targetValue = if (selected) Color(0xFF6200EA) else Color.Black,
                animationSpec = tween(300),
                label = "iconColor"
            )
            val bgAlpha by animateColorAsState(
                targetValue = if (selected) Color.Black.copy(alpha = 0.08f) else Color.Transparent,
                animationSpec = tween(300),
                label = "bgAlpha"
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(bgAlpha)
                    .clickable { navController.navigate(item.route) {
                        launchSingleTop = true
                        popUpTo("home") { saveState = true }
                        restoreState = true
                    }}
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(item.icon, contentDescription = item.label,
                    tint = iconColor, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(2.dp))
                Text(item.label, fontSize = 11.sp, color = iconColor,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: TravelViewModel) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showNav = currentRoute in APP_NAV_ITEMS.map { it.route } || currentRoute == "dashboard"

    Box(modifier = Modifier.fillMaxSize().background(brush = GradientTravel)) {
        // Main content — content scrolls UNDER the floating nav
        NavHost(navController = navController, startDestination = "home",
            modifier = Modifier.fillMaxSize()) {
            composable("home")    { HomeScreen(navController, viewModel) }
            composable("agent")   { 
                val state by viewModel.uiState.collectAsState()
                AnimatedContent(
                    targetState = state,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "AppStateTransition"
                ) { s ->
                    when (s) {
                        is TravelUiState.Briefing -> BriefingScreen(viewModel)
                        is TravelUiState.Loading -> LoadingScreen()
                        is TravelUiState.Success -> {
                            androidx.activity.compose.BackHandler {
                                viewModel.setBriefingState()
                            }
                            DashboardScreen(s.plan, s.isCached, onEditClick = { viewModel.setBriefingState() })
                        }
                        is TravelUiState.Error -> ErrorScreen(s.message) { viewModel.setBriefingState() }
                    }
                }
            }
            composable("flights") { FlightSearchScreen(viewModel, navController) }
            composable("trips")   { TripsScreen(navController, viewModel) }
            composable("profile") { ProfileScreen(viewModel) }
            composable("dashboard") {
                val state by viewModel.uiState.collectAsState()
                when (val s = state) {
                    is TravelUiState.Success -> {
                        androidx.activity.compose.BackHandler {
                            navController.popBackStack()
                        }
                        DashboardScreen(
                            plan = s.plan,
                            isCached = s.isCached,
                            onEditClick = {
                                viewModel.setBriefingState()
                                navController.navigate("agent") {
                                    popUpTo("home") { saveState = false }
                                }
                            }
                        )
                    }
                    else -> {
                        LaunchedEffect(Unit) { navController.navigate("agent") }
                    }
                }
            }
            composable("weather") { WeatherScreen(viewModel, navController) }
        }

        // Floating nav bar hovers above content at bottom
        if (showNav) {
            FloatingBottomNav(
                navController = navController,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}