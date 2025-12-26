package com.memorize.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.memorize.ui.viewmodel.SearchViewModel
import com.memorize.ui.viewmodel.SearchUiState

@Composable
fun SearchScreen(
    onTextSelected: (String) -> Unit,
    onFileSelected: () -> Unit,
    onTextInput: () -> Unit,
    onShowStructure: (String) -> Unit = {},
    onShowSettings: () -> Unit = {},
    viewModel: SearchViewModel = viewModel()
) {
    android.util.Log.d("SearchScreen", "SearchScreen: Composable function called")
    val uiState by viewModel.uiState.collectAsState(initial = SearchUiState())
    android.util.Log.d("SearchScreen", "SearchScreen: uiState collected, savedTexts.size=${uiState.savedTexts.size}")
    
    // Refresh saved texts when screen is displayed
    LaunchedEffect(Unit) {
        android.util.Log.d("SearchScreen", "LaunchedEffect(Unit): Refreshing saved texts")
        try {
            viewModel.refreshSavedTexts()
        } catch (e: Exception) {
            android.util.Log.e("SearchScreen", "Error refreshing saved texts", e)
            e.printStackTrace()
        }
    }
    
    android.util.Log.d("SearchScreen", "SearchScreen: Starting Column composition")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        android.util.Log.d("SearchScreen", "SearchScreen: Column content starting")
        // Заголовок с кнопкой настроек
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Поиск текста",
                style = MaterialTheme.typography.headlineSmall
            )
            IconButton(
                onClick = onShowSettings,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Настройки",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            label = { Text("Введите название и автора (например: \"Зимнее утро Пушкин\" или \"автор: Пушкин Зимнее утро\")") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { viewModel.search() },
            enabled = !uiState.isLoading && uiState.searchQuery.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Загрузка...")
            } else {
                Text("Найти и начать учить")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Divider()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Или загрузите свой текст",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onFileSelected,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text("📁 Загрузить файл")
            }
            
            Button(
                onClick = onTextInput,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Text("✏️ Ввести текст")
            }
        }
        
        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.error!!,
                color = MaterialTheme.colorScheme.error
            )
        }
        
        
        // Show saved texts list
        android.util.Log.d("SearchScreen", "SearchScreen: Checking savedTexts, size=${uiState.savedTexts.size}")
        if (uiState.savedTexts.isNotEmpty()) {
            android.util.Log.d("SearchScreen", "SearchScreen: Rendering saved texts list")
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Сохраненные тексты:",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn {
                items(uiState.savedTexts.size) { index ->
                    // Safe access to list item
                    if (index < uiState.savedTexts.size) {
                        val text = uiState.savedTexts[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onTextSelected(text.id) }
                                ) {
                                    Text(
                                        text = text.title,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // Кнопка структуры (иконка)
                                IconButton(
                                    onClick = { onShowStructure(text.id) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Структура",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                // Кнопка удаления (иконка)
                                IconButton(
                                    onClick = { viewModel.deleteText(text.id) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Удалить",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            android.util.Log.d("SearchScreen", "SearchScreen: No saved texts to display")
        }
        android.util.Log.d("SearchScreen", "SearchScreen: Column composition completed")
    }
}

