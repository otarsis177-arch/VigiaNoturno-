package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Amber500
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Red500
import com.example.ui.theme.Slate800

@Composable
fun DecibelMeter(
    currentDb: Float,
    thresholdDb: Float,
    modifier: Modifier = Modifier
) {
    val animatedDb by animateFloatAsState(
        targetValue = currentDb.coerceIn(20f, 100f),
        label = "db_animation"
    )

    val isExceeded = currentDb >= thresholdDb

    val statusColor by animateColorAsState(
        targetValue = when {
            isExceeded -> Red500
            currentDb >= thresholdDb - 8f -> Amber500
            else -> Cyan400
        },
        label = "status_color"
    )

    val statusText = when {
        isExceeded -> "PERTURBAÇÃO / RUÍDO ACIMA DO LIMITE!"
        currentDb >= thresholdDb - 8f -> "Atenção: Ruído próximo ao limite"
        currentDb >= 45f -> "Ambiente moderado"
        else -> "Ambiente silencioso"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Slate800.copy(alpha = 0.85f))
            .padding(16.dp)
            .testTag("decibel_meter_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isExceeded) Icons.Default.VolumeUp else Icons.Default.GraphicEq,
                        contentDescription = "Ícone de volume",
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "NÍVEL SONORO",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${animatedDb.toInt()}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = statusColor
                    )
                    Text(
                        text = " dB",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Barra gráfica de decibéis (20 dB a 100 dB)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .testTag("decibel_canvas_bar")
            ) {
                val width = size.width
                val height = size.height

                // Fundo da barra
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    size = size,
                    cornerRadius = CornerRadius(height / 2, height / 2)
                )

                // Progresso atual
                val normalizedCurrent = ((animatedDb - 20f) / 80f).coerceIn(0f, 1f)
                val fillWidth = width * normalizedCurrent

                val gradient = Brush.horizontalGradient(
                    colors = listOf(
                        Emerald400,
                        Cyan400,
                        Amber500,
                        Red500
                    )
                )

                if (fillWidth > 0f) {
                    drawRoundRect(
                        brush = gradient,
                        size = Size(fillWidth, height),
                        cornerRadius = CornerRadius(height / 2, height / 2)
                    )
                }

                // Marcador do limiar (Threshold Marker)
                val normalizedThreshold = ((thresholdDb - 20f) / 80f).coerceIn(0f, 1f)
                val markerX = width * normalizedThreshold

                drawLine(
                    color = Color.White,
                    start = Offset(markerX, -4f),
                    end = Offset(markerX, height + 4f),
                    strokeWidth = 3.dp.toPx()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )
                Text(
                    text = "Gatilho: ${thresholdDb.toInt()} dB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}
