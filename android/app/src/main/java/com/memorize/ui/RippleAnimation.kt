package com.memorize.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

@Composable
fun RippleAnimation(
    modifier: Modifier = Modifier,
    audioLevel: Float = 0f,
    color: Color = androidx.compose.ui.graphics.Color(0xFF2196F3)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ripple")
    
    val ripple1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple1"
    )
    
    val ripple2 by infiniteTransition.animateFloat(
        initialValue = 0.33f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple2"
    )
    
    val ripple3 by infiniteTransition.animateFloat(
        initialValue = 0.66f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple3"
    )
    
    Canvas(modifier = modifier.size(200.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.minDimension / 2
        
        // Base circle size based on audio level
        val baseRadius = 30f + (audioLevel * 50f)
        
        // Draw ripples
        drawCircle(
            color = color.copy(alpha = 0.3f * (1 - ripple1)),
            radius = baseRadius + (maxRadius - baseRadius) * ripple1,
            center = center
        )
        
        drawCircle(
            color = color.copy(alpha = 0.3f * (1 - ripple2)),
            radius = baseRadius + (maxRadius - baseRadius) * ripple2,
            center = center
        )
        
        drawCircle(
            color = color.copy(alpha = 0.3f * (1 - ripple3)),
            radius = baseRadius + (maxRadius - baseRadius) * ripple3,
            center = center
        )
        
        // Draw center circle
        drawCircle(
            color = color.copy(alpha = 0.8f + audioLevel * 0.2f),
            radius = baseRadius,
            center = center
        )
        
        // Draw audio level indicator (waveform)
        if (audioLevel > 0.1f) {
            val wavePoints = 8
            val waveRadius = baseRadius + 20f
            val waveAmplitude = audioLevel * 15f
            
            for (i in 0 until wavePoints) {
                val angle = (i * 2 * PI / wavePoints).toFloat()
                val x = center.x + cos(angle) * waveRadius
                val y = center.y + sin(angle) * waveRadius
                val endX = center.x + cos(angle) * (waveRadius + waveAmplitude)
                val endY = center.y + sin(angle) * (waveRadius + waveAmplitude)
                
                drawLine(
                    color = color,
                    start = Offset(x, y),
                    end = Offset(endX, endY),
                    strokeWidth = 3f
                )
            }
        }
    }
}

