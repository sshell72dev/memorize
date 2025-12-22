package com.memorize.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.memorize.database.MemorizeDatabase
import com.memorize.ui.viewmodel.LearningViewModel

@Composable
fun LearningScreen(
    textId: String,
    onComplete: (String) -> Unit,
    database: MemorizeDatabase
) {
    val viewModel: LearningViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LearningViewModel(
                    application = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application,
                    database = database,
                    textId = textId,
                    onComplete = onComplete
                ) as T
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Progress indicator
        Text(
            text = "Раздел ${uiState.currentSection + 1}/${uiState.totalSections} | " +
                   "Абзац ${uiState.currentParagraph + 1}/${uiState.totalParagraphs} | " +
                   "Фраза ${uiState.currentPhrase + 1}/${uiState.totalPhrases}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Current phrase display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (uiState.currentPhraseText.isNotEmpty()) {
                    Text(
                        text = uiState.currentPhraseText,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                if (uiState.isListening) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                    Text("Слушаю...", modifier = Modifier.padding(top = 8.dp))
                }
                
                if (uiState.feedback != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.feedback!!,
                        color = if (uiState.isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { viewModel.onDontRemember() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text("Не помню")
            }
            
            if (uiState.canContinue) {
                Button(
                    onClick = { viewModel.continueToNext() }
                ) {
                    Text("Далее")
                }
            }
        }
    }
}

