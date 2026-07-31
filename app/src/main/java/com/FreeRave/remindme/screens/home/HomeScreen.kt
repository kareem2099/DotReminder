package com.FreeRave.remindme.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat // Changed for AutoMirrored
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.material3.FloatingActionButton // Added for FAB
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold // Added for FAB
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.FreeRave.remindme.BuildConfig
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.navigation.Screen
import com.FreeRave.remindme.ui.components.PrimaryButton
import com.FreeRave.remindme.ui.components.SearchBar
import com.FreeRave.remindme.ui.components.SwipeToDelete
import com.FreeRave.remindme.ui.components.AdvancedSearchBar
import com.FreeRave.remindme.ui.components.QuickFilter
import com.FreeRave.remindme.ui.components.EmptyRemindersState
import com.FreeRave.remindme.ui.components.NoSearchResultsState
import com.FreeRave.remindme.ui.components.PremiumReminderCard
import com.FreeRave.remindme.ui.components.WelcomeSection
import com.FreeRave.remindme.ui.components.AnimatedReminderList
import com.FreeRave.remindme.ui.theme.AppTypography
import com.FreeRave.remindme.ui.theme.getOceanEnhancedText
import com.FreeRave.remindme.utils.CategoryUtils
import com.FreeRave.remindme.utils.GpsEnableDialog
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.FreeRave.remindme.ui.theme.OceanBlueBackground
import com.FreeRave.remindme.ui.responsive.*
import com.FreeRave.remindme.ui.layout.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(navController: NavController) {
    val viewModel: HomeViewModel = hiltViewModel()
    val reminders by viewModel.reminders.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedQuickFilter by viewModel.selectedQuickFilter.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState() // Collect refreshing state
    val showGpsEnableDialog by viewModel.showGpsEnableDialog.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    // Get current theme for emoji features
    val mainViewModel: com.FreeRave.remindme.MainViewModel = hiltViewModel()
    val currentTheme by mainViewModel.currentTheme.collectAsState()

    // Load categories for filter chips
    val categoryViewModel: com.FreeRave.remindme.screens.category.CategoryViewModel = hiltViewModel()
    val allCategories by categoryViewModel.allCategories.collectAsState()

    val currentTime by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            kotlinx.coroutines.delay(30_000L)
            value = System.currentTimeMillis()
        }
    }

    OceanBlueBackground(
        themeSetting = currentTheme,
        useDarkTheme = false
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallFloatingActionButton(
                        onClick = { navController.navigate(Screen.AiChat.route) },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "AI Assistant")
                    }

                    ExtendedFloatingActionButton(
                        onClick = { navController.navigate(Screen.Add.createRoute()) },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Add Reminder", fontWeight = FontWeight.Bold) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) {
                // Welcome Section
                WelcomeSection(
                    userName = null, // TODO: Get from user preferences
                    reminders = reminders,
                    themeSetting = currentTheme,
                    currentTime = currentTime
                )

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = getResponsivePadding(16.dp)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getOceanEnhancedText("Reminders", currentTheme),
                        style = AppTypography.displayLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Calendar", style = AppTypography.bodyLarge) },
                                onClick = {
                                    showMenu = false
                                    navController.navigate(Screen.Calendar.route)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Categories", style = AppTypography.bodyLarge) },
                                onClick = {
                                    showMenu = false
                                    navController.navigate(Screen.Categories.route)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings", style = AppTypography.bodyLarge) },
                                onClick = {
                                    showMenu = false
                                    navController.navigate(Screen.Settings.route)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sign Out", style = AppTypography.bodyLarge) },
                                onClick = {
                                    showMenu = false
                                    navController.navigate(Screen.SignOut.route)
                                }
                            )
                        }
                    }
                }

                // Category Filter Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(getResponsiveSpacing(8.dp)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = getResponsivePadding(16.dp))
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text("All") }
                        )
                    }
                    // Predefined categories
                    items(CategoryUtils.predefinedCategories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { viewModel.selectCategory(category) },
                            label = { Text(category) }
                        )
                    }
                    // Custom categories
                    items(allCategories.filter { !it.isPredefined }) { category ->
                        FilterChip(
                            selected = selectedCategory == category.name,
                            onClick = { viewModel.selectCategory(category.name) },
                            label = { Text("${category.icon} ${category.name}") }
                        )
                    }
                }

                AdaptiveSpacer(getResponsiveSpacing(16.dp))

                // Advanced Search Bar
                AdvancedSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.searchReminders(it) },
                    selectedFilter = selectedQuickFilter,
                    onFilterChange = { viewModel.selectQuickFilter(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = getResponsivePadding(16.dp)),
                    placeholder = "Search reminders..."
                )

                AdaptiveSpacer(getResponsiveSpacing(16.dp))

                // Reminders List with Pull-to-Refresh
                val pullRefreshState = rememberPullRefreshState(
                    refreshing = isRefreshing,
                    onRefresh = { viewModel.refreshReminders() }
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .pullRefresh(pullRefreshState)
                ) {
                    if (reminders.isEmpty()) {
                        if (searchQuery.isNotBlank()) {
                            NoSearchResultsState(
                                searchQuery = searchQuery,
                                onClearSearch = { viewModel.searchReminders("") }
                            )
                        } else {
                            EmptyRemindersState(
                                onAddReminder = { navController.navigate(Screen.Add.createRoute()) }
                            )
                        }
                    } else {
                        AnimatedReminderList(
                            reminders = reminders,
                            onReminderClick = { reminder ->
                                navController.navigate(Screen.Edit.createRoute(reminder.id))
                            },
                            onDelete = { reminderId ->
                                viewModel.deleteReminder(reminderId)
                            },
                            currentTime = currentTime
                        )
                    }

                    PullRefreshIndicator(
                        refreshing = isRefreshing,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }

        // GPS Enable Dialog
        GpsEnableDialog(
            showDialog = showGpsEnableDialog,
            onDismiss = { viewModel.dismissGpsDialog() },
            onConfirm = { viewModel.enableGps() }
        )
    } // Closes Column
} // Closes Scaffold content lambda
    } // Closes ScreenBackground
} // Closes HomeScreen Composable
