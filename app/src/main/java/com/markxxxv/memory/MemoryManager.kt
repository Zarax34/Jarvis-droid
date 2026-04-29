package com.markxxxv.memory

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mark_settings")

class SettingsManager(private val context: Context) {
    
    private val gson = Gson()
    
    val apiKey = stringPreferencesKey("api_key")
    val modelName = stringPreferencesKey("model_name")
    val voiceSpeed = floatPreferencesKey("voice_speed")
    val muteKey = stringPreferencesKey("mute_key")
    val language = stringPreferencesKey("language")
    
    suspend fun getApiKey(): String? = context.dataStore.data.map { prefs ->
        prefs[apiKey]
    }.first()
    
    suspend fun setApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[apiKey] = key
        }
    }
    
    suspend fun getSettings(): Map<String, Any> = context.dataStore.data.map { prefs ->
        mapOf(
            "modelName" to (prefs[modelName] ?: "gemini-2.0-flash-exp"),
            "voiceSpeed" to (prefs[voiceSpeed] ?: 1.0f),
            "muteKey" to (prefs[muteKey] ?: "F4"),
            "language" to (prefs[language] ?: "auto")
        )
    }.first()
    
    suspend fun saveSettings(settings: Map<String, Any>) {
        context.dataStore.edit { prefs ->
            settings.forEach { (key, value) ->
                when (key) {
                    "modelName" -> prefs[modelName] = value as String
                    "voiceSpeed" -> prefs[voiceSpeed] = value as Float
                    "muteKey" -> prefs[muteKey] = value as String
                    "language" -> prefs[language] = value as String
                }
            }
        }
    }
}

class MemoryManager(private val context: Context) {
    
    private val gson = Gson()
    private val memoryFile = File(context.filesDir, "long_term.json")
    
    private val identityKey = stringPreferencesKey("identity")
    private val preferencesKey = stringPreferencesKey("preferences")
    private val projectsKey = stringPreferencesKey("projects")
    private val relationshipsKey = stringPreferencesKey("relationships")
    private val notesKey = stringPreferencesKey("notes")
    
    suspend fun loadMemory(): Map<String, Any> = try {
        if (memoryFile.exists()) {
            val json = memoryFile.readText()
            gson.fromJson(json, object : TypeToken<Map<String, Any>>() {}.type)
        } else {
            emptyMap()
        }
    } catch (e: Exception) {
        emptyMap()
    }
    
    suspend fun saveMemory(memory: Map<String, Any>) {
        try {
            val json = gson.toJson(memory)
            memoryFile.writeText(json)
        } catch (e: Exception) {
            // Log error
        }
    }
    
    suspend fun updateEntry(category: String, key: String, value: Any) {
        val current = loadMemory().toMutableMap()
        val categoryMap = (current[category] as? Map<String, Any>)?.toMutableMap() ?: mutableMapOf()
        categoryMap[key] = value
        current[category] = categoryMap
        saveMemory(current)
    }
    
    suspend fun extractMemory(text: String): Boolean {
        // Uses lightweight model to check if memory should be extracted
        // Two-stage: quick check then detailed extraction
        return false  // Placeholder - needs Gemini API
    }
}