package com.memorize.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    context: android.content.Context
) {
    val prefs = remember { 
        context.getSharedPreferences("memorize_prefs", android.content.Context.MODE_PRIVATE) 
    }
    
    var phrasesPerParagraph by remember {
        mutableStateOf(
            prefs.getInt("phrases_per_paragraph", 4)
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Параметры обучения",
                style = MaterialTheme.typography.titleLarge
            )
            
            Divider()
            
            // Настройка количества фраз в абзаце
            Column {
                Text(
                    text = "Количество фраз в абзаце по умолчанию",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Определяет, сколько фраз будет в одном абзаце при создании структуры текста",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Фраз в абзаце: $phrasesPerParagraph",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (phrasesPerParagraph > 1) {
                                    phrasesPerParagraph--
                                    prefs.edit().putInt("phrases_per_paragraph", phrasesPerParagraph).apply()
                                }
                            },
                            enabled = phrasesPerParagraph > 1
                        ) {
                            Text("-", style = MaterialTheme.typography.headlineMedium)
                        }
                        
                        Text(
                            text = phrasesPerParagraph.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        IconButton(
                            onClick = {
                                if (phrasesPerParagraph < 20) {
                                    phrasesPerParagraph++
                                    prefs.edit().putInt("phrases_per_paragraph", phrasesPerParagraph).apply()
                                }
                            },
                            enabled = phrasesPerParagraph < 20
                        ) {
                            Text("+", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
        }
    }
}

