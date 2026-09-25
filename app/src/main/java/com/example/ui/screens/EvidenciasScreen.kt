package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.RecordingEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Red500
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EvidenciasScreen(
    viewModel: MainViewModel,
    recordings: List<RecordingEntity>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedVideoForPlayback by remember { mutableStateOf<RecordingEntity?>(null) }
    var recordingToDelete by remember { mutableStateOf<RecordingEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Evidências Gravadas",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${recordings.size} arquivo(s) prontos como prova",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("empty_recordings_view"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nenhuma evidência gravada ainda",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Quando o vigia noturno detectar ruído acima do limite ou você iniciar manualmente, as gravações aparecerão aqui.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("recordings_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recordings, key = { it.id }) { item ->
                    RecordingCardItem(
                        recording = item,
                        onPlayClick = { selectedVideoForPlayback = item },
                        onShareClick = { shareVideo(context, item) },
                        onDeleteClick = { recordingToDelete = item }
                    )
                }
            }
        }
    }

    // Modal de Reprodução de Vídeo
    selectedVideoForPlayback?.let { recording ->
        VideoPlayerDialog(
            recording = recording,
            onDismiss = { selectedVideoForPlayback = null }
        )
    }

    // Diálogo de Confirmação de Exclusão
    recordingToDelete?.let { recording ->
        AlertDialog(
            onDismissRequest = { recordingToDelete = null },
            title = { Text(text = "Excluir Evidência?") },
            text = {
                Text(text = "Deseja realmente apagar o arquivo \"${recording.fileName}\"? Esta ação é irreversível.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecording(recording)
                        recordingToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red500),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Excluir", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { recordingToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun RecordingCardItem(
    recording: RecordingEntity,
    onPlayClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormatted = remember(recording.timestamp) {
        val sdf = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
        sdf.format(Date(recording.timestamp))
    }

    val durationFormatted = remember(recording.durationMs) {
        val totalSeconds = (recording.durationMs / 1000).toInt()
        String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    val sizeFormatted = remember(recording.fileSizeBytes) {
        val mb = recording.fileSizeBytes / (1024f * 1024f)
        String.format("%.1f MB", mb)
    }

    // Tentar carregar a miniatura
    val thumbnailBitmap = remember(recording.filePath) {
        val videoFile = File(recording.filePath)
        val thumbFile = File(videoFile.parentFile, "${videoFile.nameWithoutExtension}_thumb.jpg")
        if (thumbFile.exists()) {
            android.graphics.BitmapFactory.decodeFile(thumbFile.absolutePath)
        } else {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(recording.filePath)
                val bmp = retriever.getFrameAtTime(500000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                retriever.release()
                bmp
            } catch (_: Exception) {
                null
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recording_item_${recording.id}"),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Miniatura com botão de reproduzir
                Box(
                    modifier = Modifier
                        .size(90.dp, 68.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black)
                        .clickable(onClick = onPlayClick),
                    contentAlignment = Alignment.Center
                ) {
                    if (thumbnailBitmap != null) {
                        Image(
                            bitmap = thumbnailBitmap.asImageBitmap(),
                            contentDescription = "Miniatura do vídeo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color.DarkGray,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Botão circular play sobreposto
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Reproduzir",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Detalhes da evidência
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Badge de duração
                        Text(
                            text = durationFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Cyan400,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Slate800)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )

                        // Badge de tamanho
                        Text(
                            text = sizeFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Badge de modo
                        val isAuto = recording.triggerMode.contains("AUTO")
                        Text(
                            text = if (isAuto) "Vigia Auto" else "Manual",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isAuto) Emerald400 else Cyan400,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isAuto) Emerald400.copy(alpha = 0.15f) else Cyan400.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Barra inferior com ações: Compartilhar / Reproduzir / Excluir
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (recording.peakDecibels > 0f) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Red500,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pico: ${recording.peakDecibels.toInt()} dB",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Red500
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(1.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier.testTag("share_button_${recording.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartilhar Evidência",
                            tint = Cyan400
                        )
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.testTag("delete_button_${recording.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayerDialog(
    recording: RecordingEntity,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("video_player_dialog"),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        val mediaController = MediaController(ctx)
                        mediaController.setAnchorView(this)
                        setMediaController(mediaController)
                        setVideoPath(recording.filePath)
                        setOnPreparedListener { mp ->
                            mp.isLooping = false
                            start()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )

            // Botão fechar no topo
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .testTag("close_player_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fechar Reprodutor",
                    tint = Color.White
                )
            }

            // Título do arquivo embaixo
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = recording.fileName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Evidência de Perturbação Sonora gravada pelo Vigia Noturno",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private fun shareVideo(context: Context, recording: RecordingEntity) {
    try {
        val file = File(recording.filePath)
        if (!file.exists()) return

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Evidência de Perturbação Sonora - ${recording.fileName}")
            putExtra(
                Intent.EXTRA_TEXT,
                "Gravação de prova de perturbação sonora registrada em ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(recording.timestamp))}."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Compartilhar Evidência"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Erro ao compartilhar arquivo: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}
