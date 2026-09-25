package com.example.ui.screens

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.service.VigiaService
import com.example.ui.MainViewModel
import com.example.ui.components.DecibelMeter
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Red500
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun VigiaScreen(
    viewModel: MainViewModel,
    serviceState: VigiaService.VigiaState,
    currentDb: Float,
    thresholdDb: Float,
    silenceRemainingSec: Int,
    recordingDurationSec: Int,
    triggerCount: Int,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val recPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_pulse"
    )

    val isRecording = serviceState == VigiaService.VigiaState.RECORDING_AUTO ||
            serviceState == VigiaService.VigiaState.RECORDING_MANUAL

    val isMonitoring = serviceState == VigiaService.VigiaState.MONITORING

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Top Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    when {
                        isRecording -> Red600.copy(alpha = 0.25f)
                        isMonitoring -> Emerald400.copy(alpha = 0.15f)
                        else -> Slate800
                    }
                )
                .border(
                    width = 1.dp,
                    color = when {
                        isRecording -> Red500
                        isMonitoring -> Emerald400
                        else -> Color.Transparent
                    },
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("status_banner")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isRecording -> Red500
                                    isMonitoring -> Emerald400
                                    else -> Color.Gray
                                }
                            )
                            .alpha(if (isRecording) recPulseAlpha else 1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = when (serviceState) {
                                VigiaService.VigiaState.RECORDING_AUTO -> "GRAVANDO EVIDÊNCIA (AUTOMÁTICO)"
                                VigiaService.VigiaState.RECORDING_MANUAL -> "GRAVANDO EVIDÊNCIA (MANUAL)"
                                VigiaService.VigiaState.MONITORING -> "MODO VIGIA NOTURNO ATIVO"
                                VigiaService.VigiaState.IDLE -> "SISTEMA EM ESPERA"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isRecording -> Red500
                                isMonitoring -> Emerald400
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )

                        if (isRecording) {
                            val durationFormatted = String.format("%02d:%02d", recordingDurationSec / 60, recordingDurationSec % 60)
                            Text(
                                text = "Duração: $durationFormatted",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Red500
                            )
                        } else if (isMonitoring) {
                            Text(
                                text = "Microfone e câmera armados para captura",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (isRecording && serviceState == VigiaService.VigiaState.RECORDING_AUTO) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Silêncio restante:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${silenceRemainingSec}s",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Cyan400
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Visualizador da Câmera (PreviewView)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black)
                .border(2.dp, if (isRecording) Red500 else Slate800, RoundedCornerShape(18.dp))
                .testTag("camera_preview_container")
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        viewModel.attachCameraPreview(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Controles sobrepostos na câmera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Indicador REC na tela da câmera
                if (isRecording) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Red500)
                                .alpha(recPulseAlpha)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REC",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(1.dp))
                }

                Row {
                    // Botão alternar lente (Frontal/Traseira)
                    IconButton(
                        onClick = { viewModel.switchCameraLens() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("switch_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Alternar Câmera",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Botão Tela Apagada (Modo Noturno Stealth)
                    IconButton(
                        onClick = { viewModel.setNightScreenActive(true) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("night_screensaver_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nightlight,
                            contentDescription = "Modo Tela Apagada",
                            tint = Cyan400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Decibelímetro ao vivo
        DecibelMeter(
            currentDb = currentDb,
            thresholdDb = thresholdDb,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Controles Principais
        // 1. MODO VIGIA NOTURNO (AUTOMÁTICO)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vigia_mode_card"),
            colors = CardDefaults.cardColors(
                containerColor = Slate900
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Modo Vigia Noturno",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Monitora o microfone a noite toda. Grava vídeo automaticamente se o som ultrapassar ${thresholdDb.toInt()} dB.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (isMonitoring || serviceState == VigiaService.VigiaState.RECORDING_AUTO) {
                            viewModel.stopVigia()
                        } else {
                            viewModel.startVigia()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("toggle_vigia_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMonitoring || serviceState == VigiaService.VigiaState.RECORDING_AUTO) {
                            Red500
                        } else {
                            Emerald400
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isMonitoring || serviceState == VigiaService.VigiaState.RECORDING_AUTO) {
                            Icons.Default.Stop
                        } else {
                            Icons.Default.Security
                        },
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isMonitoring || serviceState == VigiaService.VigiaState.RECORDING_AUTO) {
                            "DESATIVAR VIGIA NOTURNO"
                        } else {
                            "ATIVAR MODO VIGIA NOTURNO"
                        },
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. MODO MANUAL (Gravação Imediata)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manual_mode_card"),
            colors = CardDefaults.cardColors(
                containerColor = Slate900
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Gravação Manual Imediata",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Inicie e pare a gravação de vídeo+áudio na hora com um toque.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                val isManualRecording = serviceState == VigiaService.VigiaState.RECORDING_MANUAL

                Button(
                    onClick = {
                        if (isManualRecording) {
                            viewModel.stopManualRecording()
                        } else {
                            viewModel.startManualRecording()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("manual_record_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isManualRecording) Red600 else Cyan400
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isManualRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isManualRecording) "PARAR GRAVAÇÃO MANUAL" else "GRAVAR AGORA (MANUAL)",
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Cards de estatísticas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("stats_triggers_card"),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Disparos Hoje",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$triggerCount",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Cyan400
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("stats_threshold_card"),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Limite Acionamento",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${thresholdDb.toInt()} dB",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Emerald400
                    )
                }
            }
        }
    }
}
