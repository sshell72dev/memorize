package com.memorize.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.memorize.database.MemorizeDatabase
import com.memorize.network.TextService
import com.memorize.utils.FileUtils
import kotlinx.coroutines.launch

@Composable
fun FileInputScreen(
    uri: Uri,
    onTextSaved: (String) -> Unit,
    onCancel: () -> Unit,
    database: MemorizeDatabase,
    textService: TextService
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var fileName by remember { mutableStateOf<String?>(null) }
    var fileContent by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(uri) {
        fileName = FileUtils.getFileName(context, uri)
        fileContent = FileUtils.readTextFromUri(context, uri)
        title = fileName?.substringBeforeLast('.') ?: "Новый текст"
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Загрузка файла",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (fileContent == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            Text(
                text = "Чтение файла...",
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название текста") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                singleLine = true
            )
            
            Text(
                text = "Содержимое файла (${fileContent!!.length} символов):",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = fileContent!!,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Отмена")
                }
                
                Button(
                    onClick = {
                        if (title.isBlank() || fileContent.isNullOrBlank()) {
                            error = "Заполните все поля"
                            return@Button
                        }
                        
                        scope.launch {
                            isLoading = true
                            error = null
                            
                            try {
                                val textId = textService.saveTextDirectly(title, fileContent!!)
                                if (textId != null) {
                                    onTextSaved(textId)
                                } else {
                                    error = "Не удалось сохранить текст"
                                    isLoading = false
                                }
                            } catch (e: Exception) {
                                error = "Ошибка: ${e.message}"
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading && title.isNotBlank() && !fileContent.isNullOrBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Сохранение...")
                    } else {
                        Text("Сохранить и начать учить")
                    }
                }
            }
            
            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

