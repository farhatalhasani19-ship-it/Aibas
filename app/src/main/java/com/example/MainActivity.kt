package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Diary
import com.example.data.model.UserProfile
import com.example.data.repository.DiaryRepository
import com.example.ui.components.StarryBackground
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DairySayanggTheme {
                MainAppContent()
            }
        }
    }
}

@Composable
fun MainAppContent() {
    val context = LocalContext.current
    val repository = remember { DiaryRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var currentDiary by remember { mutableStateOf<Diary?>(null) }
    var isLoadingProfile by remember { mutableStateOf(true) }

    var currentScreen by remember { mutableStateOf("dashboard") }

    // Listen to Firebase Auth state
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    // When user is authenticated, load or observe their profile & diary
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            isLoadingProfile = true
            try {
                val profile = repository.getUserProfile(currentUser!!.uid)
                userProfile = profile
                if (profile?.diaryId != null && profile.diaryId.isNotBlank()) {
                    // Start observing diary
                    repository.observeDiary(profile.diaryId).collect { d ->
                        currentDiary = d
                        isLoadingProfile = false
                    }
                } else {
                    isLoadingProfile = false
                }
            } catch (e: Exception) {
                isLoadingProfile = false
            }
        } else {
            userProfile = null
            currentDiary = null
            isLoadingProfile = false
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        // Authenticated flow
        if (isLoadingProfile) {
            StarryBackground {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = LavenderPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Membuka Ruang Cinta Kita... 💜",
                            color = LavenderPrimary,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        } else if (currentDiary == null) {
            // User does not have a linked diary yet -> Show Setup
            SetupDiaryScreen(
                repository = repository,
                onDiaryReady = { diary ->
                    currentDiary = diary
                    currentScreen = "dashboard"
                }
            )
        } else {
            // User is inside their active private shared diary!
            val diary = currentDiary!!

            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = MidnightSurface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.testTag("main_bottom_nav")
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == "dashboard",
                            onClick = { currentScreen = "dashboard" },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Beranda") },
                            label = { Text("Beranda", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LavenderPrimary,
                                selectedTextColor = LavenderPrimary,
                                indicatorColor = LavenderContainer,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                        NavigationBarItem(
                            selected = currentScreen == "diary",
                            onClick = { currentScreen = "diary" },
                            icon = { Icon(Icons.Default.Book, contentDescription = "Our Diary") },
                            label = { Text("Diary", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LavenderPrimary,
                                selectedTextColor = LavenderPrimary,
                                indicatorColor = LavenderContainer,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                        NavigationBarItem(
                            selected = currentScreen == "memories",
                            onClick = { currentScreen = "memories" },
                            icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "Our Memories") },
                            label = { Text("Foto", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SoftRoseSecondary,
                                selectedTextColor = SoftRoseSecondary,
                                indicatorColor = SoftRoseContainer,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                        NavigationBarItem(
                            selected = currentScreen == "letters",
                            onClick = { currentScreen = "letters" },
                            icon = { Icon(Icons.Default.Mail, contentDescription = "Love Letters") },
                            label = { Text("Surat", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = StarGoldTertiary,
                                selectedTextColor = StarGoldTertiary,
                                indicatorColor = StarGoldContainer,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                        NavigationBarItem(
                            selected = currentScreen == "settings",
                            onClick = { currentScreen = "settings" },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") },
                            label = { Text("Pengaturan", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LavenderPrimary,
                                selectedTextColor = LavenderPrimary,
                                indicatorColor = LavenderContainer,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                    }
                },
                containerColor = MidnightBase
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentScreen) {
                        "dashboard" -> DashboardScreen(
                            diary = diary,
                            repository = repository,
                            onNavigateToSection = { section -> currentScreen = section }
                        )
                        "diary" -> DiaryScreen(
                            diary = diary,
                            repository = repository,
                            onBack = { currentScreen = "dashboard" }
                        )
                        "memories" -> MemoriesScreen(
                            diary = diary,
                            repository = repository,
                            onBack = { currentScreen = "dashboard" }
                        )
                        "letters" -> LoveLettersScreen(
                            diary = diary,
                            repository = repository,
                            onBack = { currentScreen = "dashboard" }
                        )
                        "timeline" -> TimelineScreen(
                            diary = diary,
                            repository = repository,
                            onBack = { currentScreen = "dashboard" }
                        )
                        "littleThings" -> LittleThingsScreen(
                            diary = diary,
                            repository = repository,
                            onBack = { currentScreen = "dashboard" }
                        )
                        "surprise" -> SurpriseScreen(
                            diary = diary,
                            repository = repository,
                            onBack = { currentScreen = "dashboard" },
                            onOpenLetter = { currentScreen = "letters" }
                        )
                        "settings" -> SettingsScreen(
                            diary = diary,
                            repository = repository,
                            onBack = { currentScreen = "dashboard" },
                            onSignOut = {
                                currentUser = null
                                userProfile = null
                                currentDiary = null
                            }
                        )
                    }
                }
            }
        }
    }
}
