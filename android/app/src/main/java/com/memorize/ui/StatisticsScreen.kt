package com.memorize.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@Composable
fun StatisticsScreen(
    sessionId: String,
    onBack: () -> Unit
) {
    var showCelebration by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        delay(3000)
        showCelebration = false
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showCelebration) {
                CelebrationAnimation()
            }
            
            Text(
                text = "🎉 Поздравляем! 🎉",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Вы успешно выучили текст!",
                fontSize = 20.sp,
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Statistics will be loaded from database
            StatisticsCard(
                timeSpent = "15:30",
                repetitions = 42,
                mistakes = 8,
                grade = 85.5f
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Вернуться к поиску")
            }
        }
    }
}

@Composable
fun CelebrationAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "celebration")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .size(200.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD700),
                        Color(0xFFFFA500),
                        Color(0xFFFF6347)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "⭐",
            fontSize = (80 * scale).sp
        )
    }
}

@Composable
fun StatisticsCard(
    timeSpent: String,
    repetitions: Int,
    mistakes: Int,
    grade: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Статистика",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            StatisticRow("Время обучения:", timeSpent)
            StatisticRow("Количество повторов:", repetitions.toString())
            StatisticRow("Количество ошибок:", mistakes.toString())
            StatisticRow("Оценка:", "${grade.toInt()}%")
        }
    }
}

@Composable
fun StatisticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

