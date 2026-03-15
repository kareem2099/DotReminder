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
import com.FreeRave.remindme.ui.theme.ScreenBackground
import com.FreeRave.remindme.ui.theme.ScreenType
import com.FreeRave.remindme.utils.CategoryUtils
import com.FreeRave.remindme.utils.GpsEnableDialog
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
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

    ScreenBackground(
        screenType = ScreenType.HOME,
        themeSetting = currentTheme,
        useDarkTheme = false // Home screen uses light theme
    ) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.AiChat.route) },
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, "Chat with AI")
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
                    themeSetting = currentTheme
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
                            }
                        )
                    }

                    PullRefreshIndicator(
                        refreshing = isRefreshing,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }

                // Add Reminder Button
                PrimaryButton(
                    text = getOceanEnhancedText("Add Reminder", currentTheme),
                    onClick = { navController.navigate(Screen.Add.createRoute()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = getResponsivePadding(16.dp), vertical = getResponsivePadding(16.dp))
                )

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

@Composable
fun ReminderItem(
    reminder: Reminder,
    onReminderClick: () -> Unit,
    onDelete: () -> Unit
) {
    val createdTime = remember {
        SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
            .format(Date(reminder.time))
    }

    val reminderTime = remember {
        SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
            .format(Date(reminder.reminderTime))
    }


    SwipeToDelete(
        onDelete = onDelete
    ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onReminderClick() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (reminder.isLocationBased) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = "Location-based reminder",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Time-based reminder",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = reminder.title,
                            style = AppTypography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = reminder.description,
                        style = AppTypography.bodyLarge,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Reminder Time: $reminderTime",
                        style = AppTypography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Created Time: $createdTime",
                        style = AppTypography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (reminder.category.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = reminder.category,
                            color = Color.White,
                            modifier = Modifier
                                .background(CategoryUtils.getCategoryColor(reminder.category) ?: Color.Gray, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            style = AppTypography.labelSmall
                        )
                    }
                }
            }
    }
}
