package com.memorize

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.memorize.database.MemorizeDatabase
import com.memorize.network.TextService
import com.memorize.network.DeepSeekService
import com.memorize.ui.FileInputScreen
import com.memorize.ui.LearningScreen
import com.memorize.ui.SearchScreen
import com.memorize.ui.SettingsScreen
import com.memorize.ui.StatisticsScreen
import com.memorize.ui.TextInputScreen
import com.memorize.ui.TextPreviewScreen
import com.memorize.ui.TextStructureScreen
import com.memorize.ui.theme.MemorizeTheme
import com.memorize.ui.viewmodel.LearningViewModel
import com.memorize.ui.viewmodel.SearchViewModel
import com.memorize.ui.viewmodel.TextInputViewModel
import com.memorize.ui.viewmodel.TextStructureViewModel
import com.memorize.utils.FileUtils
import kotlinx.coroutines.launch
import com.memorize.R

class MainActivity : ComponentActivity() {
    private lateinit var database: MemorizeDatabase
    private lateinit var textService: TextService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("Memorize", "MainActivity.onCreate started")
        
        try {
            Log.d("Memorize", "Initializing database...")
            // Initialize database
            database = Room.databaseBuilder(
                applicationContext,
                MemorizeDatabase::class.java,
                MemorizeDatabase.DATABASE_NAME
            )
            .allowMainThreadQueries() // Временно для отладки
            .fallbackToDestructiveMigration() // Clear DB on schema change (for now)
            .build()
            Log.d("Memorize", "Database initialized successfully")
            
            // Initialize DeepSeek service
            Log.d("Memorize", "Initializing DeepSeek service...")
            val apiKey = getString(R.string.deepseek_api_key)
            
            // Log configuration (without exposing full API key for security)
            Log.d("Memorize", "API Key: ${if (apiKey.length > 10) apiKey.take(10) + "..." else "INVALID"}")
            
            if (apiKey == "YOUR_DEEPSEEK_API_KEY" || apiKey.isBlank()) {
                Log.e("Memorize", "ERROR: API key is not configured! Please set deepseek_api_key in config.xml")
            }
            
            val deepSeekService = DeepSeekService(apiKey)
            
            // Get or create userId
            val prefs = getSharedPreferences("memorize_prefs", MODE_PRIVATE)
            var userId = prefs.getString("user_id", null)
            if (userId == null) {
                userId = java.util.UUID.randomUUID().toString()
                prefs.edit().putString("user_id", userId).apply()
                Log.d("Memorize", "Created new user ID: $userId")
            } else {
                Log.d("Memorize", "Using existing user ID: $userId")
            }
            
            textService = TextService(database, deepSeekService, userId, this@MainActivity)
            Log.d("Memorize", "Services initialized successfully")
            
            Log.d("Memorize", "Setting content...")
            setContent {
                var hasPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }
                
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasPermission = isGranted
                    Log.d("Memorize", "Microphone permission granted: $isGranted")
                }
                
