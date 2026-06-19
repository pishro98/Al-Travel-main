package com.example.ui

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
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.model.*

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
                1 -> ActivitiesTab(plan.activities, plan.destination)
                2 -> ItineraryTab(plan.itineraryDays)
                3 -> BudgetTab(plan.budgetBreakdown, plan.totalBudget)
                4 -> TipsTab(plan.tips)
            }
        }
    }
}
