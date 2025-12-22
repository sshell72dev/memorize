package com.memorize

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.memorize.database.MemorizeDatabase
import com.memorize.network.TextService
import com.memorize.network.YandexGPTService
import com.memorize.ui.FileInputScreen
import com.memorize.ui.LearningScreen
import com.memorize.ui.SearchScreen
import com.memorize.ui.StatisticsScreen
import com.memorize.ui.TextInputScreen
import com.memorize.ui.theme.MemorizeTheme
import com.memorize.ui.viewmodel.LearningViewModel
import com.memorize.ui.viewmodel.SearchViewModel
import com.memorize.ui.viewmodel.TextInputViewModel
import com.memorize.utils.FileUtils
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var database: MemorizeDatabase
    private lateinit var textService: TextService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize database
        database = Room.databaseBuilder(
            applicationContext,
            MemorizeDatabase::class.java,
            MemorizeDatabase.DATABASE_NAME
        ).build()
        
        // Initialize Yandex GPT service
        val apiKey = getString(R.string.yandex_api_key)
        val folderId = getString(R.string.yandex_folder_id)
        val yandexGPTService = YandexGPTService(apiKey, folderId)
        textService = TextService(database, yandexGPTService)
        
        setContent {
            MemorizeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(database, textService)
                }
            }
        }
    }
}

    @Composable
    fun AppNavigation(
        database: MemorizeDatabase,
        textService: TextService
    ) {
        val navController = rememberNavController()
        val scope = rememberCoroutineScope()
        var selectedFileUri: Uri? by remember { mutableStateOf(null) }
        
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
                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                return SearchViewModel(database, textService) as T
                            }
                        }
                    )
                )
            }
            
            composable("textInput") {
                TextInputScreen(
                    onTextSaved = { textId ->
                        navController.navigate("learning/$textId") {
                            popUpTo("search") { inclusive = false }
                        }
                    },
                    onCancel = {
                        navController.popBackStack()
                    },
                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
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
