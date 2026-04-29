package com.markxxxv.api

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class GeminiApiClient(private val apiKey: String) {
    
    private val client = OkHttpClient()
    private val gson = Gson()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected
    
    private var currentModel = "gemini-2.5-flash"
    private var currentSession: String? = null
    private var systemInstruction = ""
    
    fun setSystemInstruction(instruction: String) {
        systemInstruction = instruction
    }
    
    suspend fun startRealtimeSession(): Result<String> = withContext(Dispatchers.IO) {
        try {
            currentSession = UUID.randomUUID().toString()
            
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/$currentModel:streamGenerateContent?key=$apiKey")
                .post("".toRequestBody("application/json".toMediaType()))
                .build()
            
            client.newCall(request).execute().use { response ->
                _isConnected.value = response.isSuccessful
                if (response.isSuccessful) {
                    currentSession?.let { Result.success(it) }
                } else {
                    Result.failure(IOException("Failed to start session: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun generateContent(
        contents: List<Content>,
        tools: List<Tool>? = null
    ): Result<GenerateResponse> = withContext(Dispatchers.IO) {
        try {
            val requestBody = mapOf(
                "systemInstruction" to mapOf("parts" to listOf(mapOf("text" to systemInstruction))),
                "contents" to contents,
                "generationConfig" to mapOf(
                    "temperature" to 0.9,
                    "maxOutputTokens" to 8192,
                    "topP" to 0.95,
                    "topK" to 40
                )
            ).let { config ->
                if (tools != null) config + ("tools" to tools)
                else config
            }
            
            val json = gson.toJson(requestBody)
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/$currentModel:generateContent?key=$apiKey")
                .post(json.toRequestBody("application/json".toMediaType()))
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val result = gson.fromJson(body, GenerateResponse::class.java)
                    Result.success(result)
                } else {
                    Result.failure(IOException("API error: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendRealtimeInput(audioBytes: ByteArray): Result<Unit> = suspendCoroutine { continuation ->
        if (currentSession == null) {
            continuation.resume(Result.failure(IOException("No active session")))
            return@suspendCoroutine
        }
        
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "audio.wav", audioBytes.toRequestBody("audio/wav".toMediaType()))
            .addFormDataPart("sessionId", currentSession!!)
            .build()
        
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/$currentModel:realtimeInput?key=$apiKey")
            .post(requestBody)
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resume(Result.failure(e))
            }
            
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    continuation.resume(Result.success(Unit))
                } else {
                    continuation.resume(Result.failure(IOException("Upload failed: ${response.code}")))
                }
                response.close()
            }
        })
    }
    
    fun close() {
        currentSession = null
        _isConnected.value = false
    }
}

// Data classes matching Gemini API

data class Content(
    val role: String,
    val parts: List<Part>
)

data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

data class InlineData(
    val mimeType: String,
    val data: String  // Base64 encoded
)

data class Tool(
    val functionDeclarations: List<FunctionDeclaration>
)

data class FunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: ParameterSchema
)

data class ParameterSchema(
    val type: String = "OBJECT",
    val properties: Map<String, Property>,
    val required: List<String>? = null
)

data class Property(
    val type: String,
    val description: String? = null,
    val enum: List<String>? = null
)

data class GenerateResponse(
    val candidates: List<Candidate>?,
    val promptFeedback: PromptFeedback?
)

data class Candidate(
    val content: Content?,
    val finishReason: String?,
    val safetyRatings: List<SafetyRating>?
)

data class SafetyRating(
    val category: String,
    val probability: String
)

data class PromptFeedback(
    val safetyRatings: List<SafetyRating>?
)

data class FunctionCall(
    val id: String,
    val name: String,
    val arguments: Map<String, Any>
)