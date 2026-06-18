package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.BriefingScreen
import com.example.DashboardScreen
import com.example.ErrorScreen
import com.example.LoadingScreen
import com.example.TravelUiState
import com.example.TravelViewModel
import androidx.compose.animation.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import com.example.R

@Composable
fun MainApp(viewModel: TravelViewModel) {
    val navController = rememberNavController()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Row(modifier = Modifier.fillMaxSize()) {
        if (isTablet) {
            SideNavigationRail(navController = navController)
        }
        Scaffold(
            bottomBar = {
                if (!isTablet) {
                    BottomNavigationBar(navController = navController)
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("home") {
                    HomeScreen(navController, viewModel)
                }
                composable("trips") {
                    TripsScreen(navController, viewModel)
                }
                composable("discover") {
                    DiscoverScreen(viewModel, navController)
                }
                composable("agent") {
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
                composable("profile") {
                    ProfileScreen(viewModel)
                }
                composable("flights") {
                    FlightSearchScreen(viewModel, navController)
                }
                composable("weather") {
                    WeatherScreen(viewModel, navController)
                }
                composable("budget") {
                    BudgetScreen(navController)
                }
                composable("docs") {
                    DocsScreen(navController)
                }
            }
        }
    }
}

@Composable
fun SideNavigationRail(navController: NavHostController) {
    val items = remember {
        listOf(
            NavigationItem("home", "Home", Icons.Default.Home),
            NavigationItem("flights", "Flüge", Icons.Default.FlightTakeoff),
            NavigationItem("trips", "Reisen", Icons.Default.Flight),
            NavigationItem("weather", "Wetter", Icons.Default.LocationOn),
            NavigationItem("budget", "Budget", Icons.Default.ShoppingCart),
            NavigationItem("docs", "Docs", Icons.Default.Info),
            NavigationItem("profile", "Profil", Icons.Default.Person)
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        header = {
            Icon(
                imageVector = Icons.Default.Flight,
                contentDescription = "App Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 24.dp).size(28.dp)
            )
        }
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        items.forEach { item ->
            NavigationRailItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = remember {
        listOf(
            NavigationItem("home", "Home", Icons.Default.Home),
            NavigationItem("flights", "Flüge", Icons.Default.FlightTakeoff),
            NavigationItem("trips", "Reisen", Icons.Default.Flight),
            NavigationItem("weather", "Wetter", Icons.Default.LocationOn),
            NavigationItem("budget", "Budget", Icons.Default.ShoppingCart),
            NavigationItem("docs", "Docs", Icons.Default.Info),
            NavigationItem("profile", "Profil", Icons.Default.Person)
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

data class NavigationItem(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
