package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.DecibelMeter
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Red500
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun ConfiguracoesScreen(
    viewModel: MainViewModel,
    thresholdDb: Float,
    silenceTimeoutSec: Int,
    lensFacing: Int,
    screenSaverEnabled: Boolean,
    currentDb: Float,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var tempThreshold by remember(thresholdDb) { mutableFloatStateOf(thresholdDb) }
    var tempSilence by remember(silenceTimeoutSec) { mutableFloatStateOf(silenceTimeoutSec.toFloat()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Configurações do Vigia",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Ajuste os parâmetros de detecção sonora e sensibilidade",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Teste de Calibração em Tempo Real
        DecibelMeter(
            currentDb = currentDb,
            thresholdDb = tempThreshold,
            modifier = Modifier.fillMaxWidth()
        )

        // 1. Sensibilidade (Limite de dB)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_threshold_card"),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Cyan400,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sensibilidade (Limite Sonoro)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "${tempThreshold.toInt()} dB",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Cyan400
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "A gravação automática inicia quando o barulho atingir ou ultrapassar esse valor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Slider(
                    value = tempThreshold,
                    onValueChange = {
                        tempThreshold = it
                        viewModel.setThreshold(it)
                    },
                    valueRange = 35f..90f,
                    steps = 54,
                    colors = SliderDefaults.colors(
                        thumbColor = Cyan400,
                        activeTrackColor = Cyan400,
                        inactiveTrackColor = Slate800
                    ),
                    modifier = Modifier.testTag("sensitivity_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "35 dB (Muito sensível)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "65 dB (Recomendado)", fontSize = 11.sp, color = Emerald400)
                    Text(text = "90 dB (Muito alto)", fontSize = 11.sp, color = Red500)
                }
            }
        }

        // 2. Tempo de Silêncio para Parar Gravação
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_silence_card"),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Emerald400,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Silêncio contínuo até parar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "${tempSilence.toInt()}s",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Emerald400
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "A gravação só encerra após esse período completo em silêncio. Evita cortar o vídeo entre faixas de música ou pausas no barulho.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Slider(
                    value = tempSilence,
                    onValueChange = {
                        tempSilence = it
                        viewModel.setSilenceTimeout(it.toInt())
                    },
                    valueRange = 5f..60f,
                    steps = 10,
                    colors = SliderDefaults.colors(
                        thumbColor = Emerald400,
                        activeTrackColor = Emerald400,
                        inactiveTrackColor = Slate800
                    ),
                    modifier = Modifier.testTag("silence_timeout_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "5 seg", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "15 seg (Padrão)", fontSize = 11.sp, color = Emerald400)
                    Text(text = "60 seg", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // 3. Câmera Padrão & Economia de Bateria
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_camera_options_card"),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nightlight,
                            contentDescription = null,
                            tint = Cyan400,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Protetor de Tela Noturno",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Mantém a tela em preto absoluto durante a vigia para economizar bateria e não iluminar o quarto.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = screenSaverEnabled,
                        onCheckedChange = { viewModel.toggleScreenSaver(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Cyan400,
                            checkedTrackColor = Slate800
                        ),
                        modifier = Modifier.testTag("screen_saver_switch")
                    )
                }
            }
        }

        // 4. Orientações Jurídicas & Validade de Prova
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("legal_guide_card"),
            colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Balance,
                        contentDescription = null,
                        tint = Cyan400,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Validade Jurídica como Prova",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Cyan400
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• Perturbação do sossego alheio é contravenção penal (Art. 42 do Decreto-Lei nº 3.688/1941).\n" +
                            "• Este app armazena a data, hora exata e nível sonoro (dB) medido no arquivo MP4 original com áudio nítido.\n" +
                            "• Você pode exportar as evidências e anexar ao livro de ocorrências do condomínio, termo circunstanciado na Delegacia ou ação de indenização com tutela de urgência.\n" +
                            "• Para melhor precisão, posicione o tablet voltado na direção da origem do som (janela, parede do vizinho ou teto).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
