package com.markxxxv.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestPermissions()
        
        setContent {
            MARKXXXVTheme {
                MARKXXXVApp(viewModel = viewModel { MainViewModel() })
            }
        }
    }
    
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.POST_NOTIFICATIONS
        )
        
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (notGranted.isNotEmpty()) {
            requestPermissionLauncher.launch(notGranted.toTypedArray())
        }
    }
}

@Composable
fun MARKXXXVTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MARKXXXVApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MARK XXXV", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E),
                    titleContentColor = Color(0xFFE6A756)
                )
            )
        },
        containerColor = Color(0xFF16213E)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status indicator
            StatusIndicator(state = uiState.appState)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // JARVIS Face
            JarvisFace(
                state = uiState.appState,
                modifier = Modifier.size(200.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Speech indicator
            Text(
                text = uiState.lastSpeech,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                maxLines = 2
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute button
                FloatingActionButton(
                    onClick = { viewModel.toggleMute() },
                    containerColor = if (uiState.isMuted) Color.Red else Color.DarkGray
                ) {
                    Icon(
                        imageVector = if (uiState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute"
                    )
                }
                
                // Listen button
                FloatingActionButton(
                    onClick = { viewModel.toggleListening() },
                    containerColor = Color(0xFFE6A756),
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Listen",
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                // Settings button
                FloatingActionButton(
                    onClick = { /* TODO: Settings */ },
                    containerColor = Color.DarkGray
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Logs
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3460))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    uiState.logs.takeLast(5).forEach { log ->
                        Text(
                            text = log,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(state: AppState) {
    val (color, text) = when (state) {
        AppState.LISTENING -> Pair(Color.Green, "LISTENING")
        AppState.SPEAKING -> Pair(Color(0xFFE6A756), "SPEAKING")
        AppState.THINKING -> Pair(Color.Cyan, "PROCESSING")
        AppState.MUTED -> Pair(Color.Red, "MUTED")
        AppState.IDLE -> Pair(Color.Gray, "READY")
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = text, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun JarvisFace(state: AppState, modifier: Modifier = Modifier) {
    val glowColor = when (state) {
        AppState.LISTENING -> Color.Cyan
        AppState.SPEAKING -> Color(0xFFE6A756)
        AppState.THINKING -> Color.Magenta
        AppState.MUTED -> Color.Red
        AppState.IDLE -> Color.Gray
    }
    
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(glowColor.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .clip(CircleShape)
                .background(Color(0xFF1A1A2E)),
            contentAlignment = Alignment.Center
        ) {
            // Simple face representation
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "M",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = glowColor
                )
            }
        }
    }
}