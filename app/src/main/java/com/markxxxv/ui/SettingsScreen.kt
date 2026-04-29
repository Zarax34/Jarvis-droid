package com.markxxxv.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    var apiKey by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("gemini-2.5-flash") }
    var voiceSpeed by remember { mutableFloatStateOf(1.0f) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back", color = Color(0xFFE6A756))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E),
                    titleContentColor = Color.White
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // API Key
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("Gemini API Key") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE6A756),
                    focusedLabelColor = Color(0xFFE6A756)
                )
            )
            
            // Model Selection
            OutlinedTextField(
                value = modelName,
                onValueChange = { modelName = it },
                label = { Text("Model Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE6A756),
                    focusedLabelColor = Color(0xFFE6A756)
                )
            )
            
            // Voice Speed
            Column {
                Text("Voice Speed: ${String.format("%.1f", voiceSpeed)}x", color = Color.White)
                Slider(
                    value = voiceSpeed,
                    onValueChange = { voiceSpeed = it },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFE6A756),
                        activeTrackColor = Color(0xFFE6A756)
                    )
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Save Button
            Button(
                onClick = { /* TODO: Save settings */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE6A756))
            ) {
                Text("Save Settings", color = Color.Black)
            }
        }
    }
}