package com.memorize.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val application = context.applicationContext as android.app.Application
    
    val viewModel: LearningViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LearningViewModel(
                    application = application,
                    database = database,
                    textId = textId,
                    onComplete = onComplete
                ) as T
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    
    // Show Pass2 instruction screen
    if (uiState.showPass2Instruction) {
        Pass2InstructionScreen(
            instructionText = "Теперь говори по фразе и дождись одобрения, чтобы сказать следующую. " +
                             "Если ошибёшься или нажмёшь «Не помню», бот подскажет правильный ответ. " +
                             "Блок считается выученным, когда ты пройдёшь все фразы без единой ошибки или подсказки.",
            fullText = uiState.pass2InstructionText,
            onStart = { viewModel.startPass2AfterInstruction() }
        )
        return
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Progress indicator
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (uiState.currentPhase) {
                    com.memorize.learning.LearningPhase.PASS1 -> "Режим 1: Изучение"
                    com.memorize.learning.LearningPhase.PASS2 -> "Режим 2: Закрепление"
                    else -> "Обучение"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Раздел ${uiState.currentSection + 1}/${uiState.totalSections} | " +
                       "Абзац ${uiState.currentParagraph + 1}/${uiState.totalParagraphs} | " +
                       "Фраза ${uiState.currentPhrase + 1}/${uiState.totalPhrases}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
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
                // Hide original text when listening
                if (!uiState.isListening) {
                    // Display text with typing animation
                    if (uiState.currentPhraseText.isNotEmpty()) {
                        val textToShow = if (uiState.displayedText.isNotEmpty()) {
                            uiState.displayedText
                        } else {
                            uiState.currentPhraseText
                        }
                        
                        Text(
                            text = textToShow,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                        
                        // Show cursor when typing
                        if (uiState.isSpeaking && uiState.displayedText.length < uiState.currentPhraseText.length) {
                            Text(
                                text = "|",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                // Show user spoken text - 3x larger
                if (uiState.isListening) {
                    if (uiState.userSpokenText.isNotEmpty()) {
                        Text(
                            text = uiState.userSpokenText,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = MaterialTheme.typography.displayLarge.fontSize * 3f
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                
                // Show ripple animation when listening
                if (uiState.isListening) {
                    Spacer(modifier = Modifier.height(32.dp))
                    RippleAnimation(
                        audioLevel = uiState.audioLevel,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Слушаю...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (uiState.isSpeaking) {
                    Spacer(modifier = Modifier.height(32.dp))
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Озвучиваю...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Show feedback
                if (uiState.feedback != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.feedback!!,
                        color = if (uiState.isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Control buttons - more contrast
        // Show "Не помню" button in PASS1 and PASS2 when listening or when there's an error
        if (uiState.currentPhase == com.memorize.learning.LearningPhase.PASS1 || 
            uiState.currentPhase == com.memorize.learning.LearningPhase.PASS2) {
            if (uiState.isListening || (!uiState.canContinue && !uiState.isCorrect)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { viewModel.onDontRemember() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            "Не помню",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Pass2InstructionScreen(
    instructionText: String,
    fullText: String,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Режим 2: Закрепление",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        // Instruction and text
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Instruction
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Инструкция:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = instructionText,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3f
                    )
                }
            }
            
            // Full text to be recited
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Текст для заучивания:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    val scrollState = rememberScrollState()
                    Text(
                        text = fullText,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4f,
                        textAlign = TextAlign.Justify,
                        modifier = Modifier.verticalScroll(scrollState)
                    )
                }
            }
        }
        
        // Start button
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Text(
                text = "Вперед",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

