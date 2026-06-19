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
                targetValue = if (selected) AccentBlue else Color.White.copy(alpha = 0.55f),
                animationSpec = tween(300),
                label = "iconColor"
            )
            val bgAlpha by animateColorAsState(
                targetValue = if (selected) AccentBlue.copy(alpha = 0.22f) else Color.Transparent,
                animationSpec = tween(300),
                label = "bgAlpha"
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgAlpha)
                    .clickable { navController.navigate(item.route) {
                        launchSingleTop = true
                        popUpTo("home") { saveState = true }
                        restoreState = true
                    }}
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(item.icon, contentDescription = item.label,
                    tint = iconColor, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.height(3.dp))
                Text(item.label, fontSize = 10.sp, color = iconColor,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
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