                LaunchedEffect(Unit) {
                    if (!hasPermission) {
                        Log.w("Memorize", "Microphone permission not granted, requesting...")
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
                
                MemorizeTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (hasPermission) {
                            AppNavigation(database, textService, this@MainActivity)
                        } else {
                            androidx.compose.material3.Text(
                                text = "Требуется разрешение на использование микрофона для распознавания речи.",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
            Log.d("Memorize", "Content set successfully")
        } catch (e: Exception) {
            Log.e("Memorize", "Error in onCreate", e)
            e.printStackTrace()
            // Показываем ошибку пользователю
            setContent {
                MemorizeTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        androidx.compose.material3.Text(
                            text = "Ошибка запуска приложения: ${e.message}\n\nПроверьте логи для деталей.",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    database: MemorizeDatabase,
    textService: TextService,
    context: android.content.Context
) {
    val navController = rememberNavController()
    var selectedFileUri: Uri? by remember { mutableStateOf(null) }
    var previewText: com.memorize.ui.viewmodel.FoundText? by remember { mutableStateOf(null) }
    var editTextData: Pair<String, String>? by remember { mutableStateOf(null) } // title, text
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            navController.navigate("fileInput")
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = "search"
    ) {
        composable("search") {
            android.util.Log.d("MainActivity", "composable(search): Composing search screen")
            // Use a key to ensure the same ViewModel instance is used across screens
            val searchViewModel = androidx.lifecycle.viewmodel.compose.viewModel<SearchViewModel>(
                key = "search_viewmodel",
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        android.util.Log.d("MainActivity", "composable(search): Creating SearchViewModel")
                        return SearchViewModel(database, textService, context) as T
                    }
                }
            )
            
            android.util.Log.d("MainActivity", "composable(search): Calling SearchScreen")
            SearchScreen(
                onTextSelected = { textId ->
                    navController.navigate("learning/$textId")
                },
                onFileSelected = {
                    filePickerLauncher.launch("text/*")
                },
                onTextInput = {
                    navController.navigate("textInput")
                },
                onShowStructure = { textId ->
                    navController.navigate("structure/$textId")
                },
                onShowSettings = {
                    navController.navigate("settings")
                },
                viewModel = searchViewModel
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                context = context
            )
        }
        
        composable("preview") {
            previewText?.let { foundText ->
                val scope = rememberCoroutineScope()
                var isSaving by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf<String?>(null) }
                
                // Use the same ViewModel instance as search screen
                val searchViewModel = androidx.lifecycle.viewmodel.compose.viewModel<SearchViewModel>(
                    key = "search_viewmodel",
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return SearchViewModel(database, textService, context) as T
                        }
                    }
                )
                
                TextPreviewScreen(
                    title = foundText.title,
                    author = foundText.author,
                    fullText = foundText.fullText,
                    isSaving = isSaving,
                    errorMessage = errorMessage,
                    onApprove = {
                        scope.launch {
                            try {
                                isSaving = true
                                errorMessage = null
                                android.util.Log.d("MainActivity", "onApprove: Starting save process for title='${foundText.title}'")
                                // Pass foundText directly to approveAndSave
                                val textId = searchViewModel.approveAndSave(foundText)
                                android.util.Log.d("MainActivity", "onApprove: approveAndSave returned textId: $textId")
                                if (textId != null) {
                                    android.util.Log.d("MainActivity", "onApprove: Text saved successfully, preparing navigation")
                                    // Log current back stack
                                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                                    android.util.Log.d("MainActivity", "onApprove: Current route before navigation: $currentRoute")
                                    val previousRoute = navController.previousBackStackEntry?.destination?.route
                                    android.util.Log.d("MainActivity", "onApprove: Previous route before navigation: $previousRoute")
                                    
                                    // Clear foundText FIRST, before navigation, to prevent preview from reopening
                                    android.util.Log.d("MainActivity", "onApprove: Clearing foundText")
                                    searchViewModel.clearFoundText()
                                    
                                    // Clear previewText before navigation
                                    previewText = null
                                    
                                    // Navigate back to search screen
                                    android.util.Log.d("MainActivity", "onApprove: Starting navigation - popping back to search")
                                    // Pop back to search screen (which should be the previous screen)
                                    val popped = navController.popBackStack()
                                    android.util.Log.d("MainActivity", "onApprove: popBackStack() returned: $popped")
                                    
                                    // If popBackStack failed (e.g., no previous screen), navigate explicitly
                                    if (!popped) {
                                        android.util.Log.d("MainActivity", "onApprove: popBackStack failed, navigating explicitly to search")
                                        navController.navigate("search") {
                                            // Pop all screens up to and including preview
                                            popUpTo("preview") { inclusive = true }
                                            // Don't create duplicate search screen
                                            launchSingleTop = true
                                        }
                                    }
                                } else {
                                    android.util.Log.e("MainActivity", "onApprove: Failed to save text - textId is null")
                                    errorMessage = "Не удалось сохранить текст. Попробуйте еще раз."
                                    isSaving = false
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "onApprove: Exception during save", e)
                                e.printStackTrace()
                                errorMessage = "Ошибка при сохранении: ${e.message ?: "Неизвестная ошибка"}"
                                isSaving = false
                            }
                        }
                    },
                    onEdit = {
                        android.util.Log.d("MainActivity", "onEdit: Navigating to textInput with title='${foundText.title}'")
                        editTextData = Pair(foundText.title, foundText.fullText)
                        previewText = null
                        searchViewModel.clearFoundText()
                        navController.navigate("textInput") {
                            popUpTo("search") { inclusive = false }
                        }
                    },
                    onRetry = {
                        previewText = null
                        searchViewModel.clearFoundText()
                        // Keep the search query and let user retry
                        navController.popBackStack()
                    }
                )
            } ?: run {
                // If previewText is null, navigate back
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
        
        composable("textInput") {
            val editData = editTextData
            TextInputScreen(
                initialTitle = editData?.first ?: "",
                initialText = editData?.second ?: "",
                onTextSaved = { textId ->
                    editTextData = null
                    navController.navigate("learning/$textId") {
                        popUpTo("search") { inclusive = false }
                    }
                },
                onCancel = {
                    editTextData = null
                    navController.popBackStack()
                },
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return TextInputViewModel(database, textService) as T
                        }
                    }
                )
            )
        }
        
        composable("fileInput") {
            selectedFileUri?.let { uri ->
                FileInputScreen(
                    uri = uri,
                    onTextSaved = { textId ->
                        navController.navigate("learning/$textId") {
                            popUpTo("search") { inclusive = false }
                        }
                    },
                    onCancel = {
                        navController.popBackStack()
                        selectedFileUri = null
                    },
                    database = database,
                    textService = textService
                )
            } ?: run {
                navController.popBackStack()
            }
        }
        
        composable("learning/{textId}") { backStackEntry ->
            val textId = backStackEntry.arguments?.getString("textId") ?: ""
            LearningScreen(
                textId = textId,
                onComplete = { sessionId ->
                    navController.navigate("statistics/$sessionId")
                },
                database = database
            )
        }
        
        composable("structure/{textId}") { backStackEntry ->
            val textId = backStackEntry.arguments?.getString("textId") ?: ""
            TextStructureScreen(
                onBack = { navController.popBackStack() },
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return TextStructureViewModel(database, textId) as T
                        }
                    }
                )
            )
        }

        composable("statistics/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            StatisticsScreen(
                sessionId = sessionId,
                onBack = {
                    navController.popBackStack("search", inclusive = false)
                }
            )
        }
    }
}
