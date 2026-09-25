package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.VigiaService
import com.example.ui.MainViewModel
import com.example.ui.components.NightScreenSaver
import com.example.ui.screens.ConfiguracoesScreen
import com.example.ui.screens.EvidenciasScreen
import com.example.ui.screens.VigiaScreen
import com.example.ui.theme.Cyan400
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Red500
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Manter tela ligada durante a vigia
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isTabletOrLandscape = configuration.screenWidthDp >= 600

    val serviceState by viewModel.serviceState.collectAsStateWithLifecycle()
    val currentDb by viewModel.currentDb.collectAsStateWithLifecycle()
    val silenceRemainingSec by viewModel.silenceRemainingSec.collectAsStateWithLifecycle()
    val recordingDurationSec by viewModel.recordingDurationSec.collectAsStateWithLifecycle()
    val triggerCount by viewModel.triggerCount.collectAsStateWithLifecycle()
    val recordings by viewModel.recordings.collectAsStateWithLifecycle()
    val thresholdDb by viewModel.thresholdDb.collectAsStateWithLifecycle()
    val silenceTimeoutSec by viewModel.silenceTimeoutSec.collectAsStateWithLifecycle()
    val lensFacing by viewModel.lensFacing.collectAsStateWithLifecycle()
    val screenSaverEnabled by viewModel.screenSaverEnabled.collectAsStateWithLifecycle()
    val isNightScreenActive by viewModel.isNightScreenActive.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }

    // Gerenciador de permissões
    val requiredPermissions = remember {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissions.toTypedArray()
    }

    var hasAllPermissions by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasAllPermissions = results.values.all { it }
    }

    LaunchedEffect(Unit) {
        if (!hasAllPermissions) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    if (isNightScreenActive) {
        NightScreenSaver(
            serviceState = serviceState,
            currentDb = currentDb,
            thresholdDb = thresholdDb,
            durationSec = recordingDurationSec,
            onDismiss = { viewModel.setNightScreenActive(false) }
        )
        return
    }

    if (!hasAllPermissions) {
        PermissionRequiredScreen(
            onRequestPermissions = { permissionLauncher.launch(requiredPermissions) }
        )
        return
    }

    val navItems = listOf(
        NavItem("Vigia", Icons.Filled.Security, Icons.Outlined.Security, "tab_vigia"),
        NavItem("Evidências", Icons.Filled.Videocam, Icons.Outlined.Videocam, "tab_evidencias", recordings.size),
        NavItem("Configurar", Icons.Filled.Tune, Icons.Outlined.Tune, "tab_config")
    )

    if (isTabletOrLandscape) {
        // Layout para Tablet Samsung Galaxy Tab A9 com NavigationRail
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate950)
        ) {
            NavigationRail(
                containerColor = Slate900,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("tablet_navigation_rail")
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                navItems.forEachIndexed { index, item ->
                    NavigationRailItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            if (item.badgeCount != null && item.badgeCount > 0) {
                                BadgedBox(badge = { Badge { Text("${item.badgeCount}") } }) {
                                    Icon(
                                        imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            }
                        },
                        label = { Text(item.label) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            indicatorColor = Cyan400,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(item.testTag)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> VigiaScreen(
                        viewModel = viewModel,
                        serviceState = serviceState,
                        currentDb = currentDb,
                        thresholdDb = thresholdDb,
                        silenceRemainingSec = silenceRemainingSec,
                        recordingDurationSec = recordingDurationSec,
                        triggerCount = triggerCount
                    )
                    1 -> EvidenciasScreen(
                        viewModel = viewModel,
                        recordings = recordings
                    )
                    2 -> ConfiguracoesScreen(
                        viewModel = viewModel,
                        thresholdDb = thresholdDb,
                        silenceTimeoutSec = silenceTimeoutSec,
                        lensFacing = lensFacing,
                        screenSaverEnabled = screenSaverEnabled,
                        currentDb = currentDb
                    )
                }
            }
        }
    } else {
        // Layout compacto vertical com NavigationBar
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Slate900,
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                if (item.badgeCount != null && item.badgeCount > 0) {
                                    BadgedBox(badge = { Badge { Text("${item.badgeCount}") } }) {
                                        Icon(
                                            imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                }
                            },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                indicatorColor = Cyan400,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Slate950)
            ) {
                when (selectedTab) {
                    0 -> VigiaScreen(
                        viewModel = viewModel,
                        serviceState = serviceState,
                        currentDb = currentDb,
                        thresholdDb = thresholdDb,
                        silenceRemainingSec = silenceRemainingSec,
                        recordingDurationSec = recordingDurationSec,
                        triggerCount = triggerCount
                    )
                    1 -> EvidenciasScreen(
                        viewModel = viewModel,
                        recordings = recordings
                    )
                    2 -> ConfiguracoesScreen(
                        viewModel = viewModel,
                        thresholdDb = thresholdDb,
                        silenceTimeoutSec = silenceTimeoutSec,
                        lensFacing = lensFacing,
                        screenSaverEnabled = screenSaverEnabled,
                        currentDb = currentDb
                    )
                }
            }
        }
    }
}

data class NavItem(
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String,
    val badgeCount: Int? = null
)

@Composable
fun PermissionRequiredScreen(onRequestPermissions: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(24.dp)
            .testTag("permission_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Slate900)
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Cyan400,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Permissões Necessárias",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "O Vigia Noturno precisa de acesso à Câmera e ao Microfone para gravar vídeos com áudio como prova de perturbação sonora, além de Notificações para rodar em segundo plano durante a madrugada.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = Cyan400),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("grant_permissions_button")
            ) {
                Text(
                    text = "Conceder Permissões",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}
