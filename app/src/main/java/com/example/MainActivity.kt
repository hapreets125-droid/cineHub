package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MediaViewModel

enum class Screen {
    Home,
    Cerca,
    MiaLista,
    Download
}

class MainActivity : ComponentActivity() {
    private val viewModel: MediaViewModel by viewModels { MediaViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf(Screen.Home) }
                var playerMediaId by remember { mutableStateOf<String?>(null) }
                val selectedMedia by viewModel.selectedMedia.collectAsState()

                if (playerMediaId != null) {
                    // Immersive video player screen
                    PlayerScreen(
                        mediaId = playerMediaId!!,
                        viewModel = viewModel,
                        onBack = { playerMediaId = null }
                    )
                } else {
                    // Standard App Scaffold with Netflix-inspired Bottom Bar
                    Scaffold(
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface, // Elegant Dark Surface
                                contentColor = Color.White
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen == Screen.Home,
                                    onClick = { currentScreen = Screen.Home },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home", fontSize = 11.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) // Elegant Blue Accent overlay
                                    ),
                                    modifier = Modifier.testTag("nav_home_tab")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == Screen.Cerca,
                                    onClick = { currentScreen = Screen.Cerca },
                                    icon = { Icon(Icons.Default.Search, contentDescription = "Cerca") },
                                    label = { Text("Cerca", fontSize = 11.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier.testTag("nav_search_tab")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == Screen.MiaLista,
                                    onClick = { currentScreen = Screen.MiaLista },
                                    icon = { Icon(if (currentScreen == Screen.MiaLista) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, contentDescription = "La Mia Lista") },
                                    label = { Text("La Mia Lista", fontSize = 11.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier.testTag("nav_watchlist_tab")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == Screen.Download,
                                    onClick = { currentScreen = Screen.Download },
                                    icon = { Icon(Icons.Default.Download, contentDescription = "I Miei Download") },
                                    label = { Text("Download", fontSize = 11.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier.testTag("nav_downloads_tab")
                                )
                            }
                        },
                        contentWindowInsets = WindowInsets.safeDrawing,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentScreen) {
                                Screen.Home -> HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToPlayer = { playerMediaId = it }
                                )
                                Screen.Cerca -> SearchScreen(
                                    viewModel = viewModel
                                )
                                Screen.MiaLista -> WatchlistScreen(
                                    viewModel = viewModel
                                )
                                Screen.Download -> DownloadsScreen(
                                    viewModel = viewModel,
                                    onNavigateToPlayer = { playerMediaId = it }
                                )
                            }
                        }
                    }
                }

                // Global Detail Bottm Sheet Overlay
                if (selectedMedia != null) {
                    MediaDetailSheet(
                        media = selectedMedia!!,
                        onDismiss = { viewModel.selectMedia(null) },
                        onPlay = { playerMediaId = selectedMedia!!.id },
                        onToggleWatchlist = { viewModel.toggleWatchlist(selectedMedia!!) },
                        onDownload = { viewModel.startDownload(selectedMedia!!.id) },
                        onRemoveDownload = { viewModel.removeDownload(selectedMedia!!.id) }
                    )
                }
            }
        }
    }
}
