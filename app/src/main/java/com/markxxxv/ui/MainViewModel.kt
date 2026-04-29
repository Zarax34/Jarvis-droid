package com.markxxxv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markxxxv.agent.AgentPlanner
import com.markxxxv.api.GeminiApiClient
import com.markxxxv.memory.MemoryManager
import com.markxxxv.service.VoiceService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppState {
    IDLE, LISTENING, SPEAKING, THINKING, MUTED
}

data class UiState(
    val appState: AppState = AppState.IDLE,
    val isMuted: Boolean = false,
    val lastSpeech: String = "",
    val logs: List<String> = emptyList()
)

class MainViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private var apiClient: GeminiApiClient? = null
    private var memoryManager: MemoryManager? = null
    private var agentPlanner: AgentPlanner? = null
    private var voiceService: VoiceService? = null
    
    fun initialize(apiKey: String) {
        apiClient = GeminiApiClient(apiKey)
        memoryManager = MemoryManager()
        agentPlanner = AgentPlanner(apiClient!!)
        
        viewModelScope.launch {
            log("[System] MARK XXXV initializing...")
            
            // Load memory
            val memory = memoryManager?.loadMemory()
            if (memory != null) {
                log("[Memory] Loaded ${memory.size} items")
                apiClient?.setSystemInstruction(buildSystemPrompt(memory))
            }
            
            // Start session
            apiClient?.startRealtimeSession()?.onSuccess {
                log("[System] Connected to Gemini")
            }?.onFailure { e ->
                log("[Error] ${e.message}")
            }
        }
    }
    
    fun toggleMute() {
        val newMuted = !_uiState.value.isMuted
        _uiState.value = _uiState.value.copy(
            isMuted = newMuted,
            appState = if (newMuted) AppState.MUTED else AppState.IDLE
        )
        log(if (newMuted) "[System] Muted" else "[System] Unmuted")
    }
    
    fun toggleListening() {
        val isCurrentlyListening = _uiState.value.appState == AppState.LISTENING
        if (isCurrentlyListening) {
            stopListening()
        } else {
            startListening()
        }
    }
    
    private fun startListening() {
        _uiState.value = _uiState.value.copy(appState = AppState.LISTENING)
        voiceService?.startListening { audioData ->
            processAudio(audioData)
        }
        log("[System] Listening...")
    }
    
    private fun stopListening() {
        voiceService?.stopListening()
        _uiState.value = _uiState.value.copy(appState = AppState.IDLE)
    }
    
    private fun processAudio(audioData: ByteArray) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(appState = AppState.THINKING)
            
            apiClient?.sendRealtimeInput(audioData)?.onSuccess { response ->
                // Process Gemini response
                handleGeminiResponse(response)
            }?.onFailure { e ->
                log("[Error] ${e.message}")
                _uiState.value = _uiState.value.copy(appState = AppState.IDLE)
            }
        }
    }
    
    private fun handleGeminiResponse(response: Any) {
        // Check for function calls
        // Extract text response
        // Speak response if not muted
        // Execute tools if needed
        
        _uiState.value = _uiState.value.copy(
            appState = if (!_uiState.value.isMuted) AppState.SPEAKING else AppState.IDLE,
            lastSpeech = "Response text here" // TODO: Extract from response
        )
    }
    
    fun onTextCommand(command: String) {
        viewModelScope.launch {
            log("[User] $command")
            _uiState.value = _uiState.value.copy(appState = AppState.THINKING)
            
            // Process text command similar to audio
        }
    }
    
    private fun log(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val logMessage = "[$timestamp] $message"
        
        _uiState.value = _uiState.value.copy(
            logs = _uiState.value.logs + logMessage
        )
    }
    
    private fun buildSystemPrompt(memory: Map<String, Any>): String {
        return buildString {
            appendLine("You are MARK XXXV, an AI assistant. respond in the user's language.")
            appendLine("Current time: ${java.time.LocalDateTime.now()}")
            
            if (memory.isNotEmpty()) {
                appendLine("\nUser memory:")
                memory.forEach { (key, value) ->
                    appendLine("- $key: $value")
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        apiClient?.close()
    }
}